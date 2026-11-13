package de.lennard.dropall;

import net.minecraft.client.gui.components.Button;

/** Zugriff auf die Knoepfe, die die Mixin an jedem Container-Bildschirm haelt. */
public interface ButtonHolder {
	void dropall$setButtons(Button drop, Button menu);
}
