package com.raygun.raygun4android

import com.raygun.raygun4android.utils.RaygunUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class RaygunUtilsTest {
    @Test
    fun mergeListsBothNull() {
        val result = RaygunUtils.mergeLists(null, null)
        assertEquals(0, result.size)
    }

    @Test
    fun mergeListsFirstNull() {
        val secondList = listOf("test1", "test2")
        val result = RaygunUtils.mergeLists(null, secondList)
        assertEquals(secondList, result)
    }

    @Test
    fun mergeListsSecondNull() {
        val firstList = listOf("test1", "test2")
        val result = RaygunUtils.mergeLists(firstList, null)
        assertEquals(firstList, result)
    }

    @Test
    fun mergeListsBothNotNull() {
        val firstList = listOf("test1", "test2")
        val secondList = listOf("test3", "test4")
        val expected = listOf("test1", "test2", "test3", "test4")
        val result = RaygunUtils.mergeLists(firstList, secondList)
        assertEquals(expected, result)
    }

    @Test
    fun mergeMapsBothNull() {
        val result = RaygunUtils.mergeMaps(null, null)
        assertEquals(0, result.size)
    }

    @Test
    fun mergeMapsFirstNull() {
        val secondMap = mapOf("key1" to "value1")
        val result = RaygunUtils.mergeMaps(null, secondMap)
        assertEquals(secondMap, result)
    }

    @Test
    fun mergeMapsSecondNull() {
        val firstMap = mapOf("key1" to "value1")
        val result = RaygunUtils.mergeMaps(firstMap, null)
        assertEquals(firstMap, result)
    }

    @Test
    fun mergeMapsBothNotNull() {
        val firstMap = mapOf("key1" to "value1")
        val secondMap = mapOf("key2" to "value2")
        val expected = mapOf("key1" to "value1", "key2" to "value2")
        val result = RaygunUtils.mergeMaps(firstMap, secondMap)
        assertEquals(expected, result)
    }
}
