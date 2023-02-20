/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;

import net.certiv.tools.indentguide.Activator;

/** Initialize default preference values. */
public class Initializer extends AbstractPreferenceInitializer {

	private static final String BLACK = "0,0,0"; // $NON-NLS-1$
	private static final String LIGHT = "192,192,192"; // $NON-NLS-1$

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(Pref.ENABLED, true);
		store.setDefault(Pref.LINE_ALPHA, 50);
		store.setDefault(Pref.LINE_STYLE, SWT.LINE_SOLID);
		store.setDefault(Pref.LINE_WIDTH, 1);
		store.setDefault(Pref.LINE_SHIFT, 2);
		store.setDefault(Pref.LINE_COLOR, BLACK);
		store.setDefault(Pref.LINE_COLOR + Pref.DARK, LIGHT);
		store.setDefault(Pref.DRAW_LEFT_EDGE, false);
		store.setDefault(Pref.DRAW_BLANK_LINE, true);
		store.setDefault(Pref.DRAW_COMMENT_BLOCK, false);
		store.setDefault(Pref.CONTENT_TYPES, "");
	}
}
