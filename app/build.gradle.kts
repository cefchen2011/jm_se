import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 版本号统一由 version.properties 维护，每次 assemble 后自动递增小版本（patch）
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) versionPropsFile.inputStream().use { load(it) }
}
val appVersionCode = (versionProps.getProperty("versionCode") ?: "1").toInt()
val appVersionName = versionProps.getProperty("versionName") ?: "1.0.0"

android {
    namespace = "com.comicreader"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.comicreader"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.2")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Miuix（MIUI 风格组件库，历史版本以兼容 compileSdk 36）
    implementation("top.yukonga.miuix.kmp:miuix:0.8.8")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// 每次 assemble 完成后自动递增小版本（patch），供下一次编译使用
val bumpPatchVersion = tasks.register("bumpPatchVersion") {
    group = "build"
    description = "递增 versionCode 与 versionName 的 patch 位"
    doLast {
        val code = (versionProps.getProperty("versionCode") ?: "1").toInt() + 1
        val name = versionProps.getProperty("versionName") ?: "1.0.0"
        val parts = name.split(".")
        val bumped = if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.${(parts[2].toIntOrNull() ?: 0) + 1}"
        } else {
            "$name.1"
        }
        versionPropsFile.writeText("versionCode=$code\nversionName=$bumped\n", Charsets.UTF_8)
        println(">> 版本号已递增：$name -> $bumped (code=$code)")
    }
}

afterEvaluate {
    tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.configureEach {
        finalizedBy(bumpPatchVersion)
    }
}
