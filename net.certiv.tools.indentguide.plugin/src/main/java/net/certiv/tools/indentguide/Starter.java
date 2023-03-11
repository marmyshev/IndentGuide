/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide;

import java.util.LinkedHashSet;
import java.util.Objects;
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
import org.eclipse.ui.themes.IThemeManager;

import net.certiv.tools.indentguide.adaptors.PartAdaptor;
import net.certiv.tools.indentguide.adaptors.WindowAdaptor;
import net.certiv.tools.indentguide.painter.GuidePainter;
import net.certiv.tools.indentguide.preferences.Pref;
import net.certiv.tools.indentguide.util.MsgBuilder;
import net.certiv.tools.indentguide.util.Utils;
import net.certiv.tools.indentguide.util.Utils.Delta;

public class Starter implements IStartup {

	private static final String ACTIVE_EDITOR = "getActiveEditor"; // $NON-NLS-1$
	private static final String SOURCE_VIEWER = "getSourceViewer"; // $NON-NLS-1$

	private IPreferenceStore store;
	private Set<String> excludedTypeIds;	// excluded content type ids

	// value=unique data records
	private final LinkedHashSet<Data> datas = new LinkedHashSet<>();

	private final PartWatcher partWatcher = new PartWatcher();
	private final PropWatcher propWatcher = new PropWatcher();

