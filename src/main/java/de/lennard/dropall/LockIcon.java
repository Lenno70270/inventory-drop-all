package de.lennard.dropall;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Kleines Schloss oben links in einem 16x16-Slot, 7x8 Pixel. */
public final class LockIcon {
	private static final int DARK = 0xFF3B2A0A;
	private static final int GOLD = 0xFFF2BE2E;
	private static final int STEEL = 0xFFB9B9B9;

	private LockIcon() {
	}

	public static void draw(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x + 1, y, x + 6, y + 4, DARK);
		g.fill(x + 2, y + 1, x + 5, y + 4, STEEL);
		g.fill(x + 3, y + 2, x + 4, y + 4, DARK);
		g.fill(x, y + 3, x + 7, y + 8, DARK);
		g.fill(x + 1, y + 4, x + 6, y + 7, GOLD);
		g.fill(x + 3, y + 5, x + 4, y + 7, DARK);
	}
}
