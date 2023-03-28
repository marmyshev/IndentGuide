/******************************************************************************
 * Copyright (c) 2006-2023 The IndentGuide Authors.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License.  A copy of the MIT License is included this
 * distribution and is available at https://opensource.org/licenses/MIT.
 *****************************************************************************/
package net.certiv.tools.indentguide.painter;

import java.util.Objects;

public class Pos {

	public static final Pos P0 = Pos.at(0, 0, 0, 1);

	/** Stop index in line (0..n). */
	public final int idx;

	/** Char position in line (0..n); unexpanded. */
	public final int pos;
	/** Visual column in line (0..n); expanded. */
	public final int col;

	/** Location (x pixel offset) in widget line. */
	public final int loc;

	public static Pos at(int idx, int pos, int col, int loc) {
		return new Pos(idx, pos, col, loc);
	}

	private Pos(int idx, int pos, int col, int loc) {
		this.idx = idx;
		this.pos = pos;
		this.col = col;
		this.loc = loc;
	}

	@Override
	public int hashCode() {
		return Objects.hash(col, pos, loc);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Pos other = (Pos) obj;
		return col == other.col && pos == other.pos && loc == other.loc;
	}

	@Override
	public String toString() {
		return String.format("(%s,%s)", pos, col);
	}
}
