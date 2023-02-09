/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

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
import org.eclipse.jface.util.PropertyChangeEvent;
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

import net.certiv.tools.indentguide.preferences.Settings;
import net.certiv.tools.indentguide.util.PartAdaptor;
import net.certiv.tools.indentguide.util.WindowAdaptor;

public class Starter implements IStartup {

	private static final Class<?>[] CLS_TYPE = null;
	private static final Object[] ARG_TYPE = null;

	private static final String DELIM = "\\|"; // $NON-NLS-1$
	private static final String UNKNOWN = "Unknown"; // $NON-NLS-1$

	private static final String ACTIVE_EDITOR = "getActiveEditor"; // $NON-NLS-1$
	private static final String SOURCE_VIEWER = "getSourceViewer"; // $NON-NLS-1$

	private IPreferenceStore store;
	private HashSet<String> contentTypes;

	// row=window; col=page/editor; val=painter
	private HashMap<IWorkbenchPart, HashMap<ISourceViewer, IndentGuidePainter>> paintMap = new HashMap<>();

	@Override
	public void earlyStartup() {
		UIJob job = new UIJob("Indent Guide Startup") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				store = Activator.getDefault().getPreferenceStore();
				store.addPropertyChangeListener(new StoreWatcher());
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
				Activator.log("Indent painter: workbench page '%s'", name(part));

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
		if (!store.getBoolean(Settings.ENABLED)) return;
		Activator.log("Indent painter: inspecting editor '%s'", name(part));

		if (part instanceof AbstractTextEditor) {
			AbstractTextEditor editor = (AbstractTextEditor) part;
			if (!validType(editor)) return;

			Class<?> cls = editor.getClass();
			while (!cls.equals(AbstractTextEditor.class)) {
				cls = cls.getSuperclass();
			}
			try {
				Method method = cls.getDeclaredMethod(SOURCE_VIEWER, CLS_TYPE);
				method.setAccessible(true);
				ISourceViewer viewer = (ISourceViewer) method.invoke(editor, ARG_TYPE);
				if (viewer instanceof ITextViewerExtension2) {
					HashMap<ISourceViewer, IndentGuidePainter> painters = paintMap.get(window);
					if (painters == null) painters = new HashMap<>();
					if (!painters.containsKey(viewer)) {
						IndentGuidePainter painter = new IndentGuidePainter(viewer);
						painters.put(viewer, painter);

						((ITextViewerExtension2) viewer).addPainter(painter);
						Activator.log("Indent painter: installed");
					}
					paintMap.put(window, painters);
				}
			} catch (Exception e) {
				Activator.log(e);
			}
		}
	}

	private IEditorPart activeEditor(MultiPageEditorPart mpe) {
		if (mpe instanceof FormEditor) {
			return ((FormEditor) mpe).getActiveEditor();
		}

		try {
			Method method = mpe.getClass().getDeclaredMethod(ACTIVE_EDITOR, CLS_TYPE);
			method.setAccessible(true);
			return (IEditorPart) method.invoke(mpe, ARG_TYPE);

		} catch (Exception e) {
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
			Activator.log("Indent painter: disallowed for '%s' [%s]", srcname, UNKNOWN);
			return false;
		}
		if (contentTypes.contains(type.getId())) {
			Activator.log("Indent painter: installing on '%s' [%s]", srcname, type.getName());
			return true;
		}

		Activator.log("Indent painter: disallowed for '%s' [%s]", srcname, type.getName());
		return false;
	}

	private void updateContentTypes() {
		contentTypes = new HashSet<>();
		String spec = store.getString(Settings.CONTENT_TYPES);
		String[] types = spec.split(DELIM);
		contentTypes.addAll(Arrays.asList(types));
	}

	private String name(IWorkbenchPart part) {
		return part.getClass().getName();
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
			Activator.log("Indent painter: part opened '%s'", name(part));

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
				HashMap<ISourceViewer, IndentGuidePainter> painters = paintMap.remove(part);
				if (painters != null) {
					for (IndentGuidePainter painter : painters.values()) {
						painter.deactivate(true);
					}
					painters.clear();
				}
			}
			Activator.log("Indent painter: part closed '%s'", name(part));
		}

		@Override
		public void pageChanged(PageChangedEvent event) {
			IPageChangeProvider provider = event.getPageChangeProvider();
			if (provider instanceof MultiPageEditorPart) {
				Activator.log("Indent painter: page change '%s'", name((IWorkbenchPart) provider));

				IEditorPart editor = activeEditor((MultiPageEditorPart) provider);
				if (editor != null) {
					installPainter(editor, (IWorkbenchPart) provider);

				} else {
					Activator.log("Indent painter: no editor on active MultiPageEditor page");
				}
			}
		}
	}

	private class StoreWatcher implements IPropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().startsWith(Settings.KEY)) {
				updateContentTypes();
			}
		}
	}
}
