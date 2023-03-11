package net.certiv.tools.indentguide.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Objects;
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

import net.certiv.tools.indentguide.adaptors.ContentTypeAdaptor;
import net.certiv.tools.indentguide.preferences.Pref;

public class Utils {

	public static final char SPC = ' '; // $NON-NLS-1$
	public static final char DOT = '.'; // $NON-NLS-1$
	public static final char TAB = '\t'; // $NON-NLS-1$
	public static final char NLC = '\n'; // $NON-NLS-1$
	public static final char RET = '\r'; // $NON-NLS-1$

	public static final String EMPTY = ""; // $NON-NLS-1$
	public static final String SPACE = " "; // $NON-NLS-1$
	public static final String DELIM = "|"; // $NON-NLS-1$
	public static final String EOL = System.lineSeparator();

	public static final char SP_THIN = '\u2009';	//
	public static final char SP_MARK = '\u00b7';	// ·
	public static final char TAB_MARK = '\u00bb';	// »
	public static final char RET_MARK = '\u00a4';	// ¤
	public static final char NL_MARK = '\u00b6';	// ¶

	public static final String UNKNOWN = "Unknown"; // $NON-NLS-1$
	public static final Class<?>[] NoParams = new Class<?>[0];
	public static final Object[] NoArgs = new Object[0];

	private static final String EditorsID = "org.eclipse.ui.editors"; //$NON-NLS-1$
	private static final IEclipsePreferences[] Scopes = new IEclipsePreferences[] {
			InstanceScope.INSTANCE.getNode(EditorsID), //
			DefaultScope.INSTANCE.getNode(EditorsID) //
	};

	private static Set<IContentType> platformTextTypes;
	private static IContentType txtType;

	private Utils() {}

	/** Returns the class name of the given object, or {@code UNKNOWN} if {@code null}. */
	public static String nameOf(Object obj) {
		return nameOf(obj, UNKNOWN);
	}

	/** Returns the class name of the given object, or the given default if {@code null}. */
	public static String nameOf(Object obj, String def) {
		return obj != null ? obj.getClass().getName() : def;
	}

	/**
	 * Invoke the method corresponding to the given method name having no parameters on the given
	 * target object. The method may be private. Invokes the first matching method found in the
	 * class hierarchy of the given target.
	 *
	 * @param <T>    the return object type
	 * @param target the invocation target object
	 * @param name   the method name
	 * @return the invocation result
	 * @throws Throwable on any failure
	 */
	public static <T> T invoke(Object target, String methodName) throws Throwable {
		return invoke(target, methodName, NoParams, NoArgs);
	}

