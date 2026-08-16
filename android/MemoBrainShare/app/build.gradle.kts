import java.util.Properties

plugins { id("com.android.application") }

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val googleTestAppId = "ca-app-pub-3940256099942544~3347511713"
val googleTestBannerId = "ca-app-pub-3940256099942544/6300978111"

val secretFile = rootProject.file("release-secrets.properties")
val releaseSecrets = Properties().apply {
    if (secretFile.isFile) secretFile.inputStream().use { load(it) }
}

fun releaseValue(name: String): String {
    val projectValue = project.findProperty(name)?.toString()?.trim().orEmpty()
    if (projectValue.isNotEmpty()) return projectValue
    val fileValue = releaseSecrets.getProperty(name)?.trim().orEmpty()
    if (fileValue.isNotEmpty()) return fileValue
    return System.getenv(name)?.trim().orEmpty()
}

val releaseAdmobAppId = releaseValue("MEMOBRAIN_ADMOB_APP_ID")
val releaseAdmobBannerId = releaseValue("MEMOBRAIN_ADMOB_BANNER_ID")

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

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(validateReleaseAdMob)
}

dependencies {
    implementation("androidx.work:work-runtime:2.11.2")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
}
