package net.certiv.tools.indentguide.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.widgets.Control;

public class Utils {

	private static final String DELIM = "|"; // $NON-NLS-1$

	private static Set<IContentType> allTypes;

	private Utils() {}

	/**
	 * Returns the height in pixels of the given number of lines for the given control.
	 *
	 * @param comp  the component to examine
	 * @param lines number of lines to measure
	 * @return number of pixels vertical
	 */
	public static int lineHeight(Control comp, int lines) {
		PixelConverter pc = new PixelConverter(comp);
		return pc.convertVerticalDLUsToPixels(lines > 0 ? lines : 1);
	}

	/**
	 * Returns the text content type identifiers known to the platform at plugin startup.
	 *
	 * @return the known text content type identifiers
	 */
	public static Set<IContentType> platformTextTypes() {
		if (allTypes == null) {
			IContentTypeManager mgr = Platform.getContentTypeManager();
			IContentType txtType = mgr.getContentType(IContentTypeManager.CT_TEXT);
			allTypes = Stream.of(mgr.getAllContentTypes()).filter(t -> t.isKindOf(txtType))
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		return allTypes;
	}

	public static String delimitTypes(Set<IContentType> types) {
		if (types.isEmpty()) return "";
		Set<String> terms = types.stream().map(IContentType::getId).collect(Collectors.toSet());
		return delimit(terms);
	}

	/**
	 * Convert a set of preference terms to a delimited string.
	 *
	 * @param terms set containing the individual terms
	 * @return a delimited string of preference terms
	 */
	public static String delimit(Set<String> terms) {
		return String.join(DELIM, terms);
	}

	/**
	 * Convert a delimited string to a {@code LinkedHashSet}.
	 *
	 * @param delimited a delimited string of preference terms
	 * @return set containing the individual terms
	 */
	public static Set<String> undelimit(String delimited) {
		Set<String> types = new LinkedHashSet<>();
		StringTokenizer tokens = new StringTokenizer(delimited, DELIM);
		while (tokens.hasMoreTokens()) {
			types.add(tokens.nextToken());
		}
		return types;
	}

	/**
	 * Convert the given array to an {@code LinkedList} of the same type.
	 *
	 * @param array the source array to convert
	 * @return a list containing the array elements
	 */
	public static <T> List<T> toList(T[] array) {
		return Arrays.stream(array).collect(Collectors.toCollection(LinkedList::new));
	}

	/**
	 * Convert the given array to an {@code LinkedHashSet} of the same type.
	 *
	 * @param array the source array to convert
	 * @return a set containing the array elements
	 */
	public static <T> Set<T> toSet(T[] array) {
		return Arrays.stream(array).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Subtracts from set A all elements in common with those in set B.
	 * <p>
	 * [0,1,2], [1,2,3] -> [0]
	 */
	public static <T> Set<T> subtract(Set<T> a, Set<T> b) {
		Set<T> res = new LinkedHashSet<>(a);
		res.removeAll(b);
		return res;
	}

	/**
	 * Returns the disjoint set of A and B.
	 * <p>
	 * [0,1,2], [1,2,3] -> [0,3]
	 */
	public static <T> Set<T> disjoint(Set<T> a, Set<T> b) {
		Set<T> res = new LinkedHashSet<>();
		res.addAll(subtract(a, b));
		res.addAll(subtract(b, a));
		return res;
	}

	/**
	 * Returns the delta between an initial set A and final set B.
	 * <p>
	 * [0,1,2], [1,2,3] -> added [3] & removed [0]
	 */
	public static <T> Delta<T> delta(Set<T> a, Set<T> b) {
		return new Delta<>(subtract(b, a), subtract(a, b));
	}

	public static class Delta<T> {

		public Set<T> added;
		public Set<T> rmved;

		public Delta(Set<T> added, Set<T> rmved) {
			this.added = added;
			this.rmved = rmved;
		}

		/** Returns {@code true} if both added and removed sets are empty. */
		public boolean isEmpty() {
			return added.isEmpty() & rmved.isEmpty();
		}
	}
}
