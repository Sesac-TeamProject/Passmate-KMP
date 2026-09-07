import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

// 실기기 테스트용 서버 주소 — local.properties(gitignore)에 PASSMATE_SERVER_HOST를 적으면 덮어쓴다.
// iOS의 Local.xcconfig와 같은 취급이다: 추적 파일을 건드리지 않는다.
val localServerHost: String = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }
    ?.getProperty("PASSMATE_SERVER_HOST")
    .orEmpty()
    .trim()

// 값은 Kotlin 파일로 굽는다. AGP의 buildConfig 기능을 켜면 이 모듈에 자바 소스(BuildConfig.java)가
// 처음으로 생기고, 그 순간 :shared:compileDebugJavaWithJavac가 돌면서 AGP 8.1.4의
// JdkImageTransform이 JDK 21의 jlink에서 깨진다 — 여태 자바 소스가 없어 건너뛰던 태스크다.
val generateServerHost by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/serverHost/kotlin")
    val host = localServerHost

    inputs.property("host", host)
    outputs.dir(outputDir)
    doLast {
        val target = outputDir.get().asFile
            .resolve("org/sesacteamproject/passmate/core/network/GeneratedServerHost.kt")

        target.parentFile.mkdirs()
        target.writeText(
            """
            package org.sesacteamproject.passmate.core.network

            // 생성 파일 — local.properties의 PASSMATE_SERVER_HOST에서 온다. 직접 고치지 않는다.
            internal const val BUILD_SERVER_HOST: String = "$host"
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = JvmTarget.JVM_11.target
            }
        }
    }
    
    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = JvmTarget.JVM_11.target
            }
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.websockets)
            implementation(libs.krossbow.stomp.core)
            implementation(libs.krossbow.stomp.kxserialization.json)
            implementation(libs.krossbow.websocket.ktor)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain {
            kotlin.srcDir(generateServerHost)
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "org.sesacteamproject.passmate.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
