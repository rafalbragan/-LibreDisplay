// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("io.github.takahirom.roborazzi") version "1.42.0" apply false
}

val appId = "com.libredisplay"
val mainActivity = ".MainActivity"

fun isWindows(): Boolean = System.getProperty("os.name").contains("Windows", ignoreCase = true)

data class ConnectedAndroidDevice(
    val serial: String,
    val state: String,
    val isEmulator: Boolean
)

data class DeviceGatePolicy(
    val allowPhysicalOps: Boolean,
    val allowlistedPhysicalSerials: Set<String>,
    val protectedPhysicalSerials: Set<String>,
    val requestedTargetSerial: String?
)

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

fun adbCommand(vararg args: String): Array<String> {
    val localPropertiesFile = rootProject.file("local.properties")
    val sdkDir = if (localPropertiesFile.exists()) {
        java.util.Properties().apply {
            localPropertiesFile.inputStream().use { load(it) }
        }.getProperty("sdk.dir")
    } else {
        null
    }
    val adbName = if (isWindows()) "adb.exe" else "adb"
    val adbFromSdk = sdkDir
        ?.let { java.io.File(it, "platform-tools/$adbName") }
        ?.takeIf { it.exists() }
        ?.absolutePath
    val executable = adbFromSdk ?: "adb"
    return arrayOf(executable, *args)
}

fun parseConnectedDevices(devicesOutput: String): List<ConnectedAndroidDevice> {
    return devicesOutput
        .lineSequence()
        .drop(1)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
            val serial = parts.getOrNull(0).orEmpty()
            val state = parts.getOrNull(1).orEmpty()
            val isEmulator = serial.startsWith("emulator-")
            ConnectedAndroidDevice(serial = serial, state = state, isEmulator = isEmulator)
        }
        .filter { it.serial.isNotBlank() && it.state.isNotBlank() }
        .toList()
}

fun parseSerialCsv(raw: String?): Set<String> = raw
    ?.split(Regex("[,;\\s]+"))
    ?.map { it.trim() }
    ?.filter { it.isNotBlank() }
    ?.toSet()
    ?: emptySet()

fun isRiskyDeviceTaskName(taskName: String): Boolean =
    (taskName.startsWith("connected") && taskName.endsWith("AndroidTest")) ||
        taskName.startsWith("install") ||
        taskName.startsWith("uninstall") ||
        taskName == "installAndRun" ||
        taskName == "removeApp"

fun resolveConnectedDevices(): List<ConnectedAndroidDevice> {
    val devicesOutput = runCatching { runAndCapture(*adbCommand("devices", "-l")) }
        .getOrElse { error ->
            throw GradleException(
                "Blocked device tasks: cannot verify connected Android devices (adb unavailable or failed: ${error.message})."
            )
        }

    if (!devicesOutput.contains("List of devices attached", ignoreCase = true)) {
        throw GradleException(
            "Blocked device tasks: adb output is not trusted for device detection. Output: $devicesOutput"
        )
    }
    return parseConnectedDevices(devicesOutput)
}

fun resolveDeviceGatePolicy(): DeviceGatePolicy {
    val allowPhysicalOps = (findProperty("allowPhysicalDeviceOps") as? String)
        ?.equals("true", ignoreCase = true) == true ||
        (System.getenv("ALLOW_PHYSICAL_DEVICE_OPS") ?: "").equals("true", ignoreCase = true)

    val allowlistedPhysicalSerials = parseSerialCsv(
        (findProperty("safePhysicalDeviceSerials") as? String)
            ?: System.getenv("SAFE_PHYSICAL_DEVICE_SERIALS")
    )

    val protectedDefaults = setOf("R5GL1397F1M")
    val protectedExtras = parseSerialCsv(
        (findProperty("protectedPhysicalDeviceSerials") as? String)
            ?: System.getenv("PROTECTED_PHYSICAL_DEVICE_SERIALS")
    )

    val requestedTargetSerial = (findProperty("deviceSerial") as? String)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: (System.getenv("ANDROID_SERIAL") ?: "").trim().takeIf { it.isNotBlank() }

    return DeviceGatePolicy(
        allowPhysicalOps = allowPhysicalOps,
        allowlistedPhysicalSerials = allowlistedPhysicalSerials,
        protectedPhysicalSerials = protectedDefaults + protectedExtras,
        requestedTargetSerial = requestedTargetSerial
    )
}

