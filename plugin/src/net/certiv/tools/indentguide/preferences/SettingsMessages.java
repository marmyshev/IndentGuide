package net.certiv.tools.indentguide.preferences;

import org.eclipse.osgi.util.NLS;

public class SettingsMessages extends NLS {

	private static final String BUNDLE_NAME = SettingsMessages.class.getName();

	public static String Settings_description;
	public static String Settings_enabled_label;
	public static String Settings_group_label;
	public static String Settings_alpha_label;
	public static String Settings_style_label;
	public static String Settings_style_solid;
	public static String Settings_style_dash;
	public static String Settings_style_dot;
	public static String Settings_style_dash_dot;
	public static String Settings_style_dash_dot_dot;
	public static String Settings_width_label;
	public static String Settings_shift_label;
	public static String Settings_color_label;
	public static String Settings_group2_label;
	public static String Settings_draw_left_end_label;
	public static String Settings_draw_blank_line_label;
	public static String Settings_skip_comment_block_label;
	public static String Settings_group3_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, SettingsMessages.class);
	}

	private SettingsMessages() {}
}
