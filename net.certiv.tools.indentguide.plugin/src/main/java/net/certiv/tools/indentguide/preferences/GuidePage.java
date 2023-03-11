package net.certiv.tools.indentguide.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
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
import net.certiv.tools.indentguide.util.Utils;

public class GuidePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String[] STYLES = { //
			Messages.style_solid, //
			Messages.style_dash, //
			Messages.style_dot, //
			Messages.style_dash_dot, //
			Messages.style_dash_dot_dot //
	};

	private final List<Composite> blocks = new LinkedList<>();
	private final List<Object> parts = new LinkedList<>();

	// platform 'text' content type
	private final IContentType txtType;

	// explicitly excluded content types; current as of plugin startup or last OKd values
	private Set<IContentType> excludeTypes;

	public GuidePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());

		txtType = Utils.getPlatformTextType();

		Set<String> exclude = Utils.undelimit(getPreferenceStore().getString(Pref.CONTENT_TYPES));
		excludeTypes = exclude.stream() //
				.map(e -> Utils.getPlatformTextType(e)) //
				.filter(t -> !t.equals(txtType)) //
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Override
	public void init(IWorkbench workbench) {}

	@Override
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);

		Composite comp = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
		GridLayoutFactory.fillDefaults().applyTo(comp);

		createEnabledCheckBox(comp);

		createAttributeGroup(comp);
		createDrawingGroup(comp);
		createContentTypesGroup(comp);

		applyDialogFont(comp);
		return comp;
	}

	private void createEnabledCheckBox(Composite comp) {
		Button btn = createLabeledCheckbox(comp, Messages.enabled_label, Pref.ENABLED);
		btn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				boolean active = btn.getSelection();
				for (Composite comp : blocks) {
					for (Control control : comp.getChildren()) {
						control.setEnabled(active);
					}
				}
			}
		});
		createVerticalSpacer(comp, 1);
	}

	private void createAttributeGroup(Composite parent) {
		Composite comp = createGroup(parent, Messages.attribute_group_label, false, 3);
		blocks.add(comp);

		createLabeledSpinner(comp, Messages.alpha_label1, Messages.alpha_label2, 0, 255, Pref.LINE_ALPHA);
		createLabeledCombo(comp, Messages.style_label1, Messages.style_label2, STYLES, Pref.LINE_STYLE);
		createLabeledSpinner(comp, Messages.width_label1, Messages.width_label2, 1, 8, Pref.LINE_WIDTH);
		createLabeledSpinner(comp, Messages.shift_label1, Messages.shift_label2, 0, 8, Pref.LINE_SHIFT);
		createLabeledColorEditor(comp, Messages.color_label1, Messages.color_label2, colorKey());
	}

	private void createDrawingGroup(Composite parent) {
		Composite comp = createGroup(parent, Messages.drawing_group_label, false, 1);
		blocks.add(comp);

		createLabeledCheckbox(comp, Messages.draw_lead_edge_label, Pref.DRAW_LEAD_EDGE);
		createLabeledCheckbox(comp, Messages.draw_blank_line_label, Pref.DRAW_BLANK_LINE);
		createLabeledCheckbox(comp, Messages.draw_comment_block_label, Pref.DRAW_COMMENT_BLOCK);
	}

	private void createContentTypesGroup(Composite parent) {
		Composite comp = createGroup(parent, Messages.contenttype_group_label, true, 1);
		blocks.add(comp);

		CheckboxTreeViewer viewer = new CheckboxTreeViewer(comp, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.getControl().setFont(comp.getFont());
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 150).grab(true, true).applyTo(viewer.getControl());
		parts.add(viewer);

		viewer.setContentProvider(new TypesContentProvider());
		viewer.setLabelProvider(new TypesLabelProvider());
		viewer.setComparator(new ViewerComparator());
		viewer.addFilter(new TextTypeFilter());
		viewer.setInput(Platform.getContentTypeManager());

		viewer.expandAll(); // req'd to force viewer internal state update
		viewer.collapseAll();
		viewer.expandToLevel(2);

		// init all to checked
		for (Object item : Utils.platformTextTypes()) {
			viewer.setChecked(item, true);
			viewer.setGrayed(item, false);
		}

		// remove excluded
		for (IContentType exType : excludeTypes) {
			viewer.setChecked(exType, false);
			updateCheckState(viewer, exType, false);
		}

		viewer.addCheckStateListener(evt -> {
			IContentType type = (IContentType) evt.getElement();
			boolean state = viewer.getChecked(type);
			updateCheckState(viewer, type, state);
			Activator.log("state change %s [%s]", type, state);
		});
	}

	private Composite createGroup(Composite parent, String label, boolean vert, int cols) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(label);
		GridLayoutFactory.fillDefaults().applyTo(group);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, vert).applyTo(group);

		Composite comp = new Composite(group, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(cols).equalWidth(false).margins(5, 5).applyTo(comp);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, vert).indent(5, 0).applyTo(comp);

		return comp;
	}

	private void createVerticalSpacer(Composite comp, int lines) {
		createVerticalSpacer(comp, lines, 1);
	}

	private void createVerticalSpacer(Composite comp, int lines, int span) {
		Label spacer = new Label(comp, SWT.NONE);
		int height = Utils.lineHeight(comp, lines);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, height).grab(true, false).span(span, 1).applyTo(spacer);
	}

	private Label createLabel(Composite comp, String text) {
		Label label = new Label(comp, SWT.NONE);
		label.setText(text);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
		return label;
	}

	private Button createLabeledCheckbox(Composite comp, String label, String key) {
		Button btn = new Button(comp, SWT.CHECK);
		btn.setText(label);
		btn.setData(key);
		btn.setSelection(getPreferenceStore().getBoolean(key));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(btn);

		parts.add(btn);
		return btn;
	}

	private Combo createLabeledCombo(Composite comp, String label, String trail, String[] styles, String key) {
		createLabel(comp, label);
		Combo combo = new Combo(comp, SWT.READ_ONLY);
		combo.setData(key);
		combo.setItems(styles);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).indent(9, 0).applyTo(combo);

		int idx = getPreferenceStore().getInt(key) - 1;
		idx = (idx >= 0 && idx < styles.length) ? idx : 0;
		combo.setText(styles[idx]);

		createLabel(comp, trail);

		parts.add(combo);
		return combo;
	}

	private Spinner createLabeledSpinner(Composite comp, String label, String trail, int min, int max, String key) {
		createLabel(comp, label);

		Spinner spin = new Spinner(comp, SWT.BORDER);
		spin.setData(key);
		spin.setMinimum(min);
		spin.setMaximum(max);
		spin.setSelection(getPreferenceStore().getInt(key));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).indent(9, 0).applyTo(spin);

		createLabel(comp, trail);

		parts.add(spin);
		return spin;
	}

	private void createLabeledColorEditor(Composite comp, String label, String trail, String key) {
		createLabel(comp, label);

		// required to constrain the layout expansiveness of the field editor
		Composite inner = new Composite(comp, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(inner);
		GridDataFactory.fillDefaults().applyTo(inner);

		ColorFieldEditor editor = new ColorFieldEditor(key, "", inner);
		editor.setPreferenceStore(getPreferenceStore());
		editor.load();

		createLabel(comp, trail);

		parts.add(editor);
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

			} else if (part instanceof CheckboxTreeViewer) {
				CheckboxTreeViewer viewer = (CheckboxTreeViewer) part;

				excludeTypes.clear();
				for (Object type : Utils.platformTextTypes()) {
					if (!viewer.getChecked(type)) {
						viewer.setChecked(type, true);
					}
				}
			}
		}

		super.performDefaults();
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

			} else if (part instanceof CheckboxTreeViewer) {
				CheckboxTreeViewer viewer = (CheckboxTreeViewer) part;
				excludeTypes = getUnChecked(viewer);
				store.setValue(Pref.CONTENT_TYPES, Utils.delimitTypes(excludeTypes));
			}
		}

		return super.performOk();
	}

	/**
	 * Returns the types of the checked, and optionally not grayed, elements in the tree viewer.
	 *
	 * @param viewer the tree viewer
	 * @param grayed include grayed if {@code true}; exclude grayed if {@code false}
	 * @return the checked, and optionally not grayed, types
	 */
	private Set<IContentType> getChecked(CheckboxTreeViewer viewer, boolean grayed) {
		Set<IContentType> checked = Arrays.stream(viewer.getCheckedElements()).map(e -> (IContentType) e)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (grayed) return checked;
		return checked.stream().filter(t -> !viewer.getGrayed(t)).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** Returns the types of the unchecked tree viewer items */
	private Set<IContentType> getUnChecked(CheckboxTreeViewer viewer) {
		return Utils.subtract(Utils.platformTextTypes(), getChecked(viewer, true));
	}

	private void updateCheckState(CheckboxTreeViewer viewer, IContentType type, boolean state) {
		viewer.setGrayed(type, false);

		// adjust child states to match the current item state
		TypesContentProvider provider = (TypesContentProvider) viewer.getContentProvider();
		for (Object child : provider.getChildren(type)) {
			viewer.setChecked(child, state);
		}

		// adj parent hierarchy states to reflect the current item state
		IContentType parent = (IContentType) provider.getParent(type);
		while (parent != null) {
			LinkedList<Object> children = new LinkedList<>(Arrays.asList(provider.getChildren(parent)));

			boolean all = children.stream().allMatch(e -> viewer.getChecked(e));
			boolean any = children.stream().anyMatch(e -> viewer.getChecked(e));

			viewer.setChecked(parent, all || any);
			viewer.setGrayed(parent, any && !all);

			parent = parent.getBaseType();
		}
	}

	private String colorKey() {
		String key = Pref.LINE_COLOR;
		if (Utils.isDarkTheme()) {
			key += Pref.DARK;
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

		@Override
		public Object[] getChildren(Object parent) {
			List<IContentType> elements = new ArrayList<>();
			IContentType base = (IContentType) parent;
			for (IContentType type : Utils.platformTextTypes()) {
				if (Objects.equals(type.getBaseType(), base)) {
					elements.add(type);
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
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}
}
