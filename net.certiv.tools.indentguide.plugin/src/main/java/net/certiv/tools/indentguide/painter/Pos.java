package net.certiv.tools.indentguide.painter;

import java.util.Objects;

public class Pos {

	public static final Pos NIL = Pos.at(-1, -1);
	public static final Pos P0 = Pos.at(0, 0);

	/** Position in line (0..n); unexpanded. */
	public final int pos;
	/** Visual column in line (0..n); expanded. */
	public final int col;
	/** Position is valid ({@code true}) if >= 0 */
	public final boolean valid;

	public static Pos at(int pos, int col) {
		return new Pos(pos, col);
	}

	public Pos(int pos, int col) {
		this.pos = pos;
		this.col = col;
		valid = pos > -1 & col > -1;
	}

	@Override
	public int hashCode() {
		return Objects.hash(col, pos, valid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Pos other = (Pos) obj;
		return col == other.col && pos == other.pos && valid == other.valid;
	}

	@Override
	public String toString() {
		return "(" + pos + "," + col + ")";
	}
}
