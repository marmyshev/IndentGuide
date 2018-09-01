package net.certiv.tools.indentguide.preferences;

import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;

import net.certiv.tools.indentguide.Activator;

/** Initialize default preference values. */
public class Initializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(Keys.ENABLED, true);
		store.setDefault(Keys.LINE_ALPHA, 50);
		store.setDefault(Keys.LINE_STYLE, SWT.LINE_SOLID);
		store.setDefault(Keys.LINE_WIDTH, 1);
		store.setDefault(Keys.LINE_SHIFT, 3);
		store.setDefault(Keys.LINE_COLOR, "0,0,0"); //$NON-NLS-1$
		store.setDefault(Keys.DRAW_LEFT_END, true); // $NON-NLS-1$
		store.setDefault(Keys.DRAW_BLANK_LINE, false); // $NON-NLS-1$
		store.setDefault(Keys.SKIP_COMMENT_BLOCK, false); // $NON-NLS-1$
		store.setDefault(Keys.CONTENT_TYPES, IContentTypeManager.CT_TEXT);
	}
}
