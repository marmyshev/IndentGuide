/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = Messages.class.getName();

	public static String Guide_description;
	public static String Guide_enabled_label;
	public static String Guide_attribute_group_label;
	public static String Guide_alpha_label;
	public static String Guide_style_label;
	public static String Guide_style_solid;
	public static String Guide_style_dash;
	public static String Guide_style_dot;
	public static String Guide_style_dash_dot;
	public static String Guide_style_dash_dot_dot;
	public static String Guide_width_label;
	public static String Guide_shift_label;
	public static String Guide_color_label;
	public static String Guide_drawing_group_label;
	public static String Guide_draw_left_end_label;
	public static String Guide_draw_blank_line_label;
	public static String Guide_skip_comment_block_label;
	public static String Guide_contenttype_group_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
