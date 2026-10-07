import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing. The keystore and its passwords live outside git (android/key.properties is ignored); see docs/release.md.
val keyProps = Properties().apply {
    val f = rootProject.file("key.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// "Ask Sakshi" language service (NVIDIA NIM). The key lives in android/nim.properties, which is git-ignored, and is compiled into the
// build; without the file the chat answers offline. A key compiled into an APK can be read out of it: use a throwaway key. See docs/ask.md.
val nimProps = Properties().apply {
    val f = rootProject.file("nim.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun nim(name: String, default: String) = "\"" + (nimProps.getProperty(name) ?: default).trim() + "\""

android {
    namespace = "com.kleos.sakshi"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.kleos.sakshi"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 29
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        buildConfigField("String", "NIM_API_KEY", nim("NIM_API_KEY", ""))
        buildConfigField("String", "NIM_MODEL", nim("NIM_MODEL", "openai/gpt-oss-20b"))
        buildConfigField("String", "NIM_IDEAS_MODEL", nim("NIM_IDEAS_MODEL", "nvidia/nemotron-3-super-120b-a12b"))
        buildConfigField("String", "NIM_BASE_URL", nim("NIM_BASE_URL", "https://integrate.api.nvidia.com/v1"))
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true   // WorkManager's own resources, needed by SchedulerTest
    }

    signingConfigs {
        if (keyProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 and resource shrinking stay on at Flutter's defaults. Without key.properties the build falls back to the
            // debug key so a fresh clone still builds, and tools/audit_apk.sh refuses that APK.
            signingConfig = if (keyProps.isNotEmpty()) signingConfigs.getByName("release") else {
                logger.warn("WARNING: android/key.properties is missing, so this release APK is signed with the DEBUG key.")
                signingConfigs.getByName("debug")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

flutter {
    source = "../.."
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

// The manifest scan in arch/ReleaseManifestTest reads the merged release manifest, so build it first.
tasks.withType<Test>().configureEach {
    dependsOn("processReleaseMainManifest")
    systemProperty(
        "merged.release.manifest",
        rootProject.layout.buildDirectory.get().asFile.resolve(
            "app/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml").absolutePath)
}

// With Android resources in unit tests, the packaging task reads Flutter's merged assets; declare the dependency Gradle asks for.
tasks.matching { it.name.startsWith("package") && it.name.endsWith("UnitTestForUnitTest") }.configureEach {
    val variant = name.removePrefix("package").removeSuffix("UnitTestForUnitTest")
    dependsOn("copyFlutterAssets$variant")
}
