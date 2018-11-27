package com.raygun.raygun4android.utils;

import java.util.*;

public class RaygunUtils {

    public static List mergeLists(List firstList, List secondList) {

        if (firstList == null && secondList == null) {
            return new ArrayList();
        }

        // If only the first list has contents return that.
        if (firstList != null && secondList == null) {
            return firstList;
        }

        // If only the second list has contents return that.
        if (firstList == null && secondList != null) {
            return secondList;
        }

        List merged = new ArrayList(firstList);
        merged.addAll(secondList);

        return merged;

    }

    public static Map mergeMaps(Map firstMap, Map secondMap) {

        if (firstMap == null && secondMap == null) {
            return new HashMap();
        }

        // If only the first map has contents return that.
        if (firstMap != null && secondMap == null) {
            return firstMap;
        }

        // If only the second map has contents return that.
        if (firstMap == null && secondMap != null) {
            return secondMap;
        }

        Map merged = new HashMap(firstMap);
        merged.putAll(secondMap);

        return merged;

    }
}