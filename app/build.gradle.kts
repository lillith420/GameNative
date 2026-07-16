import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets.gradle)
    alias(libs.plugins.room)
}

val keystorePropertiesFile = rootProject.file("app/keystores/keystore.properties")
val keystoreProperties: Properties? = if (keystorePropertiesFile.exists()) {
    Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }
} else null

room {
    schemaDirectory("$projectDir/schemas")
}

val copyDebugManifest by tasks.registering(Copy::class) {
    from(rootProject.file("manifest.json"))
    into(layout.buildDirectory.dir("generated/debugManifest"))
}

android {
    namespace = "app.gamenative"
    compileSdk = 36
    ndkVersion = "27.3.13750724"

    signingConfigs {
        create("pluvia") {
            if (keystoreProperties != null) {
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
            }
        }
    }

    defaultConfig {
        applicationId = "app.gamenative"
        minSdk = 26

        manifestPlaceholders["screenOrientation"] = "unspecified"
        buildConfigField("boolean", "XR_BUILD", "false")
        buildConfigField("boolean", "MODERN_XR", "false")

        versionCode = 19
        versionName = "1.1.1"

        buildConfigField("boolean", "GOLD", "false")

        buildConfigField("String", "CLOUD_PROJECT_NUMBER", """ "0" """)
        buildConfigField("String", "STEAMGRIDDB_API_KEY", """ "65063399717227a51f34e00ffee55932" """)

        val iconValue = "@mipmap/ic_launcher"
        val iconRoundValue = "@mipmap/ic_launcher_round"
        manifestPlaceholders.putAll(
            mapOf(
                "icon" to iconValue,
                "roundIcon" to iconRoundValue,
            ),
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        proguardFiles(
            getDefaultProguardFile("proguard-android.txt"),
            "proguard-rules.pro",
        )
    }

    flavorDimensions += "androidApi"
    productFlavors {
        create("legacy") {
            dimension = "androidApi"
            targetSdk = 28
            ndk.abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            buildConfigField("boolean", "MODERN_ANDROID", "false")
            buildConfigField("String", "PRELOAD_BIONIC_SO", "\"libredirect-bionic.so\"")
        }
        create("legacyXr") {
            dimension = "androidApi"
            targetSdk = 28
            ndk.abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            buildConfigField("boolean", "MODERN_ANDROID", "false")
            buildConfigField("String", "PRELOAD_BIONIC_SO", "\"libredirect-bionic.so\"")
            buildConfigField("boolean", "XR_BUILD", "true")
            manifestPlaceholders["screenOrientation"] = "landscape"
        }
        create("modern") {
            dimension = "androidApi"
            minSdk = 29
            targetSdk = 36
            ndk.abiFilters += listOf("arm64-v8a")
            buildConfigField("boolean", "MODERN_ANDROID", "true")
            buildConfigField("String", "PRELOAD_BIONIC_SO", "\"libredirect-bionic-wx.so\"")
        }
        create("modernXr") {
            dimension = "androidApi"
            minSdk = 29
            targetSdk = 36
            ndk.abiFilters += listOf("arm64-v8a")
            buildConfigField("boolean", "MODERN_ANDROID", "true")
            buildConfigField("String", "PRELOAD_BIONIC_SO", "\"libredirect-bionic-wx.so\"")
            buildConfigField("boolean", "XR_BUILD", "true")
            buildConfigField("boolean", "MODERN_XR", "true")
            buildConfigField("String", "META_APP_ID", "\"${System.getenv("META_APP_ID") ?: ""}\"")
            buildConfigField("String", "PRODUCT_SKU", "\"${System.getenv("PRODUCT_SKU") ?: ""}\"")
            manifestPlaceholders["screenOrientation"] = "landscape"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
        }
        create("release-signed") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("pluvia")
        }
        create("release-gold") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("pluvia")
            applicationIdSuffix = ".gold"
            buildConfigField("boolean", "GOLD", "true")
            val iconValue = "@mipmap/ic_launcher_gold"
            val iconRoundValue = "@mipmap/ic_launcher_gold_round"
            manifestPlaceholders.putAll(
                mapOf(
                    "icon" to iconValue,
                    "roundIcon" to iconRoundValue,
                ),
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/DebugProbesKt.bin"
            excludes += "/junit/runner/smalllogo.gif"
            excludes += "/junit/runner/logo.gif"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        disable += "ExtraTranslation"
    }
    dynamicFeatures += setOf(":ubuntufs")

    sourceSets {
        getByName("legacy") {
            java.srcDir("src/nonXr/java")
            assets {
                srcDirs("src/legacy/assets", "src/main/assets")
            }
        }
        getByName("legacyXr") {
            java.srcDir("src/nonXr/java")
            manifest.srcFile("src/legacy/AndroidManifest.xml")
            assets {
                srcDirs("src/legacy/assets", "src/main/assets")
            }
            jniLibs {
                srcDirs("src/legacy/jniLibs")
            }
        }
        getByName("modern") {
            java.srcDir("src/nonXr/java")
            assets {
                srcDirs("src/modern/assets", "src/main/assets")
            }
        }
        getByName("modernXr") {
            assets {
                srcDirs("src/modern/assets", "src/main/assets")
            }
            jniLibs {
                srcDirs("src/modern/jniLibs")
            }
        }
        getByName("debug") {
            assets.srcDir(copyDebugManifest)
        }
    }
}

dependencies {
    implementation(libs.material)
    implementation("androidx.browser:browser:1.8.0")
    val localBuild = false
    if (localBuild) {
        implementation(files("../../JavaSteam/build/libs/javasteam-1.8.0.1-22-SNAPSHOT.jar"))
        implementation(files("../../JavaSteam/javasteam-depotdownloader/build/libs/javasteam-depotdownloader-1.8.0.1-22-SNAPSHOT.jar"))
        implementation(libs.bundles.javasteam.dev)
    } else {
        implementation(libs.javasteam) { isChanging = version?.contains("SNAPSHOT") ?: false }
        implementation(libs.javasteam.depotdownloader) { isChanging = version?.contains("SNAPSHOT") ?: false }
    }
    implementation(libs.spongycastle)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.bundles.google)
    implementation(libs.bundles.winlator)
    implementation(libs.libarchive.android)
    implementation(libs.zstd.jni) { artifact { type = "aar" } }
    implementation(libs.xz)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.landscapist.coil)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.apng)
    implementation(libs.datastore.preferences)
    implementation(libs.jetbrains.kotlinx.json)
    implementation(libs.kotlin.coroutines)
    implementation(libs.timber)
    implementation(libs.zxing)
    implementation(libs.protobuf.java)
    implementation(libs.bundles.hilt)
    ksp(libs.bundles.ksp)
    implementation(libs.bundles.room)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.zstd.jni)
    testImplementation(libs.orgJson)
    testImplementation(libs.mockwebserver)

    implementation("com.auth0.android:jwtdecode:2.0.2")

    "modernXrImplementation"("com.meta.horizon.platform.sdk:core-kotlin:0.2.2")
    "modernXrImplementation"("com.meta.horizon.platform.sdk:iap-kotlin:0.2.2")
}
