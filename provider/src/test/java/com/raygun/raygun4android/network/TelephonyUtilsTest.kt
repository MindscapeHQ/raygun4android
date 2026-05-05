package com.raygun.raygun4android.network

import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in the mapping behaviour of [TelephonyUtils.networkTypeToString].
 *
 * Regression guard for the v6.0.x cleanup that suppresses the deprecated CDMA-era NETWORK_TYPE_*
 * constants. The output strings for those constants must remain stable so that crash report
 * payloads render identically.
 */
@Suppress("DEPRECATION")
class TelephonyUtilsTest {
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
