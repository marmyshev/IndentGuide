package net.certiv.tools.indentguide.painter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.junit.jupiter.api.Test;

class IndentWidthTest extends TestBase {

	@Test
	void test_stringExtent() {
		for (int size = 7; size < 15; size++) {
			for (String name : fontnames) {
				FontData fd = new FontData(name, size, SWT.NORMAL);
				Font font = new Font(widget.getDisplay(), fd);
				widget.setFont(font);
				gc.setFont(font);

				Map<Integer, Integer> widths0 = new LinkedHashMap<>();
				Map<Integer, Integer> widths1 = new LinkedHashMap<>();
				for (int col = 1; col <= 10; col++) {
					Point p = gc.stringExtent(" ");
					widths0.put(col, p.x * col);

					p = gc.stringExtent(" ".repeat(col));
					widths1.put(col, p.x);
				}
				System.out.printf("Font %-20s: size [%02d] multiply widths %s\n", name, size, widths0);
				System.out.printf("Font %-20s: size [%02d] *repeat* widths %s\n", name, size, widths1);

				gc.setFont(null);
				widget.setFont(null);
				font.dispose();

				assertTrue(widths0.equals(widths1), String.format("Font %s did not match.", name));
			}
		}
	}
}
