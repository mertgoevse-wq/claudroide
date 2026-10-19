package org.claudroide.app.feature.linux.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.system.Os
import java.io.File

class AndroidProbeSource(private val context: Context) : ProbeSource {

    override fun getManufacturer(): String = Build.MANUFACTURER

    override fun getModel(): String = Build.MODEL

    override fun getHardware(): String = Build.HARDWARE

    override fun getSupportedAbis(): List<String> = Build.SUPPORTED_ABIS.toList()

    override fun getAndroidRelease(): String = Build.VERSION.RELEASE

    override fun getSdkInt(): Int = Build.VERSION.SDK_INT

    override fun getKernelRelease(): String {
        return try {
            Os.uname().release
        } catch (e: Throwable) {
            System.getProperty("os.version") ?: "unknown"
        }
    }

    override fun getTotalRamBytes(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    override fun getAvailableRamBytes(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        return memInfo.availMem
    }

    override fun getInternalStorageTotalBytes(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.totalBytes
        } catch (e: Throwable) {
            0L
        }
    }

    override fun getInternalStorageAvailableBytes(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBytes
        } catch (e: Throwable) {
            0L
        }
    }

    override fun hasSystemFeature(featureName: String): Boolean {
        return context.packageManager.hasSystemFeature(featureName)
    }

    override fun probeAvfSupport(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        return try {
            Class.forName("android.system.virtualmachine.VirtualMachineManager")
            val vmm = context.getSystemService("virtualmachine")
            vmm != null
        } catch (e: Throwable) {
            false
        }
    }

    override fun getBootloaderUnlockedState(): Boolean? {
        // Rootless check: check system property ro.boot.flash.locked or ro.boot.verifiedbootstate
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
            val locked = getMethod.invoke(null, "ro.boot.flash.locked", "") as String
            if (locked == "0") true
            else if (locked == "1") false
            else {
                val bootState = getMethod.invoke(null, "ro.boot.verifiedbootstate", "") as String
                if (bootState == "orange") true
                else if (bootState == "green") false
                else null
            }
        } catch (e: Throwable) {
            null // Cannot be determined rootlessly without adb/fastboot
        }
    }

    override fun getSlotInfo(): Pair<Boolean, String?> {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
            val slotSuffix = getMethod.invoke(null, "ro.boot.slot_suffix", "") as String
            if (slotSuffix.isNotEmpty()) {
                Pair(true, slotSuffix)
            } else {
                Pair(false, null)
            }
        } catch (e: Throwable) {
            Pair(false, null)
        }
    }

    override fun getBatteryPercent(): Int? {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level * 100 / scale.toFloat()).toInt()
            } else null
        } catch (e: Throwable) {
            null
        }
    }

    override fun isCharging(): Boolean? {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Throwable) {
            null
        }
    }
}
