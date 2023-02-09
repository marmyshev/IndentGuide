package net.certiv.tools.indentguide.util;

import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;

public class PageAdaptor implements IPageChangeProvider {

	@Override
	public Object getSelectedPage() {
		return null;
	}

	@Override
	public void addPageChangedListener(IPageChangedListener listener) {}

	@Override
	public void removePageChangedListener(IPageChangedListener listener) {}

}
