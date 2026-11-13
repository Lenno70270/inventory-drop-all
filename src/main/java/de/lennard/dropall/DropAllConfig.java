package de.lennard.dropall;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Einstellungen in config/dropallplus.json. Gesperrte Slots liegen getrennt in {@link Locks}. */
public final class DropAllConfig {
	public static final int POSITION_BELOW = 0;
	public static final int POSITION_RIGHT = 1;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dropallplus.json");

	public boolean showButton = true;
	public int buttonPosition = POSITION_BELOW;
	public boolean dropHotbar = true;
	public boolean dropInventory = true;
	public boolean dropOffhand = true;
	public boolean dropArmor = false;
	public boolean hotkeyConfirm = false;

	public void resetToDefaults() {
		DropAllConfig defaults = new DropAllConfig();
		showButton = defaults.showButton;
		buttonPosition = defaults.buttonPosition;
		dropHotbar = defaults.dropHotbar;
		dropInventory = defaults.dropInventory;
		dropOffhand = defaults.dropOffhand;
		dropArmor = defaults.dropArmor;
		hotkeyConfirm = defaults.hotkeyConfirm;
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(this));
		} catch (IOException e) {
			DropAllMod.LOG.warn("config/dropallplus.json nicht speicherbar", e);
		}
	}

	public static DropAllConfig load() {
		try {
			if (Files.exists(FILE)) {
				DropAllConfig config = GSON.fromJson(Files.readString(FILE), DropAllConfig.class);
				if (config != null) {
					return config;
				}
			}
		} catch (IOException | RuntimeException e) {
			DropAllMod.LOG.warn("config/dropallplus.json nicht lesbar, nehme Standard", e);
		}
		DropAllConfig config = new DropAllConfig();
		config.save();
		return config;
	}
}
