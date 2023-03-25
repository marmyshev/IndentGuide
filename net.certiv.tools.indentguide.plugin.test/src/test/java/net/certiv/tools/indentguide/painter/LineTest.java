package net.certiv.tools.indentguide.painter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import net.certiv.tools.indentguide.util.Utils;

class LineTest {

	private static final int TABWIDTH = 4;

	@ParameterizedTest
	@CsvFileSource(resources = "/line_base_data.csv", quoteCharacter = '\'')
	void testLine(String txt, int len, boolean blank, int cnt, String last, int beg) {
		Line ln = new Line(0, txt + Utils.EOL, TABWIDTH);

		assertEquals(ln.blank, blank, "Blank");
		assertEquals(ln.beg, beg, "Visual col");
		assertEquals(ln.length, len, "Length");
		assertEquals(ln.stops.size(), cnt, "Stop count");
		assertEquals(ln.stops.peekLast().toString(), last, "Last stop");
	}

	@ParameterizedTest
	@CsvFileSource(resources = "/line_cmt_data.csv", quoteCharacter = '\'')
	void testComment(String txt) {
		Line ln = new Line(0, txt + Utils.EOL, TABWIDTH);

		assertTrue(ln.comment, "Comment");
	}
}
