/******************************************************************************
 * Copyright (c) 2006-2018 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
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
		store.setDefault(Settings.ENABLED, true);
		store.setDefault(Settings.LINE_ALPHA, 50);
		store.setDefault(Settings.LINE_STYLE, SWT.LINE_SOLID);
		store.setDefault(Settings.LINE_WIDTH, 1);
		store.setDefault(Settings.LINE_SHIFT, 2);
		store.setDefault(Settings.LINE_COLOR, "0,0,0"); //$NON-NLS-1$
		store.setDefault(Settings.DRAW_LEFT_END, false);
		store.setDefault(Settings.DRAW_BLANK_LINE, true);
		store.setDefault(Settings.SKIP_COMMENT_BLOCK, false);

		IContentTypeManager mgr = Platform.getContentTypeManager();
		IContentType textType = mgr.getContentType(IContentTypeManager.CT_TEXT);
		StringBuilder sb = new StringBuilder();
		for (IContentType type : mgr.getAllContentTypes()) {
			if (type.isKindOf(textType)) {
				sb.append(type.getId() + "|");
			}
		}
		sb.setLength(sb.length() - 1);
		store.setDefault(Settings.CONTENT_TYPES, sb.toString());
	}
}
