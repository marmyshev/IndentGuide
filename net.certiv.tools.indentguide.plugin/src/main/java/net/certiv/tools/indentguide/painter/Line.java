package net.certiv.tools.indentguide.painter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import net.certiv.tools.indentguide.util.MsgBuilder;
import net.certiv.tools.indentguide.util.Utils;

public class Line implements Iterable<Pos> {

	private static final Pattern COMMENT = Pattern.compile("^(\\h*/\\*.*|\\h*\\*(\\h.*|/.*|))$"); // $NON-NLS-1$

	/** The line number (0..n) */
	public final int num;
	/** The line text */
	public final String txt;
	/** The assigned tabwidth */
	public final int tabwidth;

	/** Is a blank line */
	public final boolean blank;
	/** The raw line length */
	public final int length;
	/** Is a block comment body line */
	public final boolean comment;

	/** List of the leading tab stops */
	public final LinkedList<Pos> stops = new LinkedList<>();

	/** First visible text column */
	public final int beg;

	/** Change in indents between non-blank lines surrounding this line. */
	public int delta;

	/**
	 * Creates a new Line.
	 *
	 * @param num      the line number (0..n) within the document
	 * @param txt      the line raw text content
	 * @param tabwidth the document defined tabwidth
	 */
	public Line(int num, String txt, int tabwidth) {
		this.num = num;
		this.txt = stripTerminals(txt);
		this.tabwidth = tabwidth;

		blank = this.txt.isBlank();
		length = this.txt.length();

		stops.add(Pos.P0);
		beg = process();
		comment = inBlockComment();
	}

	private String stripTerminals(String txt) {
		if (txt == null) return Utils.EMPTY;
		int dot = txt.indexOf(Utils.EOL);
		if (dot > -1) {
			txt = txt.substring(0, dot);
		}
		if (txt.isBlank()) return txt;
		return txt.stripTrailing();
	}

	private int process() {
		int beg = 0;
		for (int pos = 0, col = 0; pos < length; pos++) {
			int ch = txt.codePointAt(pos);
			switch (ch) {
				case Utils.SPC:
					beg = col += Character.charCount(ch);
					if (col % tabwidth == 0) stops.add(Pos.at(pos + 1, col));
					break;

				case Utils.TAB:
					beg = col += tabwidth - (col % tabwidth);
					stops.add(Pos.at(pos + 1, col));
					break;

				default:
					beg = col;
					return beg;
			}
		}
		return beg;
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
		MsgBuilder mb = new MsgBuilder("Line %s: \"%s\" ", num + 1, Utils.encode(txt)) //
				.append(!stops.isEmpty(), "%s:%s ", stops.size(), stops) //
				.append(blank, "%s ", "Blank") //
				.append(!blank, "len=%s beg=%s", length, beg);
		return mb.toString();
	}
}
