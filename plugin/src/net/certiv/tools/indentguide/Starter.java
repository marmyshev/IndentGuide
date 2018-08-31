package net.certiv.tools.indentguide;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension4;
import org.eclipse.ui.texteditor.ITextEditor;

import net.certiv.tools.indentguide.preferences.PreferenceConstants;

public class Starter implements IStartup {

	private IPainter painter;

	private void addListener(IEditorPart part) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		if (store.getBoolean(PreferenceConstants.ENABLED)) {
			if (part instanceof AbstractTextEditor) {
				IContentType contentType = null;
				ITextEditor textEditor = (ITextEditor) part;
				IDocumentProvider provider = textEditor.getDocumentProvider();
				if (provider instanceof IDocumentProviderExtension4) {
					try {
						contentType = ((IDocumentProviderExtension4) provider)
								.getContentType(textEditor.getEditorInput());
					} catch (CoreException e) {}
				}
				if (contentType == null) {
					return;
				}
				String id = contentType.getId();
				String[] types = store.getString(PreferenceConstants.CONTENT_TYPES).split("\\|");
				List<String> contentTypes = Arrays.asList(types);
				if (!contentTypes.contains(id)) return;

				Class<?> editor = part.getClass();
				while (!editor.equals(AbstractTextEditor.class)) {
					editor = editor.getSuperclass();
				}
				try {
					Method method = editor.getDeclaredMethod("getSourceViewer", (Class[]) null); // $NON-NLS-1$
					method.setAccessible(true);
					Object viewer = method.invoke(part, (Object[]) null);
					if (viewer instanceof ITextViewerExtension2) {
						painter = new IndentGuidePainter((ITextViewer) viewer);
						((ITextViewerExtension2) viewer).addPainter(painter);
					}
				} catch (Exception e) {
					Activator.log(e);
				}
			}
		}
	}

	@Override
	public void earlyStartup() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				if (window != null) {
					IWorkbenchPage page = window.getActivePage();
					if (page != null) {
						IEditorPart part = page.getActiveEditor();
						if (part != null) {
							addListener(part);
						}
					}
					window.getPartService().addPartListener(new PartListener());
				}
				workbench.addWindowListener(new WindowListener());
			}
		});
	}

	private class PartListener implements IPartListener2 {

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof IEditorPart) {
				addListener((IEditorPart) part);
			}
		}

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
	}

	private class WindowListener implements IWindowListener {

		@Override
		public void windowOpened(IWorkbenchWindow window) {
			if (window != null) {
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					IEditorPart part = page.getActiveEditor();
					if (part != null) {
						addListener(part);
					}
				}
				window.getPartService().addPartListener(new PartListener());
			}
		}

		@Override
		public void windowActivated(IWorkbenchWindow window) {}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {}

		@Override
		public void windowClosed(IWorkbenchWindow window) {}
	}
}
