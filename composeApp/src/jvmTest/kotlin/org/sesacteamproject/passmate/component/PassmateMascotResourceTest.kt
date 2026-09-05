package org.sesacteamproject.passmate.component

import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// 마스코트 리소스 회귀 가드 — 파일이 사라지거나, 두 타깃 사본이 어긋나거나,
// 공통 캔버스 규격이 깨지면 여기서 실패한다.
//
// 캔버스 규격이 핵심이다: PassmateMascot은 리소스가 144x156(=576x624 @4x)이고 시안 프레임이
// 그 안 (16,24)에 있다고 보고 번짐을 계산한다. 시안에서 다시 뽑을 때 프레임만 렌더하거나
// 상태마다 다른 크기로 뽑으면 마스코트가 화면마다 다른 크기·위치로 그려진다
class PassmateMascotResourceTest {

    // 컴포넌트가 실제로 계산에 쓰는 상수에서 끌어온다 — 테스트가 네 번째 사본을 갖지 않게 한다.
    // 리소스는 4배율로 뽑으므로 dp에 4를 곱한다
    private val assetScale = 4

    private val canvasWidth = (MASCOT_CANVAS_WIDTH * assetScale).toInt()

    private val canvasHeight = (MASCOT_CANVAS_HEIGHT * assetScale).toInt()

    private fun moduleDir(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null && !File(dir, "src/androidMain/res").isDirectory) {
            dir = dir.parentFile
        }
        return dir
    }

    private fun androidFile(mascot: PassmateMascots): File {
        return File(moduleDir(), "src/androidMain/res/drawable-xxxhdpi/${mascot.resourceName}.png")
    }

    private fun jvmFile(mascot: PassmateMascots): File {
        return File(moduleDir(), "src/jvmMain/resources/drawable/${mascot.resourceName}.png")
    }

    private fun iosDir(mascot: PassmateMascots): File {
        // iOS 에셋 이름은 "Mascot" + enum 이름이다 (PassmateMascots.swift의 rawValue와 같은 규칙)
        return File(moduleDir().parentFile, "iosApp/iosApp/Assets.xcassets/Mascot${mascot.name}.imageset")
    }

    private fun classpathBytes(mascot: PassmateMascots): ByteArray? {
        val loader = Thread.currentThread().contextClassLoader ?: javaClass.classLoader
        return loader.getResourceAsStream("drawable/${mascot.resourceName}.png")?.use { it.readBytes() }
    }

    private fun sizeOf(bytes: ByteArray): Pair<Int, Int> {
        val image = assertNotNull(ImageIO.read(ByteArrayInputStream(bytes)), "PNG 디코딩 실패")
        return image.width to image.height
    }

    // M1 — 앱이 실제로 읽는 경로(classpath)에 파일이 있어야 한다.
    // 없으면 Desktop 화면이 그려지는 순간 MissingResourceException으로 죽는다 (동기 로딩)
    @Test
    fun everyMascotIsOnClasspath() {
        PassmateMascots.entries.forEach { mascot ->
            assertNotNull(classpathBytes(mascot), "classpath에 drawable/${mascot.resourceName}.png 없음")
        }
    }

    // M2 — Android용과 Desktop용 사본이 갈라지면 플랫폼마다 다른 마스코트가 보인다
    @Test
    fun androidAndJvmCopiesAreIdentical() {
        PassmateMascots.entries.forEach { mascot ->
            val android = androidFile(mascot)
            val jvm = jvmFile(mascot)

            assertTrue(android.isFile, "${android.path} 없음")
            assertTrue(jvm.isFile, "${jvm.path} 없음")
            assertTrue(
                android.readBytes().contentEquals(jvm.readBytes()),
                "${mascot.name}: android/jvm 사본이 다르다"
            )
        }
    }

    // M3 — 5개가 같은 캔버스여야 상태를 바꿔도 몸통 크기·위치가 흔들리지 않는다 (PassmateMascot의 전제)
    @Test
    fun everyMascotSharesTheSameCanvas() {
        PassmateMascots.entries.forEach { mascot ->
            val size = sizeOf(assertNotNull(classpathBytes(mascot)))

            assertEquals(canvasWidth to canvasHeight, size, "${mascot.name}: 캔버스 크기가 다르다")
        }
    }

    // M4 — iOS 미러가 빠지면 SwiftUI에서 빈 이미지가 나온다. 배율별 파일 크기도 캔버스 비율을 지켜야 한다
    @Test
    fun everyMascotHasIosImageSet() {
        PassmateMascots.entries.forEach { mascot ->
            val dir = iosDir(mascot)

            assertTrue(File(dir, "Contents.json").isFile, "${dir.path}/Contents.json 없음")

            listOf("" to 1, "@2x" to 2, "@3x" to 3).forEach { (suffix, scale) ->
                val file = File(dir, "mascot_${mascot.name.lowercase()}$suffix.png")

                assertTrue(file.isFile, "${file.path} 없음")
                assertEquals(
                    (canvasWidth / 4 * scale) to (canvasHeight / 4 * scale),
                    sizeOf(file.readBytes()),
                    "${mascot.name} ${scale}x: 크기가 캔버스 비율과 다르다"
                )
            }
        }
    }
}
