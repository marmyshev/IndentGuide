/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.painter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;

import net.certiv.tools.indentguide.util.MsgBuilder;
import net.certiv.tools.indentguide.util.Utils;

public class Line implements Iterable<Pos> {

	private static final Pattern COMMENT = Pattern.compile( //
			"^(?:\\h*(?:" 					// $NON-NLS-1$
					+ "/\\*.*|"				// $NON-NLS-1$ -> ^'/*'.*$
					+ " \\*|"				// $NON-NLS-1$ -> ^' *'$
					+ " \\* .*|"			// $NON-NLS-1$ -> ^' * '.*$
					+ " \\*/.*|"			// $NON-NLS-1$ -> ^' */'.*$
					+ " (?:\\*.*)?\\*/" 	// $NON-NLS-1$ -> ^' *'.*'*/'.*$
					+ "))$" 				// $NON-NLS-1$
	);

	/** The control */
	public final StyledText widget;
	/** The line number (0..n) */
	public final int line;
	/** The line text */
	public final String txt;
	/** The assigned tab width */
	public final int tabwidth;

	/** Is a blank line */
	public final boolean blank;
	/** The raw line length */
	public final int length;
	/** Is a block comment body line */
	public final boolean comment;

	/** List of the lead tab stops */
	public final LinkedList<Pos> stops = new LinkedList<>();

	/** First visible text column */
	public final int beg;

	/** Change in indents between non-blank lines surrounding this line. */
	public int delta;

	public static Line of(StyledText widget, int line, int tabwidth) {
		return new Line(widget, line, tabwidth);
	}

	/**
	 * Creates a new Line.
	 *
	 * @param widget
	 * @param line     the line number (0..n) within the widget
	 * @param tabwidth the document defined tabwidth
	 */
	public Line(StyledText widget, int line, int tabwidth) {
		this.widget = widget;
		this.line = line;
		this.txt = widget != null ? widget.getLine(line) : Utils.EMPTY;
		this.tabwidth = tabwidth;

		blank = this.txt.isBlank();
		length = this.txt.length();

		stops.add(Pos.P0);
		beg = process();
		comment = inBlockComment();
	}

	private int process() {
		int beg = 0;
		for (int pos = 0, col = 0; pos < length; pos++) {
			int ch = txt.codePointAt(pos);
			switch (ch) {
				case Utils.SPC:
					beg = col += Character.charCount(ch);
					if (col % tabwidth == 0) stops.add(pos(stops.size(), pos + 1, col));
					break;

				case Utils.TAB:
					beg = col += tabwidth - (col % tabwidth);
					stops.add(pos(stops.size(), pos + 1, col));
					break;

				default:
					beg = col;
					return beg;
			}
		}
		return beg;
	}

	private Pos pos(int idx, int pos, int col) {
		int offset = widget.getOffsetAtLine(line);
		Point loc = widget.getLocationAtOffset(offset + pos);
		return Pos.at(idx, pos, col, loc.x);
	}

	private boolean inBlockComment() {
		if (blank) return false;

		int pos = stops.peekLast().pos;
		String rem = txt.substring(pos);
		return COMMENT.matcher(rem).matches();
	}

	/**
	 * Return the number of tab stops
	 *
	 * @return tab stop count
	 */
	public int tabs() {
		return stops.size();
	}

	/**
	 * Return {@code true} if the number of tab stops equals the given {@code cnt}.
	 *
	 * @return {@code true} if the tab stop count equals {@code cnt}.
	 */
	public boolean tabs(int cnt) {
		return stops.size() == cnt;
	}

	/**
	 * Return the column of the last stop.
	 *
	 * @return the last stop column
	 */
	public int endStop() {
		return !stops.isEmpty() ? stops.peekLast().col : 0;
	}

	@Override
	public Iterator<Pos> iterator() {
		return stops.iterator();
	}

	@Override
	public String toString() {
		MsgBuilder mb = new MsgBuilder() //
				.append("Line %s:", line) //
				.append(" %s", stops) //
				.append(blank, " %s", "Blank") //
				.append(!blank, " %s[%s]", beg, length) //
				.append("\t %s", Utils.encode(txt));
		return mb.toString();
	}
}
