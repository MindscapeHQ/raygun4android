package com.raygun.raygun4android.messages.crashreporting

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import com.raygun.raygun4android.logging.RaygunLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@SuppressLint("SwitchIntDef")
class RaygunEnvironmentMessage private constructor() {
    private val availableVirtualMemory: Long = 0
    private val cpu: String? = null
    private val totalVirtualMemory: Long = 0
    private var architecture: String? = null
    private var availablePhysicalMemory: Long = 0
    private var board: String? = null
    private var brand: String? = null
    private var currentOrientation: String? = null
    private var deviceCode: String? = null
    private var deviceName: String? = null
    private var diskSpaceFree: Long = 0
    private var locale: String? = null
    private var oSVersion: String? = null
    private var osSDKVersion: String? = null
    private var processorCount = 0
    private var totalPhysicalMemory: Long = 0
    private var utcOffset = 0.0
    private var windowsBoundHeight = 0
    private var windowsBoundWidth = 0

    companion object {
        @Throws(IOException::class)
        suspend fun getTotalRam(): String? =
            withContext(Dispatchers.IO) {
                var reader: RandomAccessFile? = null
                try {
                    reader = RandomAccessFile("/proc/meminfo", "r")
                    return@withContext reader.readLine()
                } finally {
                    reader?.close()
                }
            }

        suspend operator fun invoke(context: Context): RaygunEnvironmentMessage {
            val raygunEnvironmentMessage = RaygunEnvironmentMessage()
            try {
                raygunEnvironmentMessage.architecture = Build.CPU_ABI
                raygunEnvironmentMessage.oSVersion = Build.VERSION.RELEASE
                raygunEnvironmentMessage.osSDKVersion = Build.VERSION.SDK_INT.toString()
                raygunEnvironmentMessage.deviceName = Build.MODEL
                raygunEnvironmentMessage.deviceCode = Build.DEVICE
                raygunEnvironmentMessage.brand = Build.BRAND
                raygunEnvironmentMessage.board = Build.BOARD

                raygunEnvironmentMessage.processorCount = Runtime.getRuntime().availableProcessors()

                val orientation = context.resources.configuration.orientation
                raygunEnvironmentMessage.currentOrientation =
                    when (orientation) {
                        1 -> "Portrait"
                        2 -> "Landscape"
                        3 -> "Square"
                        else -> "Undefined"
                    }

                val metrics = DisplayMetrics()
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay
                    .getMetrics(metrics)
                raygunEnvironmentMessage.windowsBoundWidth = metrics.widthPixels
                raygunEnvironmentMessage.windowsBoundHeight = metrics.heightPixels

                val tz = TimeZone.getDefault()
                val now = Date()
                raygunEnvironmentMessage.utcOffset =
                    (
                        TimeUnit.SECONDS.convert(
                            tz.getOffset(now.time).toLong(),
                            TimeUnit.MILLISECONDS,
                        ) / 3600
                    ).toDouble()
                raygunEnvironmentMessage.locale =
                    context.resources.configuration.locale
                        .toString()

                val mi = ActivityManager.MemoryInfo()
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.getMemoryInfo(mi)
                raygunEnvironmentMessage.availablePhysicalMemory = mi.availMem / 0x100000

                raygunEnvironmentMessage.totalPhysicalMemory =
                    getTotalRam()?.let {
                        val p = Pattern.compile("^\\D*(\\d*).*$")
                        val m = p.matcher(it)
                        m.find()
                        val match = m.group(1)
                        match!!.toLong() / 0x400
                    } ?: 0

                val stat =
                    withContext(Dispatchers.IO) { StatFs(Environment.getDataDirectory().path) }

                val availableBlocks = stat.availableBlocks.toLong()
                val blockSize = stat.blockSize.toLong()
                raygunEnvironmentMessage.diskSpaceFree = (availableBlocks * blockSize) / 0x100000
            } catch (e: Exception) {
                RaygunLogger.w("Couldn't get all env data: $e")
            }
            return raygunEnvironmentMessage
        }
    }
}
