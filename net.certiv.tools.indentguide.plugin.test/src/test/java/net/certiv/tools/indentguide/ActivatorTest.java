package net.certiv.tools.indentguide;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ActivatorTest {

	@Test
	public void testActivatorId() {
		Assertions.assertNotNull(Activator.PLUGIN_ID);
	}

}
