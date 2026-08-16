import java.util.Properties

plugins { id("com.android.application") }

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val googleTestAppId = "ca-app-pub-3940256099942544~3347511713"
val googleTestBannerId = "ca-app-pub-3940256099942544/6300978111"

fun loadProperties(fileName: String): Properties = Properties().apply {
    val source = rootProject.file(fileName)
    if (source.isFile) source.inputStream().use { load(it) }
}

val releaseSecrets = loadProperties("release-secrets.properties")
val signingSecrets = loadProperties("signing-secrets.properties")

fun secretValue(name: String, source: Properties): String {
    val projectValue = project.findProperty(name)?.toString()?.trim().orEmpty()
    if (projectValue.isNotEmpty()) return projectValue

    val fileValue = source.getProperty(name)?.trim().orEmpty()
    if (fileValue.isNotEmpty()) return fileValue

    return System.getenv(name)?.trim().orEmpty()
}

val releaseAdmobAppId = secretValue("MEMOBRAIN_ADMOB_APP_ID", releaseSecrets)
val releaseAdmobBannerId = secretValue("MEMOBRAIN_ADMOB_BANNER_ID", releaseSecrets)

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
        versionCode = 7
        versionName = "1.0.0"
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
            manifestPlaceholders["ADMOB_APP_ID"] = googleTestAppId
            buildConfigField("String", "ADMOB_BANNER_ID", quoted(googleTestBannerId))
        }

        getByName("release") {
            val appId = if (releaseAdmobAppId.isNotEmpty()) releaseAdmobAppId else googleTestAppId
            val bannerId = if (releaseAdmobBannerId.isNotEmpty()) releaseAdmobBannerId else googleTestBannerId
            manifestPlaceholders["ADMOB_APP_ID"] = appId
            buildConfigField("String", "ADMOB_BANNER_ID", quoted(bannerId))
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

val validateReleaseAdMob by tasks.registering {
    group = "verification"
    description = "Fails release builds unless production AdMob IDs are supplied at build time."

    doLast {
        fun fail(message: String): Nothing = throw GradleException(message)

        if (releaseAdmobAppId.isEmpty()) {
            fail("MEMOBRAIN_ADMOB_APP_ID is required for release builds. Use release-secrets.properties or an environment/project property.")
        }
        if (releaseAdmobBannerId.isEmpty()) {
            fail("MEMOBRAIN_ADMOB_BANNER_ID is required for release builds. Use release-secrets.properties or an environment/project property.")
        }
        if (releaseAdmobAppId == googleTestAppId || releaseAdmobBannerId == googleTestBannerId) {
            fail("Google test AdMob IDs cannot be used in a MemoBrain release build.")
        }
        if (releaseAdmobAppId.contains("0000000000000000") || releaseAdmobBannerId.contains("0000000000000000")) {
            fail("Replace the placeholder AdMob IDs in release-secrets.properties before building a release.")
        }
        if (!Regex("^ca-app-pub-[0-9]{16}~[0-9]{10}$").matches(releaseAdmobAppId)) {
            fail("MEMOBRAIN_ADMOB_APP_ID format is invalid.")
        }
        if (!Regex("^ca-app-pub-[0-9]{16}/[0-9]{10}$").matches(releaseAdmobBannerId)) {
            fail("MEMOBRAIN_ADMOB_BANNER_ID format is invalid.")
        }
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

val validateReleaseConfig by tasks.registering {
    group = "verification"
    description = "Validates production AdMob IDs and release signing configuration."
    dependsOn(validateReleaseAdMob, validateReleaseSigning)
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(validateReleaseConfig)
}

dependencies {
    implementation("androidx.work:work-runtime:2.11.2")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
}
