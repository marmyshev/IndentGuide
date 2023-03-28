/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.painter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;

import net.certiv.tools.indentguide.Activator;
import net.certiv.tools.indentguide.preferences.Pref;
import net.certiv.tools.indentguide.util.MsgBuilder;
import net.certiv.tools.indentguide.util.Utils;

/**
 * A painter for drawing visible indent guide lines.
 *
 * @see org.eclipse.jface.text.WhitespaceCharacterPainter
 * @see org.eclipse.ui.texteditor.ShowWhitespaceCharactersAction
 */
public class GuidePainter implements IPainter, PaintListener {

	private ITextViewer viewer;
	private StyledText widget;

	private boolean advanced;
	private IPreferenceStore store;

	private boolean active;
	private int lineAlpha;
	private int lineStyle;
	private int lineWidth;
	private int lineShift;
	private Color lineColor;
	private boolean drawLeadEdge;
	private boolean drawBlankLn;
	private boolean drawComment;

	private Line prevNb; // prior non-blank line
	private Line currLn; // current line
	private Line nextNb; // next non-blank line

	/**
	 * Creates a new painter for the given text viewer.
	 *
	 * @param viewer the text viewer the painter should be attached to
	 */
	public GuidePainter(ITextViewer viewer) {
		this.viewer = viewer;
		widget = viewer.getTextWidget();
		advanced = Utils.setAdvanced(widget);
		store = Activator.getDefault().getPreferenceStore();

		loadPrefs();
	}

	@Override
	public void paint(int reason) {
		IDocument doc = viewer.getDocument();
		if (doc == null) {
			deactivate(false);
			return;
		}

		if (!active) {
			active = true;
			widget.addPaintListener(this);
			redrawAll();

		} else if (reason == CONFIGURATION || reason == INTERNAL) {
			redrawAll();

		} else if (reason == TEXT_CHANGE) { // redraw current line only
			try {
				IRegion region = doc
						.getLineInformationOfOffset(Utils.docOffset(viewer, widget.getCaretOffset()));
				int offset = Utils.widgetOffset(viewer, region.getOffset());
				int cnt = widget.getCharCount();
				int len = Math.min(region.getLength(), cnt - offset);
				if (offset >= 0 && len > 0) {
					widget.redrawRange(offset, len, true);
				}
			} catch (BadLocationException e) {}
		}
	}

	/** Request a redraw of all visible content. */
	public void redrawAll() {
		widget.redraw();
	}

	@Override
	public void paintControl(PaintEvent evt) {
		if (widget != null) {
			handleDrawRequest(evt.gc, evt.x, evt.y, evt.width, evt.height);
		}
	}

	// Draw characters in view range.
	private void handleDrawRequest(GC gc, int x, int y, int w, int h) {
		int begLine = widget.getLineIndex(y);
		int endLine = widget.getLineIndex(y + h - 1);

		// Activator.log("draw request @(%s:%s)", begLine + 1, endLine + 1);

		if (begLine <= endLine && begLine < widget.getLineCount()) {
			Color color = gc.getForeground();
			LineAttributes attributes = gc.getLineAttributes();

			gc.setForeground(lineColor);
			gc.setLineStyle(lineStyle);
			gc.setLineWidth(lineWidth);
			if (advanced) {
				int alpha = gc.getAlpha();
				gc.setAlpha(lineAlpha);
				drawLineRange(gc, begLine, endLine, x, w);
				gc.setAlpha(alpha);
			} else {
				drawLineRange(gc, begLine, endLine, x, w);
			}

			gc.setForeground(color);
			gc.setLineAttributes(attributes);
		}
	}

