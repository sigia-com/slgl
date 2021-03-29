package io.slgl.api.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CollectionUtils {

	private CollectionUtils() {
	}

	public static <T> List<T> nullToEmptyList(List<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}

		return list;
	}

	public static <T> Collection<T> nullToEmptyCollection(Collection<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}

		return list;
	}

	public static <K, V> Map<K, V> nullToEmptyMap(Map<K, V> map) {
		if (map == null) {
			return Collections.emptyMap();
		}

		return map;
	}
}