fun requireSafeDeviceTasks(taskPaths: List<String>) {
    val connected = resolveConnectedDevices().filter { it.state == "device" }
    val bySerial = connected.associateBy { it.serial }
    if (connected.isEmpty()) {
        throw GradleException(
            "Blocked device tasks ${taskPaths.joinToString(", ")}: no online Android device/emulator is connected."
        )
    }

    val policy = resolveDeviceGatePolicy()
    val targetSerial = policy.requestedTargetSerial

    // Always force deterministic targeting when selection is ambiguous.
    if (targetSerial == null && connected.size != 1) {
        throw GradleException(
            "Blocked device tasks ${taskPaths.joinToString(", ")}: multiple devices detected. " +
                "Set ANDROID_SERIAL or -PdeviceSerial=<serial> to target one explicit device."
        )
    }

    val effectiveTarget = targetSerial ?: connected.single().serial
    val targetDevice = bySerial[effectiveTarget]
        ?: throw GradleException(
            "Blocked device tasks ${taskPaths.joinToString(", ")}: requested target serial '$effectiveTarget' is not connected."
        )

    if (policy.protectedPhysicalSerials.contains(effectiveTarget) && !targetDevice.isEmulator) {
        throw GradleException(
            "Blocked device tasks ${taskPaths.joinToString(", ")}: serial '$effectiveTarget' is explicitly protected (Product Owner phone)."
        )
    }

    if (!targetDevice.isEmulator) {
        if (!policy.allowPhysicalOps) {
            throw GradleException(
                "Blocked device tasks ${taskPaths.joinToString(", ")}: physical target '$effectiveTarget' requires explicit authorization " +
                    "(-PallowPhysicalDeviceOps=true or ALLOW_PHYSICAL_DEVICE_OPS=true)."
            )
        }
        if (!policy.allowlistedPhysicalSerials.contains(effectiveTarget)) {
            throw GradleException(
                "Blocked device tasks ${taskPaths.joinToString(", ")}: physical target '$effectiveTarget' is not allowlisted. " +
                    "Configure safePhysicalDeviceSerials / SAFE_PHYSICAL_DEVICE_SERIALS for dedicated test devices only."
            )
        }
    }
}

fun requireSafeDeviceTask(taskPath: String) {
    requireSafeDeviceTasks(listOf(taskPath))
}

fun resolveSafeAdbTargetSerial(): String {
    val connected = resolveConnectedDevices().filter { it.state == "device" }
    val requested = resolveDeviceGatePolicy().requestedTargetSerial
    if (requested != null) return requested
    if (connected.size == 1 && connected.first().isEmulator) {
        return connected.first().serial
    }
    throw GradleException(
        "Blocked adb command: set ANDROID_SERIAL or -PdeviceSerial=<serial> for deterministic targeting."
    )
}

tasks.register("verifySafeDeviceGate") {
    group = "verification"
    description = "Runs physical-device safety gate check without installing or uninstalling anything."
    doLast {
        requireSafeDeviceTask(path)
        println("SAFE DEVICE GATE: PASS (no unauthorized physical-device operations allowed).")
    }
}

allprojects {
    tasks.configureEach {
        val isRiskyDeviceTask = isRiskyDeviceTaskName(name)
        if (isRiskyDeviceTask) {
            doFirst {
                requireSafeDeviceTask(path)
            }
        }
    }
}

gradle.taskGraph.whenReady {
    val riskyTasks = allTasks
        .filter { isRiskyDeviceTaskName(it.name) }
        .map { it.path }
        .distinct()
    if (riskyTasks.isNotEmpty()) {
        requireSafeDeviceTasks(riskyTasks)
    }
}

tasks.register("checkEnvironment") {
    notCompatibleWithConfigurationCache("Uses shell process execution for environment diagnostics.")
    group = "verification"
    description = "Shows Java/Gradle/SDK info and checks adb/device availability."
    doLast {
        println("=== LibreCare Environment Check ===")
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
    description = "Starts filtered logcat for LibreCare, AndroidRuntime and System.err tags."
    doLast {
        val cmd = if (isWindows()) {
            arrayOf("cmd", "/c", "adb", "logcat", "LibreCare:D", "AndroidRuntime:E", "System.err:W", "*:S")
        } else {
            arrayOf("adb", "logcat", "LibreCare:D", "AndroidRuntime:E", "System.err:W", "*:S")
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
        val targetSerial = resolveSafeAdbTargetSerial()
        val cmd = if (isWindows()) {
            arrayOf("cmd", "/c", "adb", "-s", targetSerial, "shell", "am", "start", "-n", "$appId/$mainActivity")
        } else {
            arrayOf("adb", "-s", targetSerial, "shell", "am", "start", "-n", "$appId/$mainActivity")
        }
        exec {
            commandLine(*cmd)
        }
    }
}

tasks.register("removeApp") {
    notCompatibleWithConfigurationCache("Uses adb command execution.")
    group = "device"
    description = "Uninstalls LibreCare application from connected device."
    doLast {
        val targetSerial = resolveSafeAdbTargetSerial()
        val cmd = if (isWindows()) {
            arrayOf("cmd", "/c", "adb", "-s", targetSerial, "uninstall", appId)
        } else {
            arrayOf("adb", "-s", targetSerial, "uninstall", appId)
        }
        exec {
            commandLine(*cmd)
            isIgnoreExitValue = true
        }
    }
}

tasks.register("helpNoSdk") {
    group = "help"
    description = "Shows workflow for working without a local Android SDK."
    doLast {
        println("=== LibreCare: praca bez lokalnego Android SDK ===")
        println("Lokalnie bez SDK nie uruchomisz: assembleDebug/installDebug/bundleRelease.")
        println("Mozesz pracowac nad kodem i budowac APK w GitHub Actions.")
        println("")
        println("Kroki:")
        println("1) git add .")
        println("2) git commit -m \"Twoja zmiana\"")
        println("3) git push origin master")
        println("4) GitHub -> Actions -> Android APK Build")
        println("5) Pobierz artefakt: LibreCare-debug")
        println("")
        println("Taski, ktore dzialaja bez SDK lokalnie:")
        println("- checkEnvironment")
        println("- helpNoSdk")
    }
}

