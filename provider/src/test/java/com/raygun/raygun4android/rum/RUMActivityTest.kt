package com.raygun.raygun4android.rum

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.raygun.raygun4android.RaygunRUMEventType
import com.raygun.raygun4android.TestTree
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import timber.log.Timber

class RUMActivityTest {
    @Before
    fun setup() {
        Timber.plant(TestTree())
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun activityLifecycle() {
        val mockActivity = mock<AppCompatActivity>(AppCompatActivity::class.java)
        val mockFragmentManager = mock<FragmentManager>(FragmentManager::class.java)
        whenever(mockActivity.supportFragmentManager).thenReturn(mockFragmentManager)
        val mockRUM = mock<RUM>(RUM::class.java)
        val rumFragment = RUMFragment(mockRUM)
        val rumActivity = RUMActivity(mockRUM, rumFragment)

        // Simulate lifecycle from Created to Destroyed
        rumActivity.onActivityCreated(mockActivity, null)
        rumActivity.onActivityStarted(mockActivity)
        rumActivity.onActivityResumed(mockActivity)
        rumActivity.onActivityPaused(mockActivity)
        rumActivity.onActivityStopped(mockActivity)
        rumActivity.onActivityDestroyed(mockActivity)

        // onActivityResumed should have called to sendRUMTimingEvent
        verify(mockRUM).sendRUMTimingEvent(
            eq(RaygunRUMEventType.ACTIVITY_LOADED),
            eq("AppCompatActivity"),
            anyLong()
        )

        // seen should be called for each "onActivity..."
        verify(mockRUM, times(6)).seen()
    }
}
