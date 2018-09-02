/*******************************************************************************
 * Copyright (c) 2006, 2009 Wind River Systems, Inc., IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anton Leherbauer (Wind River Systems) - initial API and implementation - https://bugs.eclipse.org/bugs/show_bug.cgi?id=22712
 *     Anton Leherbauer (Wind River Systems) - [painting] Long lines take too long to display when "Show Whitespace Characters" is enabled - https://bugs.eclipse.org/bugs/show_bug.cgi?id=196116
 *     Anton Leherbauer (Wind River Systems) - [painting] Whitespace characters not drawn when scrolling to right slowly - https://bugs.eclipse.org/bugs/show_bug.cgi?id=206633
 *     Tom Eicher (Avaloq Evolution AG) - block selection mode
 *******************************************************************************/
package net.certiv.tools.indentguide;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
 * @see org.eclipse.jface.text.WhitespaceCharacterPainter
 */
public class IndentGuidePainter implements IPainter, PaintListener {

	private boolean advanced; // is advanced graphics subsystem available
	private boolean active;	// is painter active
	private ITextViewer textViewer; // source viewer for this painter
	private StyledText textWidget;
	private IPreferenceStore store;

	private int lineAlpha;
	private int lineStyle;
	private int lineWidth;
	private int lineShift;
	private int spaceWidth;
	private boolean drawLeftEnd;
	private boolean drawBlankLine;
	private boolean skipCommentBlock;

