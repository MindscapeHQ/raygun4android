package com.raygun.raygun4android

import android.content.Context
import com.raygun.raygun4android.utils.RaygunFileFilter
import com.raygun.raygun4android.utils.RaygunFileUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class RaygunFileUtilsTest {
    @Test
    fun getExtensionReturnsCorrectExtension() {
        val filename = "testfile.txt"
        val expectedExtension = "txt"
        val resultExtension = RaygunFileUtils.getExtension(filename)

        assertEquals(expectedExtension, resultExtension)
    }

    @Test
    fun clearCachedReportsDeletesNothing() {
        val mockContext = mock<Context>()
        val mockCacheDir = mock<File>()

        val mockFile1 = mock<File>()
        whenever(mockFile1.name).thenReturn("file1.txt")

        val mockFile2 = mock<File>()
        whenever(mockFile2.name).thenReturn("file2.txt")

        val mockFile3 = mock<File>()
        whenever(mockFile3.name).thenReturn("file3.txt")

        val mockFiles = arrayOf(mockFile1, mockFile2, mockFile3)

        whenever(mockContext.cacheDir).thenReturn(mockCacheDir)
        whenever(mockCacheDir.listFiles(anyOrNull<RaygunFileFilter>())).thenReturn(mockFiles)

        RaygunFileUtils.clearCachedReports(mockContext)

        verify(mockFiles[0], times(0)).delete()
        verify(mockFiles[1], times(0)).delete()
        verify(mockFiles[2], times(0)).delete()
    }

    @Test
    fun clearCachedReportsDeletesRaygunReports() {
        val mockContext = mock<Context>()
        val mockCacheDir = mock<File>()

        val mockFile1 = mock<File>()
        whenever(mockFile1.name).thenReturn("file1.raygun4")

        val mockFile2 = mock<File>()
        whenever(mockFile2.name).thenReturn("file2.txt")

        val mockFile3 = mock<File>()
        whenever(mockFile3.name).thenReturn("file3.raygun4")

        val mockFiles = arrayOf(mockFile1, mockFile2, mockFile3)

        whenever(mockContext.cacheDir).thenReturn(mockCacheDir)
        whenever(mockCacheDir.listFiles(anyOrNull<RaygunFileFilter>())).thenReturn(mockFiles)

        RaygunFileUtils.clearCachedReports(mockContext)

        verify(mockFiles[0], times(1)).delete()
        verify(mockFiles[1], times(0)).delete()
        verify(mockFiles[2], times(1)).delete()
    }
}
