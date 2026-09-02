package org.sesacteamproject.passmate.component

import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.loadXmlImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// 아이콘 리소스 회귀 가드 — 파일이 사라지거나, 두 타깃 사본이 어긋나거나,
// 리소스에 시맨틱 색이 굽히면 여기서 실패한다 (계획 문서 data-model.md V1~V4)
class PassmateIconResourceTest {

    private val neutralColors = setOf("#00000000", "#FF000000")

    private fun moduleDir(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null && !File(dir, "src/androidMain/res").isDirectory) {
            dir = dir.parentFile
        }
        return dir
    }

    private fun androidFile(icon: PassmateIcons): File {
        return File(moduleDir(), "src/androidMain/res/drawable/${icon.resourceName}.xml")
    }

    private fun jvmFile(icon: PassmateIcons): File {
        return File(moduleDir(), "src/jvmMain/resources/drawable/${icon.resourceName}.xml")
    }

    private fun classpathBytes(icon: PassmateIcons): ByteArray? {
        val loader = Thread.currentThread().contextClassLoader ?: javaClass.classLoader
        return loader.getResourceAsStream("drawable/${icon.resourceName}.xml")?.use { it.readBytes() }
    }

    private fun parseRoot(bytes: ByteArray): Element {
        return DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(InputSource(ByteArrayInputStream(bytes)))
            .documentElement
    }

    private fun pathElements(root: Element): List<Element> {
        val nodes = root.getElementsByTagName("path")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    // V1 — 앱이 실제로 읽는 경로(classpath)에 파일이 있어야 한다.
    // 없으면 Desktop 화면이 그려지는 순간 MissingResourceException으로 죽는다 (동기 로딩)
    @Test
    fun everyIconIsOnClasspath() {
        PassmateIcons.entries.forEach { icon ->
            assertNotNull(classpathBytes(icon), "classpath에 drawable/${icon.resourceName}.xml 없음")
        }
    }

    // V2 — Android용과 Desktop용 사본이 갈라지면 플랫폼마다 다른 아이콘이 보인다
    @Test
    fun androidAndJvmCopiesAreIdentical() {
        PassmateIcons.entries.forEach { icon ->
            val android = androidFile(icon)
            val jvm = jvmFile(icon)

            assertTrue(android.isFile, "${android.path} 없음")
            assertTrue(jvm.isFile, "${jvm.path} 없음")
            assertEquals(android.readText(), jvm.readText(), "${icon.name}: android/jvm 사본이 다르다")
        }
    }

    // V3 — 리소스에 시맨틱 색을 구우면 재사용이 막히고 색 토큰 규칙(§11-2)과 어긋난다
    @Test
    fun resourcesUseNeutralColorsOnly() {
        PassmateIcons.entries.forEach { icon ->
            val root = parseRoot(assertNotNull(classpathBytes(icon)))

            pathElements(root).forEach { path ->
                listOf("fillColor", "strokeColor").forEach { name ->
                    val value = path.getAttribute("android:$name")

                    if (value.isNotEmpty()) {
                        assertTrue(
                            value.uppercase() in neutralColors,
                            "${icon.name}: $name=$value 는 중립색이 아니다"
                        )
                    }
                }
            }
        }
    }

    // V4 — Desktop 화면과 동일한 경로(loadXmlImageVector)로 파싱되고 뷰포트·경로가 온전해야 한다
    @Test
    fun everyIconParsesAsVector() {
        PassmateIcons.entries.forEach { icon ->
            val bytes = assertNotNull(classpathBytes(icon))
            val root = parseRoot(bytes)
            val paths = pathElements(root)

            assertEquals("vector", root.tagName, "${icon.name}: 루트가 <vector>가 아니다")
            assertEquals("24", root.getAttribute("android:viewportWidth"), "${icon.name}: 뷰포트 폭")
            assertEquals("24", root.getAttribute("android:viewportHeight"), "${icon.name}: 뷰포트 높이")
            assertTrue(paths.isNotEmpty(), "${icon.name}: <path>가 없다")

            paths.forEach { path ->
                val data = path.getAttribute("android:pathData")

                assertTrue(data.isNotEmpty(), "${icon.name}: pathData 비어 있음")
                assertTrue(addPathNodes(data).isNotEmpty(), "${icon.name}: pathData 파싱 실패 — $data")
            }

            val image = loadXmlImageVector(InputSource(ByteArrayInputStream(bytes)), Density(1f))

            assertEquals(24.dp, image.defaultWidth, "${icon.name}: defaultWidth")
            assertEquals(24.dp, image.defaultHeight, "${icon.name}: defaultHeight")
        }
    }
}
