package net.certiv.tools.indentguide.painter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class LineTest extends TestBase {

	private static final int TABWIDTH = 4;

	@ParameterizedTest
	@CsvFileSource(resources = "/line_base_data.csv", quoteCharacter = '\'')
	void testLine(String txt, int len, boolean blank, int cnt, String last, int beg) {

		int size = 11;
		String name = fontnames[3];
		FontData fd = new FontData(name, size, SWT.NORMAL);
		Font font = new Font(widget.getDisplay(), fd);

		gc.setFont(font);
		widget.setFont(font);
		widget.setTabs(TABWIDTH);
		widget.setText(txt);

		Line ln = new Line(widget, 0, TABWIDTH);

		assertEquals(ln.blank, blank, "Blank");
		assertEquals(ln.beg, beg, "Visual col");
		assertEquals(ln.length, len, "Length");
		assertEquals(ln.stops.size(), cnt, "Stop count");
		// assertEquals(ln.stops.peekLast().toString(), last, "Last stop");
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/line_cmt_data.csv", quoteCharacter = '\'')
	void testComment(String txt) {

		int size = 11;
		String name = fontnames[3];
		FontData fd = new FontData(name, size, SWT.NORMAL);
		Font font = new Font(widget.getDisplay(), fd);

		gc.setFont(font);
		widget.setFont(font);
		widget.setTabs(TABWIDTH);
		widget.setText(txt);

		Line ln = new Line(widget, 0, TABWIDTH);

		assertTrue(ln.comment, "Comment");
	}
}
