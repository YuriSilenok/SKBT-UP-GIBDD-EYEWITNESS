import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Public build-time configuration. CLI -P overrides local and tracked files.
val appConfig = Properties().apply {
    rootProject.file("config/app.properties").reader(Charsets.UTF_8).use { load(it) }
    rootProject.file("config/local.properties").takeIf { it.exists() }?.reader(Charsets.UTF_8)?.use { load(it) }
}
fun configured(name: String, fallback: String = ""): String =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: appConfig.getProperty(name)?.takeIf { it.isNotBlank() } ?: fallback
fun httpsUrl(name: String, value: String): String {
    val parsed = URI(value)
    require(parsed.scheme == "https" && parsed.host != null && parsed.userInfo == null) { "$name must be an HTTPS URL without credentials" }
    return value
}
val configuredApi = httpsUrl("apiBaseUrl", configured("apiBaseUrl"))
require(configuredApi.endsWith("/") && URI(configuredApi).rawQuery == null && URI(configuredApi).rawFragment == null) { "apiBaseUrl must end with / and have no query or fragment" }

android {
    namespace = "com.example.gibddochevidets"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        manifestPlaceholders["appLabel"] = "@string/app_name"
        buildConfigField("String", "API_BASE_URL", "\"$configuredApi\"")
        for ((key, field, path) in listOf(
            Triple("privacyPolicyUrl", "PRIVACY_POLICY_URL", "policy"),
            Triple("privacyConsentUrl", "PRIVACY_CONSENT_URL", "consent"),
            Triple("privacyRegulationUrl", "PRIVACY_REGULATION_URL", "regulation")
        )) {
            val url = httpsUrl(key, configured(key, configuredApi + "legal/" + path))
            buildConfigField("String", field, "\"$url\"")
        }
        val consentVersion = configured("consentVersion")
        require(consentVersion.matches(Regex("[A-Za-z0-9._-]{1,64}"))) { "Invalid consentVersion" }
        buildConfigField("String", "CONSENT_VERSION", "\"$consentVersion\"")

        applicationId = "com.example.gibddochevidets"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("ciVersionCode").map { it.toInt() }.orElse(1).get()
        versionName = providers.gradleProperty("ciVersionName").orElse("1.0").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // Оставляем только реальные архитектуры телефонов
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["appLabel"] = "Трезвый водитель 44 (тест)"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
        }
        release {
            isMinifyEnabled = true          // ✅ включает сжатие кода
            isShrinkResources = true        // ✅ удаляет неиспользуемые ресурсы
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // ============================================================
    // MAP
    // ============================================================


    // ============================================================
    // ANDROIDX
    // ============================================================
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")

    // ============================================================
    // COROUTINES
    // ============================================================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // ============================================================
    // RETROFIT
    // ============================================================
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ============================================================
    // OKHTTP
    // ============================================================
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ============================================================
    // COMPOSE
    // ============================================================

    // ============================================================
    // TESTS
    // ============================================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

// Reproducible third-party inventory for the distributed Android application.
tasks.register("thirdPartyInventory") {
    doLast {
        val dependencies = configurations.getByName(providers.gradleProperty("inventoryConfiguration").orElse("releaseRuntimeClasspath").get()).incoming.resolutionResult.allComponents
            .mapNotNull { component -> (component.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier)?.let {
                "${it.group}:${it.module}:${it.version}"
            } }.distinct().sorted()
        val output = layout.buildDirectory.file("reports/licenses/dependencies.txt").get().asFile
        output.parentFile.mkdirs()
        output.writeText(dependencies.joinToString("\n") + "\n")
    }
}
