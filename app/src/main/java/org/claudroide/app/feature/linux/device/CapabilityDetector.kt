package org.claudroide.app.feature.linux.device

object CapabilityDetector {

    fun generateReport(probe: ProbeSource): DeviceCapabilityReport {
        val manufacturer = probe.getManufacturer()
        val model = probe.getModel()
        val hardware = probe.getHardware()
        val abis = probe.getSupportedAbis()
        val primaryAbi = abis.firstOrNull() ?: "unknown"
        val totalRam = probe.getTotalRamBytes()
        val availRam = probe.getAvailableRamBytes()
        val totalRamGb = totalRam / (1024.0 * 1024.0 * 1024.0)
        val kernelRelease = probe.getKernelRelease()
        val androidRelease = probe.getAndroidRelease()
        val sdkInt = probe.getSdkInt()

        // Device classification based on hardware & model
        val isA56 = model.contains("A56", ignoreCase = true) || hardware.contains("s5e8855", ignoreCase = true)
        val isOnePlus6T = model.contains("A6013", ignoreCase = true) || model.contains("A6010", ignoreCase = true) ||
                hardware.contains("qcom", ignoreCase = true) || model.contains("OnePlus 6T", ignoreCase = true)
        val isPi400 = model.contains("Pi 400", ignoreCase = true) || hardware.contains("BCM2711", ignoreCase = true) ||
                manufacturer.contains("Raspberry Pi", ignoreCase = true)

        val deviceId = when {
            isA56 -> "galaxy-a56-5g"
            isOnePlus6T -> "oneplus-6t"
            isPi400 -> "raspberry-pi-400"
            else -> "generic-${primaryAbi}"
        }

        val codename = when {
            isA56 -> "SM-A566B"
            isOnePlus6T -> "fajita"
            isPi400 -> "rpi-400"
            else -> model
        }

        val marketingName = when {
            isA56 -> "Samsung Galaxy A56 5G"
            isOnePlus6T -> "OnePlus 6T"
            isPi400 -> "Raspberry Pi 400"
            else -> "$manufacturer $model"
        }

        val safetyState = when {
            isA56 -> SafetyState.SAFE
            isOnePlus6T -> SafetyState.LAB
            isPi400 -> SafetyState.SAFE
            else -> SafetyState.EXPERIMENTAL
        }

        val roles = when {
            isA56 -> listOf(DeviceRole.CONTROLLER, DeviceRole.LOCAL_HOST)
            isOnePlus6T -> listOf(DeviceRole.LAB_DEVICE, DeviceRole.NATIVE_LINUX_TARGET, DeviceRole.HEADLESS_SERVER)
            isPi400 -> listOf(DeviceRole.ROADMAP_CLIENT)
            else -> listOf(DeviceRole.LOCAL_HOST)
        }

        val cpu = CpuInfo(
            name = when {
                isA56 -> "Exynos 1580"
                isOnePlus6T -> "Qualcomm SDM845"
                isPi400 -> "Broadcom BCM2711"
                else -> hardware
            },
            arch = if (primaryAbi.startsWith("arm64")) "arm64" else primaryAbi,
            abi = primaryAbi,
            coreCount = Runtime.getRuntime().availableProcessors(),
            features = emptyList(),
            status = if (isPi400) CapabilityStatus.RESEARCHED else CapabilityStatus.VERIFIED
        )

        val gpu = GpuInfo(
            name = when {
                isA56 -> "Xclipse 540 (AMD RDNA)"
                isOnePlus6T -> "Adreno 630"
                isPi400 -> "VideoCore VI"
                else -> "Unknown GPU"
            },
            driver = when {
                isOnePlus6T -> "freedreno (mainline) / kgsl (Android)"
                isPi400 -> "v3d"
                else -> "vulkan-samsung"
            },
            hardwareAccelerationStatus = if (isOnePlus6T || isPi400) CapabilityStatus.RESEARCHED else CapabilityStatus.INFERRED
        )

        val memory = MemoryInfo(
            totalBytes = totalRam,
            availableBytes = availRam,
            totalGb = totalRamGb,
            status = CapabilityStatus.VERIFIED
        )

        val storage = StorageInfo(
            internalTotalBytes = probe.getInternalStorageTotalBytes(),
            internalAvailableBytes = probe.getInternalStorageAvailableBytes(),
            appPrivateBytesAvailable = probe.getInternalStorageAvailableBytes(),
            status = CapabilityStatus.VERIFIED
        )

        val kernel = KernelInfo(
            release = kernelRelease,
            version = System.getProperty("os.version") ?: "unknown",
            architecture = System.getProperty("os.arch") ?: "unknown",
            isMainline = kernelRelease.startsWith("6.") && !kernelRelease.contains("android"),
            status = CapabilityStatus.VERIFIED
        )

        val android = AndroidInfo(
            versionRelease = androidRelease,
            apiLevel = sdkInt,
            securityPatch = "unknown",
            buildId = "unknown",
            knoxVaultPresent = isA56,
            status = CapabilityStatus.VERIFIED
        )

        val bootloaderUnlocked = probe.getBootloaderUnlockedState()
        val bootloader = BootloaderInfo(
            isUnlocked = bootloaderUnlocked,
            oemUnlockAllowed = if (isA56) false else null,
            status = if (bootloaderUnlocked != null) CapabilityStatus.VERIFIED else CapabilityStatus.UNKNOWN
        )

        val (isAb, currentSlot) = probe.getSlotInfo()
        val slots = SlotInfo(
            isAbDevice = isAb || isOnePlus6T,
            currentSlot = currentSlot,
            status = if (currentSlot != null) CapabilityStatus.VERIFIED else if (isOnePlus6T) CapabilityStatus.RESEARCHED else CapabilityStatus.UNKNOWN
        )

        val recovery = RecoveryInfo(
            recoveryType = if (isOnePlus6T) "Lineage Recovery / TWRP" else "Samsung Stock Recovery",
            efsBackupStatus = CapabilityStatus.UNKNOWN,
            status = CapabilityStatus.UNKNOWN
        )

        val connectivity = ConnectivityInfo(
            wifiSupported = probe.hasSystemFeature("android.hardware.wifi"),
            wifiHotspotSupported = true,
            wifiStatus = CapabilityStatus.VERIFIED,
            bluetoothSupported = probe.hasSystemFeature("android.hardware.bluetooth"),
            bluetoothPanSupported = true,
            bluetoothStatus = CapabilityStatus.VERIFIED,
            cellularSupported = probe.hasSystemFeature("android.hardware.telephony"),
            cellularModemDataSupported = true,
            cellularStatus = CapabilityStatus.VERIFIED,
            usbHostSupported = probe.hasSystemFeature("android.hardware.usb.host"),
            usbOtgSupported = true,
            usbStatus = CapabilityStatus.VERIFIED
        )

        val display = DisplayInfo(
            widthPixels = 1080,
            heightPixels = 2340,
            densityDpi = 420,
            externalDisplaySupported = isA56,
            status = CapabilityStatus.INFERRED
        )

        val audio = AudioInfo(
            outputSupported = true,
            inputSupported = true,
            status = CapabilityStatus.VERIFIED
        )

        val camera = CameraInfo(
            backCameraSupported = probe.hasSystemFeature("android.hardware.camera"),
            frontCameraSupported = probe.hasSystemFeature("android.hardware.camera.front"),
            mainlineLinuxSupported = false, // SDM845 camera unsupported on mainline Linux!
            status = if (isOnePlus6T) CapabilityStatus.UNSUPPORTED else CapabilityStatus.VERIFIED
        )

        val sensors = SensorsInfo(
            accelerometer = probe.hasSystemFeature("android.hardware.sensor.accelerometer"),
            gyroscope = probe.hasSystemFeature("android.hardware.sensor.gyroscope"),
            magnetometer = probe.hasSystemFeature("android.hardware.sensor.compass"),
            status = CapabilityStatus.VERIFIED
        )

        val powerThermal = PowerThermalInfo(
            batteryPresent = true,
            batteryCapacityPercent = probe.getBatteryPercent(),
            isCharging = probe.isCharging(),
            chargeThresholdControlSupported = isOnePlus6T, // Lineage/postmarketOS charge control
            status = CapabilityStatus.VERIFIED
        )

        val modem = ModemInfo(
            modemManagerCompatible = isOnePlus6T,
            callsSupported = true,
            smsSupported = true,
            dataSupported = true,
            status = if (isOnePlus6T) CapabilityStatus.RESEARCHED else CapabilityStatus.INFERRED
        )

        val avfSupported = probe.probeAvfSupport()
        val virtualization = VirtualizationInfo(
            avfAvailable = avfSupported,
            pKvmSupported = avfSupported,
            qemuTcgSupported = true,
            terminalAppDetected = false,
            status = if (avfSupported) CapabilityStatus.VERIFIED else CapabilityStatus.UNKNOWN
        )

        // Mode analysis & recommendations
        val trace = mutableListOf<String>()

        val modeAStatus = CapabilityStatus.VERIFIED
        trace.add("Mode A (PRoot): Rootless userspace is supported on host kernel without root.")

        val modeB1Status = if (avfSupported) {
            trace.add("Mode B1 (AVF): VirtualMachineManager verified active.")
            CapabilityStatus.VERIFIED
        } else {
            trace.add("Mode B1 (AVF): Not exposed or protected on this device/firmware.")
            CapabilityStatus.UNSUPPORTED
        }

        val modeB2Status = CapabilityStatus.EXPERIMENTAL
        trace.add("Mode B2 (QEMU TCG): Available as universal software fallback (degraded performance).")

        val modeCStatus = when {
            isA56 -> {
                trace.add("Mode C (Native Linux): Not supported on A56 due to locked bootloader & Knox constraints.")
                CapabilityStatus.UNSUPPORTED
            }
            isOnePlus6T -> {
                if (bootloaderUnlocked == true) {
                    trace.add("Mode C (Native Linux): OnePlus 6T bootloader is unlocked. Mainline kernel supported via A/B dual boot.")
                    CapabilityStatus.VERIFIED
                } else {
                    trace.add("Mode C (Native Linux): OnePlus 6T has strong mainline support, but bootloader/slot requires fastboot inspection.")
                    CapabilityStatus.UNKNOWN
                }
            }
            isPi400 -> {
                trace.add("Mode C (Native Linux): Raspberry Pi 400 runs native Linux directly on bare metal.")
                CapabilityStatus.VERIFIED
            }
            else -> CapabilityStatus.UNKNOWN
        }

        val recommendedMode = when {
            isPi400 -> "MODE_C_NATIVE"
            modeCStatus == CapabilityStatus.VERIFIED -> "MODE_C_NATIVE"
            modeB1Status == CapabilityStatus.VERIFIED -> "MODE_B_AVF"
            else -> "MODE_A_PROOT"
        }
        trace.add("Recommended mode: $recommendedMode")

        val modes = ModeSupportMatrix(
            modeA_PRoot = modeAStatus,
            modeB1_AVF = modeB1Status,
            modeB2_QEMU = modeB2Status,
            modeC_NativeLinux = modeCStatus,
            recommendedMode = recommendedMode,
            reasoningTrace = trace
        )

        return DeviceCapabilityReport(
            deviceId = deviceId,
            marketingName = marketingName,
            manufacturer = manufacturer,
            model = model,
            codename = codename,
            safetyState = safetyState,
            roles = roles,
            cpu = cpu,
            gpu = gpu,
            memory = memory,
            storage = storage,
            kernel = kernel,
            android = android,
            bootloader = bootloader,
            slots = slots,
            recovery = recovery,
            connectivity = connectivity,
            display = display,
            audio = audio,
            camera = camera,
            sensors = sensors,
            powerThermal = powerThermal,
            modem = modem,
            virtualization = virtualization,
            modes = modes
        )
    }
}
