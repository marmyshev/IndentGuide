package net.certiv.tools.indentguide.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.certiv.tools.indentguide.Activator;
import net.certiv.tools.indentguide.util.Prefs;
import net.certiv.tools.indentguide.util.Utils;

public class SettingsPage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String[] STYLES = { Messages.Guide_style_solid, Messages.Guide_style_dash,
			Messages.Guide_style_dot, Messages.Guide_style_dash_dot, Messages.Guide_style_dash_dot_dot };

	private final List<Composite> blocks = new LinkedList<>();
	private final List<Object> parts = new LinkedList<>();

	// platform 'text' content type
	private IContentType txtType;

	// initial or last OKd content type ids
	private Set<String> current;

	public SettingsPage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
		txtType = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		current = Prefs.asLinkedSet(getPreferenceStore().getString(Settings.CONTENT_TYPES));
	}

	@Override
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);

		Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(comp);
		applyDialogFont(comp);

		createEnabledCheckBox(comp);

		createAttributeGroup(comp);
		createDrawingGroup(comp);
		createContentTypesGroup(comp);

		return comp;
	}

	private void createEnabledCheckBox(Composite comp) {
		Button btn = createLabeledCheckbox(comp, Messages.Guide_enabled_label, Settings.ENABLED);
		btn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				boolean state = btn.getSelection();
				for (Composite comp : blocks) {
					for (Control control : comp.getChildren()) {
						control.setEnabled(state);
					}
				}
			}
		});
		createVerticalSpacer(comp, 1);
	}

	private void createAttributeGroup(Composite parent) {
		Composite comp = createGroup(parent, Messages.Guide_attribute_group_label, false, 2);
		blocks.add(comp);

		createLabeledSpinner(comp, Messages.Guide_alpha_label, 0, 255, Settings.LINE_ALPHA);
		createLabeledCombo(comp, Messages.Guide_style_label, STYLES, Settings.LINE_STYLE);
		createLabeledSpinner(comp, Messages.Guide_width_label, 1, 8, Settings.LINE_WIDTH);
		createLabeledSpinner(comp, Messages.Guide_shift_label, 0, 8, Settings.LINE_SHIFT);
		createLabeledColorEditor(comp, Messages.Guide_color_label, colorKey());
	}

	private void createDrawingGroup(Composite parent) {
		Composite comp = createGroup(parent, Messages.Guide_drawing_group_label, false, 1);
		blocks.add(comp);

		createLabeledCheckbox(comp, Messages.Guide_draw_left_end_label, Settings.DRAW_LEFT_END);
		createLabeledCheckbox(comp, Messages.Guide_draw_blank_line_label, Settings.DRAW_BLANK_LINE);
		createLabeledCheckbox(comp, Messages.Guide_skip_comment_block_label, Settings.SKIP_COMMENT_BLOCK);
	}

	private void createContentTypesGroup(Composite parent) {
		Composite comp = createGroup(parent, Messages.Guide_contenttype_group_label, true, 1);
		blocks.add(comp);

		CheckboxTreeViewer viewer = new CheckboxTreeViewer(comp, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.getControl().setFont(comp.getFont());
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 150).grab(true, true).applyTo(viewer.getControl());
		parts.add(viewer);

		viewer.setContentProvider(new TypesContentProvider());
		viewer.setLabelProvider(new TypesLabelProvider());
		viewer.setComparator(new ViewerComparator());
		viewer.setInput(Platform.getContentTypeManager());
		viewer.addFilter(new TextTypeFilter());

		viewer.expandAll(); // force viewer internal state update
		viewer.collapseAll();
		viewer.expandToLevel(2);

		TypesContentProvider provider = (TypesContentProvider) viewer.getContentProvider();
		viewer.addCheckStateListener(evt -> {
			IContentType type = (IContentType) evt.getElement();
			updateCheckState(viewer, provider, type);
			Activator.log("state change %s [%s]", type, viewer.getChecked(type));
		});

		initCheckState(viewer, provider);
	}

	private Composite createGroup(Composite parent, String label, boolean vert, int cols) {
		Group group = new Group(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(group);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, vert).applyTo(group);
		group.setText(label);

		Composite comp = new Composite(group, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(cols).margins(5, 5).applyTo(comp);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, vert).indent(5, 0).applyTo(comp);

		return comp;
	}

	/** Creates a vertical spacer. */
	private void createVerticalSpacer(Composite comp, int lines) {
		createVerticalSpacer(comp, lines, 1);
	}

	/** Creates a vertical spacer. */
	private void createVerticalSpacer(Composite comp, int lines, int span) {
		Label lbl = new Label(comp, SWT.NONE);
		int height = Prefs.lineHeight(comp, lines);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, height).grab(true, false).span(span, 1).applyTo(lbl);
	}

	private Label createLabel(Composite comp, String text) {
		Label label = new Label(comp, SWT.NONE);
		label.setText(text);
		return label;
	}

	private Button createLabeledCheckbox(Composite comp, String label, String key) {
		Button btn = new Button(comp, SWT.CHECK);
		btn.setText(label);
		btn.setData(key);
		btn.setSelection(getPreferenceStore().getBoolean(key));
		parts.add(btn);
		return btn;
	}

	private Combo createLabeledCombo(Composite comp, String label, String[] styles, String key) {
		createLabel(comp, label);
		Combo combo = new Combo(comp, SWT.READ_ONLY);
		combo.setData(key);
		combo.setItems(styles);

		int idx = getPreferenceStore().getInt(key) - 1;
		idx = (idx >= 0 && idx < styles.length) ? idx : 0;
		combo.setText(styles[idx]);

		parts.add(combo);
		return combo;
	}

	private Spinner createLabeledSpinner(Composite comp, String label, int min, int max, String key) {
		createLabel(comp, label);
		Spinner spin = new Spinner(comp, SWT.BORDER);
		spin.setData(key);
		spin.setMinimum(min);
		spin.setMaximum(max);
		spin.setSelection(getPreferenceStore().getInt(key));
		parts.add(spin);
		return spin;
	}

	private void createLabeledColorEditor(Composite comp, String label, String key) {
		ColorFieldEditor editor = new ColorFieldEditor(key, Messages.Guide_color_label, comp);
		editor.setPreferenceStore(getPreferenceStore());
		editor.load();
		parts.add(editor);
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();

		for (Object part : parts) {
			if (part instanceof Button) {
				Button btn = ((Button) part);
				String key = (String) btn.getData();
				store.setValue(key, btn.getSelection());

			} else if (part instanceof Combo) {
				Combo combo = ((Combo) part);
				String key = (String) combo.getData();
				store.setValue(key, combo.getSelectionIndex() + 1);

			} else if (part instanceof Spinner) {
				Spinner spin = ((Spinner) part);
				String key = (String) spin.getData();
				store.setValue(key, spin.getSelection());

			} else if (part instanceof ColorFieldEditor) {
				ColorFieldEditor editor = (ColorFieldEditor) part;
				editor.store();
				Activator.getDefault().setColor(editor.getColorSelector().getColorValue());

			} else if (part instanceof CheckboxTreeViewer) {
				CheckboxTreeViewer viewer = (CheckboxTreeViewer) part;
				current = getCheckState(viewer);
				store.setValue(Settings.CONTENT_TYPES, Prefs.delimited(current));

				Set<String> alltypes = Prefs.asLinkedSet(store.getDefaultString(Settings.CONTENT_TYPES));
				Set<String> disabled = Utils.subtract(alltypes, current);
				if (!disabled.isEmpty()) Activator.log("content types disabled: %s", disabled);
			}
		}

		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();

		for (Object part : parts) {
			if (part instanceof Button) {
				Button btn = ((Button) part);
				String key = (String) btn.getData();
				btn.setSelection(store.getDefaultBoolean(key));

			} else if (part instanceof Combo) {
				Combo combo = ((Combo) part);
				String key = (String) combo.getData();
				int idx = store.getDefaultInt(key) - 1;
				combo.setText(STYLES[idx]);

			} else if (part instanceof Spinner) {
				Spinner spin = ((Spinner) part);
				String key = (String) spin.getData();
				spin.setSelection(store.getDefaultInt(key));

			} else if (part instanceof ColorFieldEditor) {
				ColorFieldEditor editor = (ColorFieldEditor) part;
				editor.loadDefault();
				Activator.getDefault().setColor(editor.getColorSelector().getColorValue());

			} else if (part instanceof CheckboxTreeViewer) {
				CheckboxTreeViewer viewer = (CheckboxTreeViewer) part;

				for (Object item : viewer.getCheckedElements()) {
					viewer.setChecked(item, false);
					viewer.setGrayed(item, false);
				}

				IContentTypeManager mgr = Platform.getContentTypeManager();
				TypesContentProvider provider = (TypesContentProvider) viewer.getContentProvider();

				current = Prefs.asLinkedSet(store.getDefaultString(Settings.CONTENT_TYPES));
				for (String id : current) {
					IContentType type = mgr.getContentType(id);
					if (!type.equals(txtType)) {
						viewer.setChecked(type, true);
						updateCheckState(viewer, provider, type);
					}
				}
			}
		}

		super.performDefaults();
	}

	private void initCheckState(CheckboxTreeViewer viewer, TypesContentProvider provider) {
		IContentTypeManager mgr = Platform.getContentTypeManager();
		for (String elem : current) {
			IContentType type = mgr.getContentType(elem);
			if (type != null && !type.equals(txtType)) {
				viewer.setChecked(type, true);
				updateCheckState(viewer, provider, type);
			}
		}
	}

	/* Returns the id of checked, but not grayed, elements in the tree viewer */
	private Set<String> getCheckState(CheckboxTreeViewer viewer) {
		Set<String> checked = new LinkedHashSet<>();
		for (Object item : viewer.getCheckedElements()) {
			if (!viewer.getGrayed(item)) {
				checked.add(((IContentType) item).getId());
			}
		}
		return checked;
	}

	private void updateCheckState(CheckboxTreeViewer viewer, TypesContentProvider provider, IContentType type) {
		boolean state = viewer.getChecked(type);

		// adj child states to match the current item state
		for (Object child : provider.getChildren(type)) {
			viewer.setChecked(child, state);
		}
		viewer.setGrayed(type, false);

		// adj parent hierarchy states to reflect the current item state
		IContentType parent = (IContentType) provider.getParent(type);
		while (parent != null) {
			LinkedList<Object> children = new LinkedList<>(Arrays.asList(provider.getChildren(parent)));

			boolean allchecked = children.stream().allMatch(e -> viewer.getChecked(e));
			boolean onechecked = children.stream().anyMatch(e -> viewer.getChecked(e));

			viewer.setGrayed(parent, onechecked && !allchecked);
			viewer.setChecked(parent, allchecked || onechecked);

			parent = parent.getBaseType();
		}
	}

	private String colorKey() {
		String key = Settings.LINE_COLOR;
		if (Activator.getDefault().isDarkTheme()) {
			key += Settings.DARK;
		}
		return key;
	}

	private class TextTypeFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (element instanceof IContentType) {
				IContentType type = (IContentType) element;
				return type.isKindOf(txtType);
			}
			return false;
		}
	}

	private class TypesLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			IContentType contentType = (IContentType) element;
			return contentType.getName();
		}
	}

	private class TypesContentProvider implements ITreeContentProvider {

		private IContentTypeManager mgr;

		@Override
		public Object[] getChildren(Object parent) {
			List<IContentType> elements = new ArrayList<>();
			IContentType base = (IContentType) parent;
			for (IContentType type : mgr.getAllContentTypes()) {
				if (Objects.equals(type.getBaseType(), base) && type.isKindOf(txtType)) {
					elements.add(type); // constrained to text content types
				}
			}
			return elements.toArray(new IContentType[0]);
		}

		@Override
		public Object getParent(Object element) {
			IContentType type = (IContentType) element;
			return type.getBaseType();
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(null);
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			mgr = (IContentTypeManager) newInput;
		}
	}
}
