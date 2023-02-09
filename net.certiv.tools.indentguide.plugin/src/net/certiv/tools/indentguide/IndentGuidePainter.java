/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide;

import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;

import net.certiv.tools.indentguide.preferences.Settings;

/**
 * A painter for drawing visible indent guide lines.
 *
 * @see org.eclipse.jface.text.MarginPainter
 * @see org.eclipse.jface.text.WhitespaceCharacterPainter
 * @see org.eclipse.ui.texteditor.ShowWhitespaceCharactersAction
 */
public class IndentGuidePainter implements IPainter, PaintListener {

	private static final Pattern COMMENT_LEAD = Pattern.compile("^ \\*([ \\t].*|/.*|)$"); // $NON-NLS-1$
	private static final String SPACE = " "; //$NON-NLS-1$

	private boolean advanced; // advanced graphics subsystem
	private boolean active; // painter state
	private ITextViewer viewer; // containing source viewer
	private StyledText widget;
	private IPreferenceStore store;

	private int lineAlpha;
	private int lineStyle;
	private int lineWidth;
	private int lineShift;
	private boolean drawLeftEnd;
	private boolean drawBlankLine;
	private boolean skipCommentBlock;

	private final IPropertyChangeListener propertyWatcher = event -> {
		if (event.getProperty().startsWith(Settings.KEY)) {
			update();
			redrawAll();
		}
	};

	/**
	 * Creates a new painter for the given text viewer.
	 *
	 * @param viewer the text viewer the painter should be attached to
	 */
	public IndentGuidePainter(ITextViewer viewer) {
		this.viewer = viewer;
		widget = viewer.getTextWidget();
		GC gc = new GC(widget);
		gc.setAdvanced(true);
		advanced = gc.getAdvanced();
		gc.dispose();

		store = Activator.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(propertyWatcher);
		update();
	}

	@Override
	public void dispose() {
		store.removePropertyChangeListener(propertyWatcher);
		store = null;
		viewer = null;
		widget = null;
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
				IRegion lineRegion = doc.getLineInformationOfOffset(getDocumentOffset(widget.getCaretOffset()));
				int widgetOffset = getWidgetOffset(lineRegion.getOffset());
				int charCount = widget.getCharCount();
				int redrawLength = Math.min(lineRegion.getLength(), charCount - widgetOffset);
				if (widgetOffset >= 0 && redrawLength > 0) {
					widget.redrawRange(widgetOffset, redrawLength, true);
				}
			} catch (BadLocationException e) {}
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
	public void setPositionManager(IPaintPositionManager manager) {}

	@Override
	public void paintControl(PaintEvent event) {
		if (widget != null) {
			handleDrawRequest(event.gc, event.x, event.y, event.width, event.height);
		}
	}

	private void update() {
		lineAlpha = store.getInt(Settings.LINE_ALPHA);
		lineStyle = store.getInt(Settings.LINE_STYLE);
		lineWidth = store.getInt(Settings.LINE_WIDTH);
		lineShift = store.getInt(Settings.LINE_SHIFT);
		drawLeftEnd = store.getBoolean(Settings.DRAW_LEFT_END);
		drawBlankLine = store.getBoolean(Settings.DRAW_BLANK_LINE);
		skipCommentBlock = store.getBoolean(Settings.SKIP_COMMENT_BLOCK);
	}

	// Draw characters in view range.
	private void handleDrawRequest(GC gc, int x, int y, int w, int h) {
		int begLine = widget.getLineIndex(y);
		int endLine = widget.getLineIndex(y + h - 1);

		if (begLine <= endLine && begLine < widget.getLineCount()) {
			Color color = gc.getForeground(); // existing
			LineAttributes attributes = gc.getLineAttributes();
			gc.setForeground(Activator.getDefault().getColor());
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
		int spaceWidth = gc.stringExtent(SPACE).x - 1;

		StyledTextContent content = widget.getContent();

		for (int line = begLine; line <= endLine; line++) {
			int offset = widget.getOffsetAtLine(line);
			if (!isFoldedLine(content.getLineAtOffset(offset))) {
				String text = widget.getLine(line);
				int extend = 0;
				if (skipCommentBlock && assumeCommentBlock(text, tabWidth)) {
					extend -= tabWidth;
				}
				if (drawBlankLine && text.trim().length() == 0) {
					int prevLine = line;
					while (--prevLine >= 0) {
						text = widget.getLine(prevLine);
						if (text.trim().length() > 0) {
							int postLine = line;
							int lineCount = widget.getLineCount();
							while (++postLine < lineCount) {
								String tmp = widget.getLine(postLine);
								if (tmp.trim().length() > 0) {
									if (countSpaces(text, tabWidth) < countSpaces(tmp, tabWidth)) {
										extend += tabWidth;
									}
									break;
								}
							}
							break;
						}
					}
				}
				int count = countSpaces(text, tabWidth) + extend;
				for (int col = drawLeftEnd ? 0 : tabWidth; col < count; col += tabWidth) {
					draw(gc, offset, col, spaceWidth);
				}
			}
		}
	}

	private void draw(GC gc, int offset, int column, int spaceWidth) {
		Point pos = widget.getLocationAtOffset(offset);
		pos.x += column * spaceWidth + lineShift;
		gc.drawLine(pos.x, pos.y, pos.x, pos.y + widget.getLineHeight(offset));
	}

	private int countSpaces(String str, int tabs) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
			case ' ':
				count++;
				break;
			case '\t':
				int z = tabs - count % tabs;
				count += z;
				break;
			default:
				return count;
			}
		}
		return count;
	}

	private boolean assumeCommentBlock(String text, int tabs) {
		int count = countSpaces(text, tabs);
		count = (count / tabs) * tabs;
		int index = 0;
		for (int i = 0; i < count; i++) {
			switch (text.charAt(index)) {
			case ' ':
				index++;
				break;
			case '\t':
				index++;
				int z = tabs - i % tabs;
				i += z;
				break;
			default:
				i = count;
			}
		}
		text = text.substring(index);
		return COMMENT_LEAD.matcher(text).matches();
	}

	/**
	 * Check if the given widget line is a folded line.
	 *
	 * @param widgetLine the widget line number
	 * @return {@code true} if the line is folded
	 */
	private boolean isFoldedLine(int widgetLine) {
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 ext = (ITextViewerExtension5) viewer;
			int modelLine = ext.widgetLine2ModelLine(widgetLine);
			int widgetLine2 = ext.modelLine2WidgetLine(modelLine + 1);
			return widgetLine2 == -1;
		}
		return false;
	}

	/**
	 * Redraw all of the text widgets visible content.
	 */
	private void redrawAll() {
		widget.redraw();
	}

	/**
	 * Convert a document offset to the corresponding widget offset.
	 *
	 * @param docOffset the document offset
	 * @return widget offset
	 */
	private int getWidgetOffset(int docOffset) {
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
			return extension.modelOffset2WidgetOffset(docOffset);
		}

		IRegion visible = viewer.getVisibleRegion();
		int widgetOffset = docOffset - visible.getOffset();
		if (widgetOffset > visible.getLength()) return -1;
		return widgetOffset;
	}

	/**
	 * Convert a widget offset to the corresponding document offset.
	 *
	 * @param widgetOffset the widget offset
	 * @return document offset
	 */
	private int getDocumentOffset(int widgetOffset) {
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
			return extension.widgetOffset2ModelOffset(widgetOffset);
		}
		IRegion visible = viewer.getVisibleRegion();
		if (widgetOffset > visible.getLength()) return -1;
		return widgetOffset + visible.getOffset();
	}
}
