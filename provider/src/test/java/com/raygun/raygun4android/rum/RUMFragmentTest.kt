package com.raygun.raygun4android.rum

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.raygun.raygun4android.RaygunRUMEventType
import com.raygun.raygun4android.TestTree
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import timber.log.Timber

@RunWith(MockitoJUnitRunner::class)
class RUMFragmentTest {
    @Before
    fun setup() {
        Timber.plant(TestTree())
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    private lateinit var mockFragmentManager: FragmentManager
    private lateinit var mockRUM: RUM

    @Test
    fun fragmentLifecycle() {
        val mockFragmentManager = mock<FragmentManager>(FragmentManager::class.java)
        val mockRUM = mock<RUM>(RUM::class.java)
        val rumFragment = RUMFragment(mockRUM)
        val fragment = Fragment()
        rumFragment.onFragmentCreated(mockFragmentManager, fragment, null)
        rumFragment.onFragmentResumed(mockFragmentManager, fragment)
        verify(mockRUM).sendRUMTimingEvent(RaygunRUMEventType.FRAGMENT_LOADED, "Fragment", anyLong())
    }
}