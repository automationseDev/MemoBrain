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

val explicitDevelopKeystorePath = secretValue("MEMOBRAIN_DEVELOP_KEYSTORE_PATH", signingSecrets)
val explicitDevelopKeystorePassword = secretValue("MEMOBRAIN_DEVELOP_KEYSTORE_PASSWORD", signingSecrets)
val explicitDevelopKeyAlias = secretValue("MEMOBRAIN_DEVELOP_KEY_ALIAS", signingSecrets)
val explicitDevelopKeyPassword = secretValue("MEMOBRAIN_DEVELOP_KEY_PASSWORD", signingSecrets)
val explicitDevelopSigningValues = listOf(
    explicitDevelopKeystorePath,
    explicitDevelopKeystorePassword,
    explicitDevelopKeyAlias,
    explicitDevelopKeyPassword
)
val explicitDevelopSigningConfigured = explicitDevelopSigningValues.any { it.isNotEmpty() }
val explicitDevelopSigningReady = explicitDevelopSigningValues.all { it.isNotEmpty() }
val developKeystoreFile = if (explicitDevelopKeystorePath.isNotEmpty()) {
    rootProject.file(explicitDevelopKeystorePath)
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
        versionCode = 13
        versionName = "1.4.0"
        manifestPlaceholders["appLabel"] = "MemoBrain"
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
        if (explicitDevelopSigningReady) {
            create("develop") {
                storeFile = developKeystoreFile
                storePassword = explicitDevelopKeystorePassword
                keyAlias = explicitDevelopKeyAlias
                keyPassword = explicitDevelopKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-develop"
            manifestPlaceholders["appLabel"] = "MemoBrain Develop"

            // Reuse the configured persistent signing key so Develop APKs remain
            // updateable across machines instead of relying on transient debug keys.
            if (explicitDevelopSigningReady) {
                signingConfig = signingConfigs.getByName("develop")
            } else if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }

            // No native AdMob unit IDs are compiled into MemoBrain.
            // Google Mobile Ads SDK is present only for WebView API for Ads.
        }

        getByName("release") {
            isMinifyEnabled = false
            manifestPlaceholders["appLabel"] = "MemoBrain"

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

val validateDevelopSigning by tasks.registering {
    group = "verification"
    description = "Rejects incomplete signing configuration instead of silently changing the Develop signing key."

    doLast {
        if (explicitDevelopSigningConfigured && !explicitDevelopSigningReady) {
            throw GradleException("Develop-specific signing settings are incomplete. Set all MEMOBRAIN_DEVELOP_KEYSTORE_PATH, MEMOBRAIN_DEVELOP_KEYSTORE_PASSWORD, MEMOBRAIN_DEVELOP_KEY_ALIAS, and MEMOBRAIN_DEVELOP_KEY_PASSWORD values.")
        }
        if (explicitDevelopSigningReady && (developKeystoreFile == null || !developKeystoreFile.isFile)) {
            throw GradleException("The configured MemoBrain Develop keystore file does not exist: $explicitDevelopKeystorePath")
        }
        val signingValues = listOf(releaseKeystorePath, releaseKeystorePassword, releaseKeyAlias, releaseKeyPassword)
        if (!explicitDevelopSigningReady && signingValues.any { it.isNotEmpty() } && !releaseSigningReady) {
            throw GradleException("Develop signing settings are incomplete. Set all MEMOBRAIN_KEYSTORE_PATH, MEMOBRAIN_KEYSTORE_PASSWORD, MEMOBRAIN_KEY_ALIAS, and MEMOBRAIN_KEY_PASSWORD values to keep the signing certificate stable.")
        }
        if (!explicitDevelopSigningReady && releaseSigningReady && (releaseKeystoreFile == null || !releaseKeystoreFile.isFile)) {
            throw GradleException("The configured MemoBrain Develop keystore file does not exist: $releaseKeystorePath")
        }
    }
}

tasks.matching { it.name == "assembleDebug" || it.name == "bundleDebug" }.configureEach {
    dependsOn(validateDevelopSigning)
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
