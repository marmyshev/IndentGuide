package net.certiv.tools.indentguide.util;

import java.lang.reflect.Method;
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
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.themes.ColorUtil;

import net.certiv.tools.indentguide.preferences.Pref;

public class Utils {

	public static final char SPC = ' '; // $NON-NLS-1$
	public static final char TAB = '\t'; // $NON-NLS-1$
	public static final char NLC = '\n'; // $NON-NLS-1$
	public static final char RET = '\r'; // $NON-NLS-1$
	public static final String EOL = System.lineSeparator();
	public static final String EMPTY = ""; // $NON-NLS-1$
	public static final String SPACE = " "; // $NON-NLS-1$
	public static final String DELIM = "|"; // $NON-NLS-1$

	public static final char SP_THIN = '\u2009';	//
	public static final char SP_MARK = '\u00b7';	// ·
	public static final char TAB_MARK = '\u00bb';	// »
	public static final char RET_MARK = '\u00a4';	// ¤
	public static final char NL_MARK = '\u00b6';	// ¶

	private static final String EditorsID = "org.eclipse.ui.editors"; //$NON-NLS-1$
	private static final IEclipsePreferences[] Scopes = new IEclipsePreferences[] {
			InstanceScope.INSTANCE.getNode(EditorsID), //
			DefaultScope.INSTANCE.getNode(EditorsID) //
	};

	public static final Class<?>[] NoParams = null;
	public static final Object[] NoArgs = null;

	private static Set<IContentType> platformTextTypes;

	private Utils() {}

	public static <T> T invoke(Object target, String methodName) throws Throwable {
		return invoke(target, methodName, NoParams, NoArgs);
	}

	@SuppressWarnings("unchecked")
	public static <T> T invoke(Object target, String methodName, Class<?>[] params, Object[] args) throws Throwable {
		Method m = target.getClass().getMethod(methodName, params);
		m.setAccessible(true);
		return (T) m.invoke(target, args);
	}

	/** Restrict the range of the given val to between -1 and 1. */
	public static int limit(int val) {
		return (val > 1) ? 1 : (val < -1 ? -1 : val);
	}

	/** Encodes WS as discrete visible characters. */
	public static String encode(String in) {
		if (in == null) return EMPTY;

		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < in.length(); idx++) {
			char ch = in.charAt(idx);
			switch (ch) {
				case SPC:
					sb.append(SP_MARK);
					break;
				case TAB:
					sb.append(TAB_MARK);
					// sb.append(SP_THIN);
					break;
				case RET:
					sb.append(RET_MARK);
					break;
				case NLC:
					sb.append(NL_MARK);
					break;
				default:
					sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Convert a document offset to the corresponding widget offset.
	 *
	 * @param offset the document offset
	 * @return widget offset
	 */
	public static int widgetOffset(ITextViewer viewer, int offset) {
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 view = (ITextViewerExtension5) viewer;
			return view.modelOffset2WidgetOffset(offset);
		}

		IRegion visible = viewer.getVisibleRegion();
		int widgetOffset = offset - visible.getOffset();
		if (widgetOffset > visible.getLength()) return -1;
		return widgetOffset;
	}

	/**
	 * Convert a widget offset to the corresponding document offset.
	 *
	 * @param offset the widget offset
	 * @return document offset
	 */
	public static int docOffset(ITextViewer viewer, int offset) {
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 view = (ITextViewerExtension5) viewer;
			return view.widgetOffset2ModelOffset(offset);
		}

		IRegion visible = viewer.getVisibleRegion();
		if (offset > visible.getLength()) return -1;
		return offset + visible.getOffset();
	}

	/**
	 * Check if the given widget line is a folded line.
	 *
	 * @param viewer the viewer containing the widget
	 * @param line   the widget line number
	 * @return {@code true} if the line is folded
	 */
	public static boolean isFolded(ITextViewer viewer, int line) {
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 ext = (ITextViewerExtension5) viewer;
			int modelLine = ext.widgetLine2ModelLine(line);
			int widgetLine = ext.modelLine2WidgetLine(modelLine + 1);
			return widgetLine == -1;
		}
		return false;
	}

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
		if (platformTextTypes == null) {
			IContentTypeManager mgr = Platform.getContentTypeManager();
			IContentType txtType = mgr.getContentType(IContentTypeManager.CT_TEXT);
			platformTextTypes = Stream.of(mgr.getAllContentTypes()).filter(t -> t.isKindOf(txtType))
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		return platformTextTypes;
	}

	/**
	 * Convert a set of content types to a delimited string suitable for preference storage.
	 *
	 * @param terms set containing the individual terms
	 * @return a delimited string of preference terms
	 */
	public static String delimitTypes(Set<IContentType> types) {
		if (types.isEmpty()) return EMPTY;
		Set<String> terms = types.stream().map(IContentType::getId).collect(Collectors.toSet());
		return delimit(terms);
	}

	/**
	 * Convert a set of terms to a delimited string.
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
	 * Returns the delta between an initial set A and later set B.
	 * <p>
	 * [0,1,2], [1,2,3] -> added [3] & removed [0]
	 */
	public static <T> Delta<T> delta(Set<T> before, Set<T> after) {
		return new Delta<>(subtract(after, before), subtract(before, after));
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

	/**
	 * Sets the widget to always use the operating system's advanced graphics subsystem for all
	 * graphics operations.
	 *
	 * @param widget
	 * @return
	 */
	public static boolean setAdvanced(StyledText widget) {
		GC gc = new GC(widget);
		gc.setAdvanced(true);
		boolean advanced = gc.getAdvanced();
		gc.dispose();
		return advanced;
	}

	public static Color getColor(IPreferenceStore store) {
		String key = Pref.LINE_COLOR;
		if (isDarkTheme()) {
			key += Pref.DARK;
		}
		String raw = store.getString(key);
		return new Color(PlatformUI.getWorkbench().getDisplay(), ColorUtil.getColorValue(raw));
	}

	/**
	 * Returns {@code true} if the current platform theme is 'dark'; empirically defined where the
	 * editor foreground color is relatively darker than the background color.
	 * <p>
	 * black -> '0'; white -> '255*3'
	 */
	public static boolean isDarkTheme() {
		RGB fg = getRawRGB(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
		RGB bg = getRawRGB(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
		return (fg.red + fg.blue + fg.green) > (bg.red + bg.blue + bg.green);
	}

	private static RGB getRawRGB(String key) {
		String value = Platform.getPreferencesService().get(key, null, Scopes);
		if (value == null) return PreferenceConverter.COLOR_DEFAULT_DEFAULT;
		return StringConverter.asRGB(value, PreferenceConverter.COLOR_DEFAULT_DEFAULT);
	}
}
