package org.sesacteamproject.passmate.ios

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Kotlin suspend 함수를 Swift로 노출하면 **메인 스레드에서만** 호출할 수 있다.
// 그런데 완료 콜백(`{ result, error in }`)은 Ktor의 백그라운드 스레드로 온다.
// 그 안에서 곧바로 다음 suspend 함수를 부르면 Kotlin/Native가 예외를 던지고 앱이 죽는다
// (SIGABRT — 2026-09-07 '내 리포트 보기' 크래시). 콜백 안에서 다음 호출을 하려면
// 반드시 DispatchQueue.main 으로 넘긴 뒤에 한다.
//
// 이벤트 스트림 콜백(`{ streamEvent in }`, 1인자)은 SessionEventStreamWatcher가
// Dispatchers.Main으로 방출하므로 검사 대상이 아니다.
class IosCompletionThreadTest {

    // `.invoke(...) { result, error in` — ObjC로 노출된 suspend 함수의 완료 콜백
    private val asyncCall = Regex("""\.(invoke|start)\s*\(.*\)\s*\{\s*(\[weak self]\s*)?\w+\s*,\s*\w+\s+in\s*$""")

    private val mainHop = Regex("""DispatchQueue\.main\.(async|asyncAfter)""")

    private val selfCall = Regex("""self\??\.(\w+)\s*\(""")

    private fun iosSourceDir(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile

        while (dir.parentFile != null && !File(dir, "iosApp/iosApp").isDirectory) {
            dir = dir.parentFile
        }
        return File(dir, "iosApp/iosApp")
    }

    // 같은 파일에서 완료 콜백을 여는 메서드 이름 — 콜백 안의 간접 호출을 잡기 위해 쓴다
    private fun methodsWithAsyncCall(lines: List<String>): Set<String> {
        val names = mutableSetOf<String>()
        var current: String? = null
        var depth = 0

        lines.forEach { line ->
            val declared = Regex("""\bfunc\s+(\w+)""").find(line)

            if (current == null && declared != null) {
                current = declared.groupValues[1]
                depth = 0
            }
            if (current != null) {
                if (asyncCall.containsMatchIn(line)) {
                    names.add(current!!)
                }
                depth += line.count { it == '{' } - line.count { it == '}' }
                if (depth <= 0 && line.contains("}")) {
                    current = null
                }
            }
        }
        return names
    }

    private fun violationsIn(file: File): List<String> {
        val lines = file.readLines()
        val asyncMethods = methodsWithAsyncCall(lines)
        val found = mutableListOf<String>()
        // 여는 중괄호마다 종류를 쌓는다: 완료 콜백 / 메인 큐 홉 / 그 외
        val scopes = ArrayDeque<String>()

        lines.forEachIndexed { index, line ->
            val insideCallback = scopes.contains("callback")
            val insideMain = scopes.contains("main")

            if (insideCallback && !insideMain) {
                val indirect = selfCall.findAll(line).map { it.groupValues[1] }.firstOrNull { it in asyncMethods }

                if (asyncCall.containsMatchIn(line)) {
                    found.add("${file.name}:${index + 1} — 콜백 스레드에서 suspend 호출: ${line.trim()}")
                } else if (indirect != null) {
                    found.add("${file.name}:${index + 1} — 콜백 스레드에서 $indirect() 호출(내부가 suspend)")
                }
            }
            repeat(line.count { it == '{' }) {
                scopes.addLast(
                    when {
                        asyncCall.containsMatchIn(line) -> "callback"
                        mainHop.containsMatchIn(line) -> "main"
                        else -> "other"
                    }
                )
            }
            repeat(line.count { it == '}' }) {
                scopes.removeLastOrNull()
            }
        }
        return found
    }

    @Test
    fun neverCallsSuspendFunctionFromCompletionThread() {
        val violations = iosSourceDir().walkTopDown()
            .filter { it.isFile && it.extension == "swift" }
            .flatMap { violationsIn(it) }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "완료 콜백은 백그라운드 스레드다 — DispatchQueue.main 안으로 옮겨야 한다:\n" +
                violations.joinToString("\n")
        )
    }
}
