package com.raygun.raygun4android.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Locks in the mapping behaviour of [TelephonyUtils.networkTypeToString].
 *
 * Regression guard for the v6.0.x cleanup that suppresses the deprecated CDMA-era NETWORK_TYPE_*
 * constants. The output strings for those constants must remain stable so that crash report
 * payloads render identically.
 */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class TelephonyUtilsTest {
    @Test
    fun deniedPhoneStatePermissionReturnsUnknown() {
        val context = mock<Context>()
        whenever(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        assertEquals("Unknown", TelephonyUtils.getNetworkType(context))
        verify(context).checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
        verifyNoMoreInteractions(context)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun grantedPhoneStatePermissionReturnsLegacyNetworkTypeOnApi23() {
        val context = mock<Context>()
        val telephonyManager = mock<TelephonyManager>()
        whenever(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager)
        whenever(telephonyManager.networkType).thenReturn(TelephonyManager.NETWORK_TYPE_LTE)

        assertEquals("LTE", TelephonyUtils.getNetworkType(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun grantedPhoneStatePermissionReturnsDataNetworkTypeOnApi24() {
        val context = mock<Context>()
        val telephonyManager = mock<TelephonyManager>()
        whenever(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager)
        whenever(telephonyManager.dataNetworkType).thenReturn(TelephonyManager.NETWORK_TYPE_LTE)

        assertEquals("LTE", TelephonyUtils.getNetworkType(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun unavailableTelephonyServiceReturnsUnknown() {
        val context = mock<Context>()
        whenever(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(null)

        assertEquals("Unknown", TelephonyUtils.getNetworkType(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun securityExceptionReturnsUnknown() {
        val context = mock<Context>()
        whenever(context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.TELEPHONY_SERVICE)).thenThrow(SecurityException())

        assertEquals("Unknown", TelephonyUtils.getNetworkType(context))
    }

    @Test
    fun mapsCurrentNetworkTypes() {
        assertEquals("EDGE", TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_EDGE))
        assertEquals("GPRS", TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_GPRS))
        assertEquals(
            "HSDPA",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_HSDPA),
        )
        assertEquals("HSPA", TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_HSPA))
        assertEquals(
            "HSPA+",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_HSPAP),
        )
        assertEquals(
            "HSUPA",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_HSUPA),
        )
        assertEquals("LTE", TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_LTE))
        assertEquals("UMTS", TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_UMTS))
    }

    @Test
    fun mapsDeprecatedCdmaEraNetworkTypes() {
        // These constants are deprecated since API 30 but still emitted by older
        // devices via TelephonyManager.getNetworkType(); the mapping is preserved
        // intentionally so payload output does not regress.
        assertEquals(
            "1xRTT",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_1xRTT),
        )
        assertEquals("CDMA", TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_CDMA))
        assertEquals(
            "eHRPD",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_EHRPD),
        )
        assertEquals(
            "EVDO rev. 0",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_EVDO_0),
        )
        assertEquals(
            "EVDO rev. A",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_EVDO_A),
        )
        assertEquals(
            "EVDO rev. B",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_EVDO_B),
        )
    }

    @Test
    fun unknownNetworkTypeReturnsUnknown() {
        assertEquals("Unknown", TelephonyUtils.networkTypeToString(0))
        assertEquals("Unknown", TelephonyUtils.networkTypeToString(-1))
        assertEquals("Unknown", TelephonyUtils.networkTypeToString(99_999))
        // NETWORK_TYPE_UNKNOWN itself
        assertEquals(
            "Unknown",
            TelephonyUtils.networkTypeToString(TelephonyManager.NETWORK_TYPE_UNKNOWN),
        )
    }
}
