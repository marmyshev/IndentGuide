/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension4;
import org.eclipse.ui.themes.IThemeManager;

import net.certiv.tools.indentguide.adaptors.PartAdaptor;
import net.certiv.tools.indentguide.adaptors.WindowAdaptor;
import net.certiv.tools.indentguide.painter.GuidePainter;
import net.certiv.tools.indentguide.preferences.Pref;
import net.certiv.tools.indentguide.util.MsgBuilder;
import net.certiv.tools.indentguide.util.Utils;
import net.certiv.tools.indentguide.util.Utils.Delta;

public class Starter implements IStartup {

	private static final String UNKNOWN = "Unknown"; // $NON-NLS-1$

	private static final String ACTIVE_EDITOR = "getActiveEditor"; // $NON-NLS-1$
	private static final String SOURCE_VIEWER = "getSourceViewer"; // $NON-NLS-1$

	private IPreferenceStore store;
	private Set<String> excludedTypeIds;	// excluded content types

	// row=window; col=page/editor; val=painter
	private HashMap<IWorkbenchPart, HashMap<ISourceViewer, GuidePainter>> paintMap = new HashMap<>();

	private final IPropertyChangeListener propsListener = evt -> {
		String prop = evt.getProperty();
		Object old = evt.getOldValue();
		Object cur = evt.getNewValue();

		if (prop.equals(IThemeManager.CHANGE_CURRENT_THEME)) {
			Activator.log("theme change '%s' [%s] => [%s]", prop, old, cur);
			loadPaintPrefs();

		} else if (prop.startsWith(Pref.KEY)) {
			if (prop.equals(Pref.CONTENT_TYPES)) {
				updateContentTypes();

				Delta<String> delta = Utils.delta(Utils.undelimit((String) old), Utils.undelimit((String) cur));
				MsgBuilder mb = new MsgBuilder("content type change [%s]", prop);
				if (!delta.added.isEmpty()) mb.nl().indent("added   [%s]", delta.added);
				if (!delta.rmved.isEmpty()) mb.nl().indent("removed [%s]", delta.rmved);
				Activator.log(mb.toString());

			} else {
				Activator.log("property change '%s' [%s] => [%s]", prop, old, cur);
			}

			loadPaintPrefs();
		}
	};

	@Override
	public void earlyStartup() {
		UIJob job = new UIJob("Indent Guide Startup") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(propsListener);
				store = Activator.getDefault().getPreferenceStore();
				store.addPropertyChangeListener(propsListener);
				updateContentTypes();

				IWorkbench workbench = PlatformUI.getWorkbench();
				for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
					initWorkbenchWindow(window);
				}