	/**
	 * Invoke the method corresponding to the given method name and parameter types on the given
	 * target object. The method may be private. Invokes the first matching method found in the
	 * class hierarchy of the given target.
	 *
	 * @param <T>    the return object type
	 * @param target the invocation target object
	 * @param name   the method name
	 * @param params the method parameter types
	 * @param args   the method parameter arguments
	 * @return the invocation result
	 * @throws Throwable on any failure
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invoke(Object target, String name, Class<?>[] params, Object[] args) throws Throwable {
		Class<?> cls = find(target.getClass(), name, params);
		Method m = cls.getDeclaredMethod(name, params);
		m.setAccessible(true);
		return (T) m.invoke(target, args);
	}

	/**
	 * Finds the Class (the given class or a superclass thereof) that declares a method with the
	 * given name and parameter types.
	 *
	 * @param from   the initial class to consider
	 * @param name   the target method name
	 * @param params the target method parameter types
	 * @return the Class that declares a method with the given name and parameter types, or
	 *         {@code null} if not found
	 */
	public static Class<?> find(Class<?> from, String name, Class<?>[] params) {
		Class<?> cls = from;
		while (cls != null) {
			Method[] methods = cls.getDeclaredMethods();
			for (Method method : methods) {
				if (method.getName().equals(name)) {
					if (Arrays.equals(method.getParameterTypes(), params)) {
						return cls;
					}
				}
			}
			cls = cls.getSuperclass();
		}
		return null;
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
	 * Returns the text content types known to the platform at plugin startup. Includes an 'UNKNOWN'
	 * placeholder entry for unknown/undefined content types
	 *
	 * @return the known text content type identifiers
	 */
	public static Set<IContentType> platformTextTypes() {
		if (platformTextTypes == null) {
			IContentTypeManager mgr = Platform.getContentTypeManager();
			IContentType txtType = getPlatformTextType();
			platformTextTypes = Stream.of(mgr.getAllContentTypes()).filter(t -> t.isKindOf(txtType))
					.collect(Collectors.toCollection(LinkedHashSet::new));

			// add a limited placeholder entry for unknown/undefined content types
			platformTextTypes.add(ContentTypeAdaptor.unknown(txtType));

			// make immutable
			platformTextTypes = Collections.unmodifiableSet(platformTextTypes);
		}
		return platformTextTypes;
	}

	/**
	 * Returns the well-defined platform text content type.
	 *
	 * @return the text content type
	 */
	public static IContentType getPlatformTextType() {
		if (txtType == null) {
			IContentTypeManager mgr = Platform.getContentTypeManager();
			txtType = mgr.getContentType(IContentTypeManager.CT_TEXT);
		}
		return txtType;
	}

	/**
	 * Returns the platform text content type matching the given type identifier.
	 *
	 * @return the text content type for the given identifier, or {@code null} if not found
	 */
	public static IContentType getPlatformTextType(String id) {
		return platformTextTypes().stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
	}

	/**
	 * Returns the platform text content types matching the given type identifiers.
	 *
	 * @return set of text content types for the given identifiers
	 */
	public static Set<IContentType> getPlatformTextType(Set<String> ids) {
		return ids.stream() //
				.map(id -> getPlatformTextType(id)) //
				.filter(Objects::nonNull) //
				.collect(Collectors.toUnmodifiableSet());
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
	public static LinkedHashSet<String> undelimit(String delimited) {
		LinkedHashSet<String> types = new LinkedHashSet<>();
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
	public static <T> LinkedList<T> toList(T[] array) {
		return Arrays.stream(array).collect(Collectors.toCollection(LinkedList::new));
	}

	/**
	 * Convert the given array to an {@code LinkedHashSet} of the same type.
	 *
	 * @param array the source array to convert
	 * @return a set containing the array elements
	 */
	public static <T> LinkedHashSet<T> toSet(T[] array) {
		return Arrays.stream(array).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Subtracts from set A all elements in common with those in set B.
	 * <p>
	 * [0,1,2], [1,2,3] -> [0]
	 */
	public static <T> LinkedHashSet<T> subtract(Set<T> a, Set<T> b) {
		LinkedHashSet<T> res = new LinkedHashSet<>(a);
		res.removeAll(b);
		return res;
	}

	/**
	 * Returns the disjoint set of A and B.
	 * <p>
	 * [0,1,2], [1,2,3] -> [0,3]
	 */
	public static <T> LinkedHashSet<T> disjoint(Set<T> a, Set<T> b) {
		LinkedHashSet<T> res = new LinkedHashSet<>();
		res.addAll(subtract(a, b));
		res.addAll(subtract(b, a));
		return res;
	}

	public static class Delta<T> {

		public final Set<T> added;
		public final Set<T> rmved;

		/**
		 * Computes the delta between a prior set A and later set B.
		 * <p>
		 * [0,1,2], [1,2,3] -> added [3] & removed [0]
		 *
		 * @param prior the relatively earlier state set
		 * @param later the relatively later state set
		 * @return the delta between the prior set A and later set B
		 */
		public static <T> Delta<T> of(Set<T> prior, Set<T> later) {
			return new Delta<>(subtract(later, prior), subtract(prior, later));
		}

		private Delta(Set<T> added, Set<T> rmved) {
			this.added = Collections.unmodifiableSet(added);
			this.rmved = Collections.unmodifiableSet(rmved);
		}

		/** Returns {@code true} if either added or removed set is non-empty. */
		public boolean changed() {
			return increased() || decreased();
		}

		/** Returns {@code true} if the added set is non-empty. */
		public boolean increased() {
			return !added.isEmpty();
		}

		/** Returns {@code true} if the removed set is non-empty. */
		public boolean decreased() {
			return !rmved.isEmpty();
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
		boolean adv = gc.getAdvanced();
		gc.dispose();
		return adv;
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
