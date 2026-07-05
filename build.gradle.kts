// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val appId = "com.libredisplay"
val mainActivity = ".MainActivity"

fun isWindows(): Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)

fun runAndCapture(vararg command: String): String {
    val output = java.io.ByteArrayOutputStream()
    exec {
        commandLine(*command)
        standardOutput = output
        errorOutput = output
        isIgnoreExitValue = true
    }
    return output.toString().trim()
}

tasks.register("checkEnvironment") {
    notCompatibleWithConfigurationCache("Uses shell process execution for environment diagnostics.")
    group = "verification"
    description = "Shows Java/Gradle/SDK info and checks adb/device availability."
    doLast {
        println("=== LibreDisplay Environment Check ===")
        val javaHomeEnv = System.getenv("JAVA_HOME") ?: "NOT SET"
        val androidHomeEnv = System.getenv("ANDROID_HOME") ?: "NOT SET"
        val androidSdkRootEnv = System.getenv("ANDROID_SDK_ROOT") ?: "NOT SET"
        println("Java runtime: ${System.getProperty("java.runtime.version") ?: "unknown"}")
        println("Java home: ${System.getProperty("java.home") ?: "unknown"}")
        println("JAVA_HOME: $javaHomeEnv")
        println("Gradle version: ${gradle.gradleVersion}")

        println("ANDROID_HOME: $androidHomeEnv")
        println("ANDROID_SDK_ROOT: $androidSdkRootEnv")

        val localPropertiesFile = rootProject.file("local.properties")
        val localSdkDir = if (localPropertiesFile.exists()) {
            java.util.Properties().apply {
                localPropertiesFile.inputStream().use { load(it) }
            }.getProperty("sdk.dir")
        } else null
        println("local.properties sdk.dir: ${localSdkDir ?: "NOT SET"}")

        val sdkRoot = localSdkDir ?: System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME")
        println("Android SDK location (effective): ${sdkRoot ?: "NOT SET"}")
        println("compileSdk: 35")
        println("targetSdk: 35")
        println("minSdk: 26")

        val sdkValid = sdkRoot != null && file(sdkRoot).exists()
        val platformsValid = sdkRoot != null && file("$sdkRoot/platforms/android-35").exists()
        val buildToolsValid = sdkRoot != null && file("$sdkRoot/build-tools/35.0.0").exists()
        val platformToolsValid = sdkRoot != null && file("$sdkRoot/platform-tools").exists()
        println("SDK configured: $sdkValid")
        println("Android Platform 35 installed: $platformsValid")
        println("Build Tools 35.0.0 installed: $buildToolsValid")
        println("Platform Tools installed: $platformToolsValid")

        val adbCommand = if (isWindows()) arrayOf("cmd", "/c", "adb", "version") else arrayOf("adb", "version")
        val adbVersion = runCatching { runAndCapture(*adbCommand) }.getOrElse { "adb command not found" }
        val adbAvailable = !adbVersion.contains("not found", ignoreCase = true) && !adbVersion.contains("not recognized", ignoreCase = true)
        println("adb available in PATH: $adbAvailable")
        if (adbAvailable) {
            println(adbVersion.lineSequence().firstOrNull() ?: "")
        }

        if (adbAvailable) {
            val devicesCommand = if (isWindows()) arrayOf("cmd", "/c", "adb", "devices") else arrayOf("adb", "devices")
            val devicesOutput = runAndCapture(*devicesCommand)
            println("--- adb devices ---")
            println(devicesOutput)
            val hasDevice = devicesOutput
                .lineSequence()
                .drop(1)
                .any { it.trim().endsWith("\tdevice") }
            println("Phone connected (status=device): $hasDevice")
        }
    }
}

tasks.register("showDevices") {
    notCompatibleWithConfigurationCache("Uses adb command execution.")
    group = "device"
    description = "Prints connected Android devices using adb devices."
    doLast {
        val output = runAndCapture(*(if (isWindows()) arrayOf("cmd", "/c", "adb", "devices") else arrayOf("adb", "devices")))
        println(output)
        val hasDevice = output
            .lineSequence()
            .drop(1)
            .any { it.trim().endsWith("\tdevice") }
        println("Phone connected (status=device): $hasDevice")
    }
}

tasks.register("showLogs") {
    notCompatibleWithConfigurationCache("Uses interactive adb logcat process.")
    group = "device"
    description = "Starts filtered logcat for LibreDisplay, AndroidRuntime and System.err tags."
    doLast {
        val cmd = if (isWindows()) {
            arrayOf("cmd", "/c", "adb", "logcat", "LibreDisplay:D", "AndroidRuntime:E", "System.err:W", "*:S")
        } else {
            arrayOf("adb", "logcat", "LibreDisplay:D", "AndroidRuntime:E", "System.err:W", "*:S")
        }
        exec {
            commandLine(*cmd)
        }
    }
}

tasks.register("installAndRun") {
    notCompatibleWithConfigurationCache("Uses adb command execution.")
    group = "device"
    description = "Builds debug APK, installs on device and starts MainActivity."
    dependsOn(":app:installDebug")
    doLast {
        val cmd = if (isWindows()) {
            arrayOf("cmd", "/c", "adb", "shell", "am", "start", "-n", "$appId/$mainActivity")
        } else {
            arrayOf("adb", "shell", "am", "start", "-n", "$appId/$mainActivity")
        }
        exec {
            commandLine(*cmd)
        }
    }
}

tasks.register("removeApp") {
    notCompatibleWithConfigurationCache("Uses adb command execution.")
    group = "device"
    description = "Uninstalls LibreDisplay application from connected device."
    doLast {
        val cmd = if (isWindows()) {
            arrayOf("cmd", "/c", "adb", "uninstall", appId)
        } else {
            arrayOf("adb", "uninstall", appId)
        }
        exec {
            commandLine(*cmd)
            isIgnoreExitValue = true
        }
    }
}

