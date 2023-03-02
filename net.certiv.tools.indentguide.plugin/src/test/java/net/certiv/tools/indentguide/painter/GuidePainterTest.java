package net.certiv.tools.indentguide.painter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class GuidePainterTest {

	@ParameterizedTest
	@CsvFileSource(resources = "/line_blank_logic_data.csv", numLinesToSkip = 1)
	void testDrawLogic(int idx, boolean draw, boolean comment, boolean blank, boolean drawComment, boolean drawBlankLn,
			boolean drawLeadEdge, boolean first, boolean only) {

		assertEquals(!draw, blank && !(drawBlankLn && (!first || drawLeadEdge && first && !only)));
	}
}
