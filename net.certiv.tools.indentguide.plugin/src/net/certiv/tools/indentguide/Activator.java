/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.themes.ColorUtil;
import org.osgi.framework.BundleContext;

import net.certiv.tools.indentguide.preferences.Settings;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "net.certiv.tools.indentguide"; //$NON-NLS-1$
	private static final String EditorsID = "org.eclipse.ui.editors"; //$NON-NLS-1$
	private static final String PREFIX = "Indent Guide: "; //$NON-NLS-1$

	private static Activator plugin;

	private final IEclipsePreferences[] editorScopes = new IEclipsePreferences[] {
			InstanceScope.INSTANCE.getNode(EditorsID), DefaultScope.INSTANCE.getNode(EditorsID) };

	private final IPropertyChangeListener themeChange = event -> {
		disposeLineColor();
		log("Theme change '%s'", event);
	};

	private Color color;

	public Activator() {
		super();
	}

	/** Returns the shared instance */
	public static Activator getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeChange);
		log("Startup");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeChange);
		disposeLineColor();
		editorScopes[0] = editorScopes[1] = null;
		plugin = null;
		super.stop(context);
	}

	public Color getColor() {
		if (color == null) {
			String key = Settings.LINE_COLOR;
			if (isDarkTheme()) {
				key += Settings.DARK;
			}
			String spec = getPreferenceStore().getString(key);
			color = new Color(PlatformUI.getWorkbench().getDisplay(), ColorUtil.getColorValue(spec));
			log("Line color set %s -> %s", key, spec);
		}
		return color;
	}

	public void setColor(Color color) {
		disposeLineColor();
		this.color = color;
	}

	public void setColor(RGB rgb) {
		setColor(new Color(PlatformUI.getWorkbench().getDisplay(), rgb));
	}

	/**
	 * Returns {@code true} if the current theme is 'dark', defined as where the foreground color is
	 * relatively darker than the background color. (black -> '0'; white -> '255*3')
	 */
	public boolean isDarkTheme() {
		RGB fg = getRawRGB(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
		RGB bg = getRawRGB(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
		return (fg.red + fg.blue + fg.green) > (bg.red + bg.blue + bg.green);
	}

	private RGB getRawRGB(String key) {
		String value = Platform.getPreferencesService().get(key, null, editorScopes);
		if (value == null) return PreferenceConverter.COLOR_DEFAULT_DEFAULT;

		RGB rgb = StringConverter.asRGB(value, null);
		if (rgb == null) return PreferenceConverter.COLOR_DEFAULT_DEFAULT;
		return rgb;
	}

	private void disposeLineColor() {
		if (color != null) {
			color.dispose();
			color = null;
		}
	}

	public static void log(String fmt, Object... args) {
		plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, PREFIX + String.format(fmt, args)));
	}

	public static void log(Throwable e) {
		plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage()));
	}
}