				workbench.addWindowListener(new WindowWatcher());
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.SHORT);
		job.setSystem(true);
		job.schedule();
	}

	private void initWorkbenchWindow(IWorkbenchWindow window) {
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IWorkbenchPart part = page.getActivePart();
				Activator.log("workbench page [%s]", name(part));

				if (part instanceof MultiPageEditorPart) {
					IEditorPart editor = activeEditor((MultiPageEditorPart) part);
					if (editor != null) installPainter(editor, part);

				} else {
					IEditorPart editor = page.getActiveEditor();
					if (editor != null) installPainter(editor, part);
				}
			}
			window.getPartService().addPartListener(new PartWatcher());
		}
	}

	private void installPainter(IEditorPart part, IWorkbenchPart window) {
		if (!store.getBoolean(Pref.ENABLED)) return;

		if (part instanceof AbstractTextEditor) {
			AbstractTextEditor editor = (AbstractTextEditor) part;
			Activator.log("inspecting editor [%s]", name(part));

			if (!validType(editor)) return;

			Class<?> cls = editor.getClass();
			while (!cls.equals(AbstractTextEditor.class)) {
				cls = cls.getSuperclass();
			}

			try {
				ISourceViewer viewer = Utils.invoke(cls, SOURCE_VIEWER);

				if (viewer instanceof ITextViewerExtension2) {
					HashMap<ISourceViewer, GuidePainter> painters = paintMap.get(window);
					if (painters == null) painters = new HashMap<>();
					if (!painters.containsKey(viewer)) {
						GuidePainter painter = new GuidePainter(viewer);
						painters.put(viewer, painter);

						((ITextViewerExtension2) viewer).addPainter(painter);
						Activator.log("painter installed");
					}
					paintMap.put(window, painters);
				}
			} catch (Throwable e) {
				Activator.log(e);
			}
		}
	}

	private IEditorPart activeEditor(MultiPageEditorPart mpe) {
		if (mpe instanceof FormEditor) {
			return ((FormEditor) mpe).getActiveEditor();
		}

		try {
			return Utils.invoke(mpe, ACTIVE_EDITOR);

		} catch (Throwable e) {
			Activator.log(e);
			return null;
		}
	}

	private boolean validType(AbstractTextEditor editor) {
		IEditorInput src = editor.getEditorInput();
		String srcname = src != null ? src.getName() : UNKNOWN;
		IContentType type = null;

		IDocumentProvider provider = editor.getDocumentProvider();
		if (provider instanceof IDocumentProviderExtension4) {
			try {
				type = ((IDocumentProviderExtension4) provider).getContentType(src);
			} catch (CoreException e) {
				Activator.log(e);
			}
		}

		if (type == null) {
			Activator.log("painter disallowed for '%s' [%s]", srcname, UNKNOWN);
			return false;
		}
		if (!excludedTypeIds.contains(type.getId())) {
			Activator.log("installing painter on '%s' [%s]", srcname, type.getName());
			return true;
		}

		Activator.log("painter disallowed for '%s' [%s]", srcname, type.getName());
		return false;
	}

	private void updateContentTypes() {
		excludedTypeIds = Utils.undelimit(store.getString(Pref.CONTENT_TYPES));
	}

	private String name(IWorkbenchPart part) {
		return part.getClass().getName();
	}

	private void loadPaintPrefs() {
		for (HashMap<ISourceViewer, GuidePainter> map : paintMap.values()) {
			for (GuidePainter painter : map.values()) {
				painter.loadPrefs();
				painter.redrawAll();
			}
		}
	}

	private class WindowWatcher extends WindowAdaptor {

		@Override
		public void windowOpened(IWorkbenchWindow window) {
			initWorkbenchWindow(window);
		}
	}

	private class PartWatcher extends PartAdaptor {

		@Override
		public void partOpened(IWorkbenchPartReference ref) {
			IWorkbenchPart part = ref.getPart(false);
			Activator.log("part opened '%s'", name(part));

			if (part instanceof MultiPageEditorPart) {
				IEditorPart editor = activeEditor((MultiPageEditorPart) part);
				if (editor != null) installPainter(editor, part);

			} else if (part instanceof IEditorPart) {
				installPainter((IEditorPart) part, part);
			}
		}

		@Override
		public void partClosed(IWorkbenchPartReference ref) {
			IWorkbenchPart part = ref.getPart(false);
			if (part instanceof MultiPageEditorPart || part instanceof AbstractTextEditor) {
				HashMap<ISourceViewer, GuidePainter> painters = paintMap.remove(part);
				if (painters != null) {
					for (GuidePainter painter : painters.values()) {
						painter.deactivate(true);
					}
					painters.clear();
				}
				Activator.log("part closed '%s'", name(part));
			}
		}

		@Override
		public void pageChanged(PageChangedEvent event) {
			IPageChangeProvider provider = event.getPageChangeProvider();
			if (provider instanceof MultiPageEditorPart) {
				Activator.log("MultiPageEditor page change '%s'", name((IWorkbenchPart) provider));

				IEditorPart editor = activeEditor((MultiPageEditorPart) provider);
				if (editor != null) {
					installPainter(editor, (IWorkbenchPart) provider);
				}
			}
		}
	}
}