	@Override
	public void earlyStartup() {
		UIJob job = new UIJob("Indent Guide Startup") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IWorkbench wb = PlatformUI.getWorkbench();
				wb.getThemeManager().addPropertyChangeListener(propWatcher);
				store = Activator.getDefault().getPreferenceStore();
				store.addPropertyChangeListener(propWatcher);

				updateContentTypes();
				initWorkbenchWindows();

				wb.addWindowListener(new WindowWatcher());
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.SHORT);
		job.setSystem(true);
		job.schedule(200);
	}

	private void initWorkbenchWindows() {
		IWorkbench wb = PlatformUI.getWorkbench();
		for (IWorkbenchWindow window : wb.getWorkbenchWindows()) {
			initWorkbenchWindow(window);
			window.getPartService().addPartListener(partWatcher);
		}
	}

	private void initWorkbenchWindow(IWorkbenchWindow window) {
		for (IWorkbenchPage page : window.getPages()) {
			IWorkbenchPart part = page.getActivePart();
			if (part instanceof MultiPageEditorPart || part instanceof AbstractTextEditor) {
				installPainter(part);
			}
		}
	}

	private void installPainter(IWorkbenchPart part) {
		if (!store.getBoolean(Pref.ENABLED)) return;

		AbstractTextEditor editor = activeEditor(part);
		if (editor == null) return;

		IContentType type = typeOf(editor);
		boolean valid = valid(type);
		Activator.log("painter %sallowed for '%s' [%s]", valid ? "" : "dis", srcname(editor), type.getName());
		if (!valid) return;

		try {
			ISourceViewer viewer = Utils.invoke(editor, SOURCE_VIEWER);

			if (viewer instanceof ITextViewerExtension2) {
				Data data = findRecord(part, editor);
				if (data == null) {
					data = new Data(part, editor, type, viewer);
					datas.add(data);
				}
				if (data.painter == null) {
					data.painter = new GuidePainter(viewer);
					((ITextViewerExtension2) viewer).addPainter(data.painter);
					Activator.log("painter installed");
				}

			} else {
				Activator.log("painter not installable in viewer [%s]", Utils.nameOf(viewer));
			}

		} catch (Throwable e) {
			Activator.log(e);
		}
	}

	private AbstractTextEditor activeEditor(IWorkbenchPart part) {
		IEditorPart editor = null;

		if (part instanceof FormEditor) {
			editor = ((FormEditor) part).getActiveEditor();

		} else if (part instanceof MultiPageEditorPart) {
			try {
				editor = Utils.invoke(part, ACTIVE_EDITOR);
			} catch (Throwable e) {
				Activator.log(e);
			}

		} else if (part instanceof IEditorPart) {
			editor = (IEditorPart) part;
		}

		return (editor instanceof AbstractTextEditor) ? (AbstractTextEditor) editor : null;
	}

	private Data findRecord(IWorkbenchPart part, AbstractTextEditor editor) {
		return datas.stream() //
				.filter(d -> d.part.equals(part) && d.editor.equals(editor)) //
				.findFirst() //
				.orElse(null);
	}

	private boolean valid(IContentType type) {
		if (type == null) return false;
		return !excludedTypeIds.contains(type.getId());
	}

	private IContentType typeOf(AbstractTextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		if (provider instanceof IDocumentProviderExtension4) {
			try {
				IContentType type = ((IDocumentProviderExtension4) provider).getContentType(editor.getEditorInput());
				if (type != null) return type;
			} catch (CoreException e) {
				Activator.log(e);
			}
		}
		return Utils.getPlatformTextType(Utils.UNKNOWN);
	}

	private String srcname(AbstractTextEditor editor) {
		IEditorInput src = editor.getEditorInput();
		return src != null ? src.getName() : Utils.UNKNOWN;
	}

	private void updateContentTypes() {
		excludedTypeIds = Utils.undelimit(store.getString(Pref.CONTENT_TYPES));
	}

	private void refreshAll() {
		Activator.log("refreshAll...");
		for (Data d : datas) {
			if (d.painter != null) {
				d.painter.loadPrefs();
				d.painter.redrawAll();
			}
		}
	}

	private void deactivate(IWorkbenchPart part) {
		AbstractTextEditor editor = activeEditor(part);
		Data d = findRecord(part, editor);
		if (d != null && d.painter != null) {
			((ITextViewerExtension2) d.viewer).removePainter(d.painter);
			d.painter = null;
		}
	}

	private void deactivate(Set<IContentType> types) {
		for (Data d : datas) {
			if (types.contains(d.type) && d.painter != null) {
				((ITextViewerExtension2) d.viewer).removePainter(d.painter);
				d.painter = null;
			}
		}
	}

	private void deactivateAll() {
		for (Data d : datas) {
			if (d.painter != null) {
				((ITextViewerExtension2) d.viewer).removePainter(d.painter);
				d.painter = null;
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
			if (part instanceof MultiPageEditorPart || part instanceof AbstractTextEditor) {
				installPainter(part);
				Activator.log("part opened '%s'", Utils.nameOf(part));
			}
		}

		@Override
		public void partClosed(IWorkbenchPartReference ref) {
			IWorkbenchPart part = ref.getPart(false);
			if (part instanceof MultiPageEditorPart || part instanceof AbstractTextEditor) {
				deactivate(part);
				Activator.log("part closed '%s'", Utils.nameOf(part));
			}
		}

		@Override
		public void pageChanged(PageChangedEvent evt) {
			IPageChangeProvider provider = evt.getPageChangeProvider();
			if (provider instanceof MultiPageEditorPart) {
				Activator.log("MultiPageEditor page change '%s'", Utils.nameOf(provider));
				installPainter((IWorkbenchPart) provider);
			}
		}
	}

	private class PropWatcher implements IPropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String prop = evt.getProperty();
			Object old = evt.getOldValue();
			Object now = evt.getNewValue();

			if (prop.equals(IThemeManager.CHANGE_CURRENT_THEME)) {
				Activator.log("theme change '%s' [%s] => [%s]", prop, old, now);
				refreshAll();

			} else if (prop.startsWith(Pref.KEY)) {
				if (prop.equals(Pref.ENABLED)) {
					Activator.log("status change '%s' [%s] => [%s]", prop, old, now);
					if ((boolean) now) {
						initWorkbenchWindows();

					} else {
						deactivateAll();
					}

				} else if (prop.equals(Pref.CONTENT_TYPES)) {
					updateContentTypes();

					// Note: logic is reversed because it is an exclusion list
					Delta<String> delta = Delta.of(Utils.undelimit((String) now), Utils.undelimit((String) old));
					if (delta.changed()) {
						MsgBuilder mb = new MsgBuilder("content type change [%s]", prop);

						if (delta.increased()) {
							initWorkbenchWindows();
							mb.nl().indent("enabled  [%s]", delta.added);
						}

						if (delta.decreased()) {
							mb.nl().indent("disabled [%s]", delta.rmved);
							deactivate(Utils.getPlatformTextType(delta.rmved));
						}

						Activator.log(mb.toString());
					}

				} else {
					Activator.log("property change '%s' [%s] => [%s]", prop, old, now);
				}

				refreshAll();
			}
		}
	}

	private class Data {
		IWorkbenchPart part;
		AbstractTextEditor editor;
		IContentType type;
		ISourceViewer viewer;
		GuidePainter painter;
	
		Data(IWorkbenchPart part, AbstractTextEditor editor, IContentType type, ISourceViewer viewer) {
			this.part = part;
			this.editor = editor;
			this.type = type;
			this.viewer = viewer;
		}
	
		@Override
		public int hashCode() {
			return Objects.hash(part, editor);
		}
	
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Data d = (Data) obj;
			return Objects.equals(part, d.part) && Objects.equals(editor, d.editor);
		}
	}
}
