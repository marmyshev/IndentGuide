package net.certiv.tools.indentguide.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Utils {

	private Utils() {}

	/**
	 * Convert the given array to an {@code ArrayList} of the same type.
	 * 
	 * @param array the source array to convert
	 * @return a list containing the array elements
	 */
	public static <T> List<T> toList(T[] array) {
		return Arrays.stream(array).collect(Collectors.toList());
	}

	/**
	 * Subtracts from set A all elements in common with set B.
	 * <p>
	 * [0,1,2], [1,2,3] -> [0]
	 */
	public static Set<String> subtract(Set<String> a, Set<String> b) {
		Set<String> res = new LinkedHashSet<>(a);
		res.removeAll(b);
		return res;
	}

	/**
	 * Returns the disjoint set of A and B.
	 * <p>
	 * [0,1,2], [1,2,3] -> [0,3]
	 */
	public static Set<String> disjoint(Set<String> a, Set<String> b) {
		Set<String> res = new LinkedHashSet<>();
		res.addAll(subtract(a, b));
		res.addAll(subtract(b, a));
		return res;
	}
}
