/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.util;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;

public class WindowAdaptor implements IWindowListener {

	@Override
	public void windowOpened(IWorkbenchWindow window) {}

	@Override
	public void windowActivated(IWorkbenchWindow window) {}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {}

	@Override
	public void windowClosed(IWorkbenchWindow window) {}
}
