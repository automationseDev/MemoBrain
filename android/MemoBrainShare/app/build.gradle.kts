import java.util.Properties

plugins { id("com.android.application") }

fun loadProperties(fileName: String): Properties = Properties().apply {
    val source = rootProject.file(fileName)
    if (source.isFile) source.inputStream().use { load(it) }
}

val signingSecrets = loadProperties("signing-secrets.properties")

fun secretValue(name: String, source: Properties): String {
    val projectValue = project.findProperty(name)?.toString()?.trim().orEmpty()
    if (projectValue.isNotEmpty()) return projectValue

    val fileValue = source.getProperty(name)?.trim().orEmpty()
    if (fileValue.isNotEmpty()) return fileValue

    return System.getenv(name)?.trim().orEmpty()
}

val releaseKeystorePath = secretValue("MEMOBRAIN_KEYSTORE_PATH", signingSecrets)
val releaseKeystorePassword = secretValue("MEMOBRAIN_KEYSTORE_PASSWORD", signingSecrets)
val releaseKeyAlias = secretValue("MEMOBRAIN_KEY_ALIAS", signingSecrets)
val releaseKeyPassword = secretValue("MEMOBRAIN_KEY_PASSWORD", signingSecrets)
val releaseSigningReady = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it.isNotEmpty() }

val releaseKeystoreFile = if (releaseKeystorePath.isNotEmpty()) {
    rootProject.file(releaseKeystorePath)
} else {
    null
}

android {
    namespace = "net.automationse.memobrainshare"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.automationse.memobrainshare"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.0"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // No native AdMob unit IDs are compiled into MemoBrain.
            // Google Mobile Ads SDK is present only for WebView API for Ads.
        }

        getByName("release") {
            isMinifyEnabled = false

            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val validateReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails release builds unless a complete local signing configuration is supplied."

    doLast {
        fun fail(message: String): Nothing = throw GradleException(message)

        if (releaseKeystorePath.isEmpty()) {
            fail("MEMOBRAIN_KEYSTORE_PATH is required for release builds. Use signing-secrets.properties or an environment/project property.")
        }
        if (releaseKeystorePassword.isEmpty()) {
            fail("MEMOBRAIN_KEYSTORE_PASSWORD is required for release builds.")
        }
        if (releaseKeyAlias.isEmpty()) {
            fail("MEMOBRAIN_KEY_ALIAS is required for release builds.")
        }
        if (releaseKeyPassword.isEmpty()) {
            fail("MEMOBRAIN_KEY_PASSWORD is required for release builds.")
        }
        if (releaseKeystoreFile == null || !releaseKeystoreFile.isFile) {
            fail("The configured MemoBrain keystore file does not exist: $releaseKeystorePath")
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(validateReleaseSigning)
}

dependencies {
    implementation("androidx.work:work-runtime:2.11.2")

    // Required only for MobileAds.registerWebView() / WebView API for Ads.
    // MemoBrain no longer uses native AdMob banners or UMP.
    implementation("com.google.android.gms:play-services-ads:25.4.0")
}
