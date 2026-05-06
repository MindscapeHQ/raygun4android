package com.raygun.raygun4android.network

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ConnectivityUtils].
 *
 * The full Android ConnectivityManager / NetworkCapabilities surface is not available in plain JVM
 * unit tests; these tests therefore exercise the code path where ConnectivityManager itself is
 * absent (which mirrors what happens inside a unit test and during early app start).
 *
 * Locks in current behaviour after the legacy pre-API-23 paths are removed (minSdk has been 23
 * since v5.1.0), so the cleanup cannot regress these defaults.
 */
class ConnectivityUtilsTest {
    @Test
    fun isNetworkAvailableReturnsFalseWhenConnectivityManagerUnavailable() {
        val context = mock<Context>()
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null)

        assertFalse(ConnectivityUtils.isNetworkAvailable(context))
    }

    @Test
    fun networkConnectivityStateReturnsNotConnectedWhenUnavailable() {
        val context = mock<Context>()
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null)

        assertEquals("Not connected", ConnectivityUtils.networkConnectivityState(context))
    }
}
