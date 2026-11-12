import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.znarfm.siphon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.znarfm.siphon"
        minSdk = 29
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.0"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = listOf(
                rootProject.file("keystore.properties"),
                project.file("keystore.properties")
            ).firstOrNull { it.exists() }

            val keystoreProperties = Properties().apply {
                if (keystorePropertiesFile != null) {
                    keystorePropertiesFile.inputStream().use { load(it) }
                }
            }

            val storeFilePath = keystoreProperties.getProperty("storeFile")
                ?: System.getenv("KEYSTORE_FILE")
            val resolvedStoreFile = storeFilePath?.let { path ->
                val rawFile = File(path)
                if (rawFile.isAbsolute && rawFile.exists()) {
                    rawFile
                } else {
                    listOf(
                        rootProject.file(path),
                        project.file(path),
                        rootProject.file("app/$path"),
                        rawFile
                    ).firstOrNull { it.exists() } ?: rootProject.file(path)
                }
            }

            val storePasswordProp = keystoreProperties.getProperty("storePassword")
                ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAliasProp = keystoreProperties.getProperty("keyAlias")
                ?: System.getenv("KEY_ALIAS")
            val keyPasswordProp = keystoreProperties.getProperty("keyPassword")
                ?: System.getenv("KEY_PASSWORD")

            if (resolvedStoreFile != null && resolvedStoreFile.exists() &&
                !storePasswordProp.isNullOrBlank() &&
                !keyAliasProp.isNullOrBlank() &&
                !keyPasswordProp.isNullOrBlank()
            ) {
                logger.lifecycle("Release signing configured with keystore: ${resolvedStoreFile.absolutePath} (alias: $keyAliasProp)")
                storeFile = resolvedStoreFile
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            } else {
                logger.warn("Release signing not configured: fileExists=${resolvedStoreFile?.exists()} (path=$storeFilePath, resolved=${resolvedStoreFile?.absolutePath}), hasStorePass=${!storePasswordProp.isNullOrBlank()}, hasAlias=${!keyAliasProp.isNullOrBlank()}, hasKeyPass=${!keyPasswordProp.isNullOrBlank()}")
                val debugStore = getByName("debug").storeFile
                if (debugStore != null && debugStore.exists()) {
                    initWith(getByName("debug"))
                }
            }
        }
    }

    buildTypes {
        debug {
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        release {
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null && releaseConfig.storeFile!!.exists()) {
                signingConfig = releaseConfig
                logger.lifecycle("Release buildType: assigned signingConfig 'release' (storeFile=${releaseConfig.storeFile})")
            } else {
                logger.warn("Release buildType: release signingConfig omitted (storeFile=${releaseConfig.storeFile})")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
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
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        )
    }
}

composeCompiler {
    enableStrongSkippingMode = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.libtorrent4j)
    implementation(libs.libtorrent4j.android.arm64)
    implementation(libs.libtorrent4j.android.x64)

    testImplementation(libs.junit)
}
