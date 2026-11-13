package de.lennard.dropall;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;

/** Gesperrte Slots nach Inventar-Index (0-8 Hotbar, 9-35 Inventar, 40 Zweithand). Gilt fuer alle Welten und Server. */
public final class Locks {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dropallplus-locks.txt");
	private static final Set<Integer> LOCKED = new TreeSet<>();

	static {
		try {
			if (Files.exists(FILE)) {
				for (String part : Files.readString(FILE).split("[,\\s]+")) {
					if (!part.isBlank()) {
						LOCKED.add(Integer.parseInt(part.trim()));
					}
				}
			}
		} catch (IOException | NumberFormatException e) {
			DropAllMod.LOG.warn("Gesperrte Slots konnten nicht gelesen werden", e);
		}
	}

	private Locks() {
	}

	public static boolean isLocked(int inventoryIndex) {
		return LOCKED.contains(inventoryIndex);
	}

	public static int count() {
		return LOCKED.size();
	}

	public static void toggle(int inventoryIndex) {
		if (!LOCKED.remove(inventoryIndex)) {
			LOCKED.add(inventoryIndex);
		}
		save();
	}

	public static void clear() {
		LOCKED.clear();
		save();
	}

	private static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, LOCKED.stream().map(String::valueOf).collect(Collectors.joining(",")));
		} catch (IOException e) {
			DropAllMod.LOG.warn("Gesperrte Slots konnten nicht gespeichert werden", e);
		}
	}
}
