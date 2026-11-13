package de.lennard.dropall;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/** Wirft alles aus dem eigenen Inventar, was laut Einstellung dazugehoert und nicht gesperrt ist. */
public final class DropAll {
	public static final int HOTBAR_END = 9;
	public static final int ARMOR_START = Inventory.INVENTORY_SIZE;
	private static final long CONFIRM_MILLIS = 1500;

	private static long lastHotkey;

	private DropAll() {
	}

	/** Index im eigenen Inventar (0-8 Hotbar, 9-35 Inventar, 36-39 Ruestung, 40 Zweithand), sonst -1. */
	public static int slotIndex(Slot slot, Player player) {
		if (slot == null || player == null || slot.container != player.getInventory()) {
			return -1;
		}
		int index = slot.getContainerSlot();
		return index >= 0 && index <= Inventory.SLOT_OFFHAND ? index : -1;
	}

	/** Ob dieser Bereich laut Einstellungen geworfen wird. */
	public static boolean wanted(int index) {
		DropAllConfig config = DropAllMod.config;
		if (index < HOTBAR_END) {
			return config.dropHotbar;
		}
		if (index < ARMOR_START) {
			return config.dropInventory;
		}
		if (index < Inventory.SLOT_OFFHAND) {
			return config.dropArmor;
		}
		return config.dropOffhand;
	}

	/**
	 * Knopf nur, wo das eigene Inventar zu sehen ist. Das Kreativinventar ist ausgenommen: Dort laufen Klicks ueber
	 * ein Sondermenue und der Server drosselt Kreativ-Drops, Items wuerden verschwinden statt zu fallen.
	 */
	public static boolean supports(AbstractContainerScreen<?> screen, Player player) {
		if (player == null || screen instanceof CreativeModeInventoryScreen) {
			return false;
		}
		for (Slot slot : screen.getMenu().slots) {
			if (slotIndex(slot, player) >= 0) {
				return true;
			}
		}
		return false;
	}

	/** Von der Taste: mit optionaler Sicherheitsabfrage (zweimal druecken). */
	public static void fromHotkey(AbstractContainerMenu menu) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (DropAllMod.config.hotkeyConfirm) {
			long now = System.currentTimeMillis();
			if (now - lastHotkey > CONFIRM_MILLIS) {
				lastHotkey = now;
				player.sendOverlayMessage(Component.translatable("dropallplus.confirm"));
				return;
			}
			lastHotkey = 0;
		}
		dropAll(menu);
	}

	/** Gibt zurueck, wie viele Stapel geworfen wurden. */
	public static int dropAll(AbstractContainerMenu menu) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		MultiPlayerGameMode gameMode = mc.gameMode;
		if (player == null || gameMode == null || player.isSpectator() || menu != player.containerMenu) {
			return 0;
		}
		int dropped = 0;
		boolean offhandInMenu = false;
		for (Slot slot : menu.slots) {
			int index = slotIndex(slot, player);
			if (index == Inventory.SLOT_OFFHAND) {
				offhandInMenu = true;
			}
			if (index < 0 || !wanted(index) || Locks.isLocked(index) || !slot.hasItem()) {
				continue;
			}
			// Wie Strg+Q auf dem Slot: ganzer Stapel.
			gameMode.handleContainerInput(menu.containerId, slot.index, 1, ContainerInput.THROW, player);
			dropped++;
		}

		// In Kisten und anderen Menues hat die Zweithand keinen Slot. Dann erst in einen freien Slot tauschen (wie F) und werfen.
		if (!offhandInMenu && DropAllMod.config.dropOffhand && !Locks.isLocked(Inventory.SLOT_OFFHAND) && !player.getOffhandItem().isEmpty()) {
			for (Slot slot : menu.slots) {
				int index = slotIndex(slot, player);
				if (index >= 0 && index < ARMOR_START && !Locks.isLocked(index) && !slot.hasItem()) {
					gameMode.handleContainerInput(menu.containerId, slot.index, Inventory.SLOT_OFFHAND, ContainerInput.SWAP, player);
					gameMode.handleContainerInput(menu.containerId, slot.index, 1, ContainerInput.THROW, player);
					dropped++;
					break;
				}
			}
		}
		return dropped;
	}
}
