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
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import net.certiv.tools.indentguide.util.MsgBuilder;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "net.certiv.tools.indentguide"; //$NON-NLS-1$
	private static final String PREFIX = "Indent Guide: "; //$NON-NLS-1$

	private static Activator plugin;

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
		log("Starting...");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	// ------------------------------------------

	public static void log(Throwable e) {
		plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage()));
	}

	public static void log(String fmt, Object... args) {
		String msg = print(PREFIX + String.format(fmt, args));
		plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, msg));
	}

	public static void log(MsgBuilder mb) {
		String msg = print(PREFIX + mb.toString());
		plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, msg));
	}

	private static String print(String msg) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
		return info(caller.getClassName(), caller.getLineNumber(), msg);
	}

	private static String info(String clsname, int line, String msg) {
		if (clsname.startsWith(PLUGIN_ID)) {
			clsname = clsname.substring(PLUGIN_ID.length() + 1);
		}
		return String.format("%s:%s \t%s", clsname, line, msg);
	}
}
