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

	public static String description;
	public static String enabled_label;
	public static String attribute_group_label;
	public static String alpha_label1;
	public static String alpha_label2;
	public static String width_label1;
	public static String width_label2;
	public static String shift_label1;
	public static String shift_label2;
	public static String color_label1;
	public static String color_label2;
	public static String style_label1;
	public static String style_label2;
	public static String style_solid;
	public static String style_dash;
	public static String style_dot;
	public static String style_dash_dot;
	public static String style_dash_dot_dot;
	public static String drawing_group_label;
	public static String draw_start_edge_label;
	public static String draw_blank_line_label;
	public static String draw_comment_block_label;
	public static String contenttype_group_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
