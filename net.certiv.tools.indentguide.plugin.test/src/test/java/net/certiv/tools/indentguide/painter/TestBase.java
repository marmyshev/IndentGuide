package net.certiv.tools.indentguide.painter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class TestBase {

	public static final boolean isWin = SWT.getPlatform().startsWith("win32");
	public static final boolean isCocoa = SWT.getPlatform().startsWith("cocoa");
	public static final boolean isGTK = SWT.getPlatform().equals("gtk");
	public static final boolean isWinOS = System.getProperty("os.name").startsWith("Windows");
	public static final boolean isLinux = System.getProperty("os.name").equals("Linux");

	protected String[] fontnames = new String[] { //
			"Consolas", "Courier New", "Menlo", //
			"Fira Code", "Source Code Pro", "Liberation Mono" //
	};

	protected Display display;
	protected Shell shell;
	protected GC gc;
	protected StyledText widget;

	@BeforeEach
	protected void setUp() {
		// display = new Display();
		shell = new Shell(display);
		gc = new GC(shell);

		shell.setSize(200, 200);
		shell.setLayout(new FillLayout());

		widget = new StyledText(shell, SWT.BORDER);
		widget.setText("This is some dummy text. Sufficient text to have the scroll bars appear.");
	}

	@AfterEach
	protected void tearDown() {
		if (gc != null) gc.dispose();
		if (shell != null) shell.dispose();
	}

}
