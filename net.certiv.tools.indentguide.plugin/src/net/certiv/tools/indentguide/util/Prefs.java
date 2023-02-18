package net.certiv.tools.indentguide.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.widgets.Control;

public class Prefs {

	private static final String DELIM = "|"; // $NON-NLS-1$

	private Prefs() {}

	/**
	 * Returns the text content type identifiers known to the platform.
	 * 
	 * @return the text content type identifiers known to the platform.
	 */
	public static Set<String> platformTextTypes() {
		IContentTypeManager mgr = Platform.getContentTypeManager();
		IContentType text = mgr.getContentType(IContentTypeManager.CT_TEXT);

		Set<String> types = new LinkedHashSet<>();
		for (IContentType type : mgr.getAllContentTypes()) {
			if (type.isKindOf(text)) {
				types.add(type.getId());
			}
		}
		return types;
	}

	public static Set<String> asLinkedSet(String delimited) {
		Set<String> types = new LinkedHashSet<>();
		StringTokenizer tokens = new StringTokenizer(delimited, DELIM);
		while (tokens.hasMoreTokens()) {
			types.add(tokens.nextToken());
		}
		return types;
	}

	public static String delimited(Set<String> terms) {
		return String.join(DELIM, terms);
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
}
