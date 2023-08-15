package com.github.d.romanov.spotify.importer.utils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ListUtils {

    public static <T> List<List<T>> splitList(List<T> list, int size) {

        if (size < list.size()) {
            int start = 0;
            List<List<T>> result = new ArrayList<>();
            while (start < list.size()) {
                int toIndex = Math.min(start + size, list.size());
                result.add(list.subList(start, toIndex));
                start += size;
            }
            return result;
        }

        return List.of(list);
    }
}