	/**
	 * Draw the given line range.
	 *
	 * @param gc      the GC
	 * @param begLine first line number
	 * @param endLine last line number (inclusive)
	 * @param x       the X-coordinate of the drawing range
	 * @param w       the width of the drawing range
	 */
	private void drawLineRange(GC gc, int begLine, int endLine, int x, int w) {
		int tabWidth = widget.getTabs();
		StyledTextContent content = widget.getContent();

		prevNb = null;
		currLn = null;
		nextNb = null;

		for (int line = begLine; line <= endLine; line++) {
			int offset = widget.getOffsetAtLine(line);
			int height = widget.getLineHeight(offset);
			int spacing = widget.getLineSpacing();

			int docLine = content.getLineAtOffset(offset); // 0..n

			if (!Utils.isFolded(viewer, docLine)) {
				prevNb = prevNonblankLine(line, tabWidth);
				currLn = new Line(widget, line, tabWidth);

				if (drawBlankLn) {
					if (currLn.blank) {
						nextNb = nextNonblankLine(line, tabWidth);

						// change in dents: - <-> +
						currLn.delta = nextNb.tabs() - prevNb.tabs();

						// capture prevLn stop locations
						currLn.stops.clear();
						currLn.stops.addAll(prevNb.stops);

						log(currLn.delta, prevNb, currLn, nextNb);

						// adjust stops dependent on delta
						if (currLn.delta < 0 && currLn.tabs() > 1) {
							currLn.stops.removeLast(); // shift in by one
						}
					}
				}

				boolean only = currLn.tabs(1);
				boolean multi = currLn.tabs() > 1;
				boolean zero = currLn.delta == 0;

				for (Pos stop : currLn.stops) {
					boolean first = stop == currLn.stops.peekFirst();
					boolean last = stop == currLn.stops.peekLast();

					if (currLn.comment) {
						// skip first visible character
						if (stop.col == currLn.beg) continue;

						// skip first where only unless drawComment or drawLeadEdge
						if (only && !(drawComment || drawLeadEdge)) continue;

						// skip first where not only unless drawLeadEdge
						if (first && !only && !drawLeadEdge) continue;

						// skip last where !only unless drawComment
						if (last && !only && !drawComment) continue;

					} else if (currLn.blank) {
						// skip first where only and zero
						if (first && only && zero) continue;

						// skip last where not only and zero
						if (last && !only && zero) continue;

						// skip first where not zero unless drawBlankLn and drawLeadEdge
						if (first && !zero && !(drawBlankLn && drawLeadEdge)) continue;

						// skip first where zero and multi unless drawBlankLn and
						// drawLeadEdge
						if (first && zero && multi && !(drawBlankLn && drawLeadEdge)) continue;

					} else {
						// skip first visible character
						if (stop.col == currLn.beg) continue;

						// skip first unless drawLeadEdge
						if (first && !drawLeadEdge) continue;
					}

					boolean asc = stop.col >= prevNb.endStop();
					Point pos = widget.getLocationAtOffset(offset);
					draw(gc, pos, stop.loc, spacing, height, asc);
				}
			}
		}
	}

	private void draw(GC gc, Point pos, int loc, int sp, int ht, boolean asc) {
		pos.x += loc + lineShift;
		if (asc) {
			gc.drawLine(pos.x, pos.y - sp, pos.x, pos.y + ht + sp);
		} else {
			gc.drawLine(pos.x, pos.y, pos.x, pos.y + ht + sp);
		}
	}

	private Line prevNonblankLine(int line, int tabWidth) {
		if (currLn != null && !currLn.blank) return currLn;
		if (prevNb != null && prevNb.line < line) return prevNb;

		for (int prev = line - 1; prev >= 0; prev--) {
			String text = widget.getLine(prev);
			if (!text.isBlank()) {
				return new Line(widget, prev, tabWidth);
			}
		}
		return new Line(null, -1, tabWidth);
	}

	private Line nextNonblankLine(int line, int tabWidth) {
		int end = widget.getLineCount();
		if (nextNb != null && nextNb.line < end && nextNb.line > line) return nextNb;

		for (int next = line + 1; next < end; next++) {
			String text = widget.getLine(next);
			if (!text.isBlank()) {
				return new Line(widget, next, tabWidth);
			}
		}
		return new Line(null, end, tabWidth);
	}

	public void loadPrefs() {
		lineAlpha = store.getInt(Pref.LINE_ALPHA);
		lineStyle = store.getInt(Pref.LINE_STYLE);
		lineWidth = store.getInt(Pref.LINE_WIDTH);
		lineShift = store.getInt(Pref.LINE_SHIFT);

		disposeLineColor();
		lineColor = Utils.getColor(store);

		drawLeadEdge = store.getBoolean(Pref.DRAW_LEAD_EDGE);
		drawBlankLn = store.getBoolean(Pref.DRAW_BLANK_LINE);
		drawComment = store.getBoolean(Pref.DRAW_COMMENT_BLOCK);
	}

	public boolean isActive() {
		return active;
	}

	public void activate(boolean redraw) {
		if (!active) {
			active = true;
			widget.addPaintListener(this);
			if (redraw) redrawAll();
		}
	}

	@Override
	public void deactivate(boolean redraw) {
		if (active) {
			active = false;
			widget.removePaintListener(this);
			if (redraw) redrawAll();
		}
	}

	@Override
	public void dispose() {
		store = null;
		viewer = null;
		widget = null;

		disposeLineColor();
	}

	private void disposeLineColor() {
		if (lineColor != null) {
			lineColor.dispose();
			lineColor = null;
		}
	}

	@Override
	public void setPositionManager(IPaintPositionManager manager) {}

	void log(int delta, Line prevNb, Line currLn, Line nextNb) {
		Activator.log(new MsgBuilder() //
				.nl().append("Delta: %s", delta) //
				.nl().append("PrevNb: %s", prevNb) //
				.nl().append("CurrLn: %s", currLn) //
				.nl().append("NextNb: %s", nextNb)) //
		;
	}
}
