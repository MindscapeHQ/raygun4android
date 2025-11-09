package com.raygun.raygun4android.utils

object RaygunUtils {
    /** @return copy of the two merged lists */
    fun mergeLists(
        firstList: List<*>?,
        secondList: List<*>?,
    ): List<*> {
        if (firstList == null && secondList == null) {
            return emptyList<Any?>()
        }

        // If only the first list has contents return that.
        if (firstList != null && secondList == null) {
            return firstList.toList()
        }

        // If only the second list has contents return that.
        if (firstList == null && secondList != null) {
            return secondList.toList()
        }

        // Concatenate the two lists
        return firstList!! + secondList!!
    }

    /** @return copy of the two merged maps */
    fun mergeMaps(
        firstMap: Map<*, *>?,
        secondMap: Map<*, *>?,
    ): Map<*, *> {
        if (firstMap == null && secondMap == null) {
            return emptyMap<Any?, Any?>()
        }

        // If only the first map has contents return that.
        if (firstMap != null && secondMap == null) {
            return firstMap.toMap()
        }

        // If only the second map has contents return that.
        if (firstMap == null && secondMap != null) {
            return secondMap.toMap()
        }

        // Merge the two maps, second map takes preference if there are duplicate keys.
        return firstMap!! + secondMap!!
    }
}
