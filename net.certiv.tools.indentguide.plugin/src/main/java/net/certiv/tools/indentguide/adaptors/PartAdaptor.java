/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.adaptors;

import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

public class PartAdaptor implements IPartListener2, IPageChangedListener {

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {}

	@Override
	public void pageChanged(PageChangedEvent event) {}
}
