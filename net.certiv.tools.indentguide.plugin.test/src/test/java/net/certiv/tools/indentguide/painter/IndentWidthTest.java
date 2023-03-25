package net.certiv.tools.indentguide.painter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndentWidthTest {

	public final static boolean isWin = SWT.getPlatform().startsWith("win32");
	public final static boolean isCocoa = SWT.getPlatform().startsWith("cocoa");
	public final static boolean isGTK = SWT.getPlatform().equals("gtk");
	public final static boolean isWinOS = System.getProperty("os.name").startsWith("Windows");
	public final static boolean isLinux = System.getProperty("os.name").equals("Linux");

	String[] fontnames = new String[] { //
			"Consolas", "Courier New", "Menlo", //
			"Fira Code", "Source Code Pro", "Liberation Mono" //
	};

	Display display;
	Shell shell;
	GC gc;

	StyledText widget;

	@BeforeEach
	public void setUp() {
		// display = new Display();
		shell = new Shell(display);
		gc = new GC(shell);

		shell.setSize(200, 200);
		shell.setLayout(new FillLayout());

		widget = new StyledText(shell, SWT.BORDER);
		widget.setText("This is some dummy text. Sufficient text to have the scroll bars appear.");
	}

	@AfterEach
	public void tearDown() {
		if (gc != null) gc.dispose();
		if (shell != null) shell.dispose();
	}

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