	private final IPropertyChangeListener propertyWatcher = new IPropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().startsWith(Settings.KEY)) {
				update();
				redrawAll();
			}
		}
	};

	/**
	 * Creates a new painter for the given text viewer.
	 *
	 * @param textViewer the text viewer the painter should be attached to
	 */
	public IndentGuidePainter(ITextViewer textViewer) {
		super();
		this.textViewer = textViewer;
		this.textWidget = textViewer.getTextWidget();
		GC gc = new GC(textWidget);
		gc.setAdvanced(true);
		this.advanced = gc.getAdvanced();
		gc.dispose();

		store = Activator.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(propertyWatcher);
		update();
	}

	@Override
	public void dispose() {
		store.removePropertyChangeListener(propertyWatcher);
		store = null;
		textViewer = null;
		textWidget = null;
	}

	@Override
	public void paint(int reason) {
		IDocument document = textViewer.getDocument();
		if (document == null) {
			deactivate(false);
			return;
		}
		if (!active) {
			active = true;
			textWidget.addPaintListener(this);
			redrawAll();

		} else if (reason == CONFIGURATION || reason == INTERNAL) {
			redrawAll();

		} else if (reason == TEXT_CHANGE) { // redraw current line only
			try {
				IRegion lineRegion = document
						.getLineInformationOfOffset(getDocumentOffset(textWidget.getCaretOffset()));
				int widgetOffset = getWidgetOffset(lineRegion.getOffset());
				int charCount = textWidget.getCharCount();
				int redrawLength = Math.min(lineRegion.getLength(), charCount - widgetOffset);
				if (widgetOffset >= 0 && redrawLength > 0) {
					textWidget.redrawRange(widgetOffset, redrawLength, true);
				}
			} catch (BadLocationException e) {}
		}
	}

	@Override
	public void deactivate(boolean redraw) {
		if (active) {
			active = false;
			textWidget.removePaintListener(this);
			if (redraw) redrawAll();
		}
	}

	@Override
	public void setPositionManager(IPaintPositionManager manager) {}

	@Override
	public void paintControl(PaintEvent event) {
		if (textWidget != null) {
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
		int startLine = textWidget.getLineIndex(y);
		int endLine = textWidget.getLineIndex(y + h - 1);
		if (startLine <= endLine && startLine < textWidget.getLineCount()) {
			Color fgColor = gc.getForeground();
			LineAttributes lineAttributes = gc.getLineAttributes();
			gc.setForeground(Activator.getDefault().getColor());
			gc.setLineStyle(lineStyle);
			gc.setLineWidth(lineWidth);
			spaceWidth = gc.getAdvanceWidth(' ');
			if (advanced) {
				int alpha = gc.getAlpha();
				gc.setAlpha(this.lineAlpha);
				drawLineRange(gc, startLine, endLine, x, w);
				gc.setAlpha(alpha);
			} else {
				drawLineRange(gc, startLine, endLine, x, w);
			}
			gc.setForeground(fgColor);
			gc.setLineAttributes(lineAttributes);
		}
	}

	/**
	 * Draw the given line range.
	 *
	 * @param gc the GC
	 * @param startLine first line number
	 * @param endLine last line number (inclusive)
	 * @param x the X-coordinate of the drawing range
	 * @param w the width of the drawing range
	 */
	private void drawLineRange(GC gc, int startLine, int endLine, int x, int w) {
		int tabs = textWidget.getTabs();

		StyledTextContent content = textWidget.getContent();
		for (int line = startLine; line <= endLine; line++) {
			int widgetOffset = textWidget.getOffsetAtLine(line);
			if (!isFoldedLine(content.getLineAtOffset(widgetOffset))) {
				String text = textWidget.getLine(line);
				int extend = 0;
				if (skipCommentBlock && assumeCommentBlock(text, tabs)) {
					extend -= tabs;
				}
				if (drawBlankLine && text.trim().length() == 0) {
					int prevLine = line;
					while (--prevLine >= 0) {
						text = textWidget.getLine(prevLine);
						if (text.trim().length() > 0) {
							int postLine = line;
							int lineCount = textWidget.getLineCount();
							while (++postLine < lineCount) {
								String tmp = textWidget.getLine(postLine);
								if (tmp.trim().length() > 0) {
									if (countSpaces(text, tabs) < countSpaces(tmp, tabs)) {
										extend += tabs;
									}
									break;
								}
							}
							break;
						}
					}
				}
				int count = countSpaces(text, tabs) + extend;
				for (int i = drawLeftEnd ? 0 : tabs; i < count; i += tabs) {
					draw(gc, widgetOffset, i);
				}
			}
		}
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
		if (text.matches("^ \\*([ \\t].*|/.*|)$")) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the given widget line is a folded line.
	 *
	 * @param widgetLine the widget line number
	 * @return <code>true</code> if the line is folded
	 */
	private boolean isFoldedLine(int widgetLine) {
		if (textViewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) textViewer;
			int modelLine = extension.widgetLine2ModelLine(widgetLine);
			int widgetLine2 = extension.modelLine2WidgetLine(modelLine + 1);
			return widgetLine2 == -1;
		}
		return false;
	}

	/**
	 * Redraw all of the text widgets visible content.
	 */
	private void redrawAll() {
		textWidget.redraw();
	}

	/**
	 * @param gc
	 * @param offset
	 * @param column
	 */
	private void draw(GC gc, int offset, int column) {
		Point pos = textWidget.getLocationAtOffset(offset);
		pos.x += column * spaceWidth + lineShift;
		gc.drawLine(pos.x, pos.y, pos.x, pos.y + textWidget.getLineHeight(offset));
	}

	/**
	 * Convert a document offset to the corresponding widget offset.
	 *
	 * @param documentOffset the document offset
	 * @return widget offset
	 */
	private int getWidgetOffset(int documentOffset) {
		if (textViewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) textViewer;
			return extension.modelOffset2WidgetOffset(documentOffset);
		}
		IRegion visible = textViewer.getVisibleRegion();
		int widgetOffset = documentOffset - visible.getOffset();
		if (widgetOffset > visible.getLength()) {
			return -1;
		}
		return widgetOffset;
	}

	/**
	 * Convert a widget offset to the corresponding document offset.
	 *
	 * @param widgetOffset the widget offset
	 * @return document offset
	 */
	private int getDocumentOffset(int widgetOffset) {
		if (textViewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) textViewer;
			return extension.widgetOffset2ModelOffset(widgetOffset);
		}
		IRegion visible = textViewer.getVisibleRegion();
		if (widgetOffset > visible.getLength()) {
			return -1;
		}
		return widgetOffset + visible.getOffset();
	}
}
