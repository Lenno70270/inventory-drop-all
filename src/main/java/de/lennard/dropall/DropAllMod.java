package de.lennard.dropall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DropAllMod implements ClientModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("dropallplus");
	public static final int BUTTON_WIDTH = 54;
	public static final int BUTTON_HEIGHT = 16;

	public static DropAllConfig config = new DropAllConfig();
	/** Ganzes Inventar werfen. Standard frei, damit nichts aus Versehen fliegt. */
	public static KeyMapping drop;
	/** Slot unter der Maus sperren. Standard mittlere Maustaste. */
	public static KeyMapping lock;
	/** Menue oeffnen. Standard frei, der Zahnrad-Knopf im Inventar geht immer. */
	public static KeyMapping menu;

	@Override
	public void onInitializeClient() {
		config = DropAllConfig.load();
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dropallplus", "main"));
		drop = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.dropallplus.drop", InputConstants.UNKNOWN.getValue(), category));
		lock = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.dropallplus.lock", InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_MIDDLE, category));
		menu = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.dropallplus.menu", InputConstants.UNKNOWN.getValue(), category));

		// Ohne offenes Menue: Tasten kommen hier an. Mit offenem Menue faengt die Mixin sie ab.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (drop.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					DropAll.fromHotkey(client.player.inventoryMenu);
				}
			}
			while (menu.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new DropAllMenu(null));
				}
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof AbstractContainerScreen<?> container && DropAll.supports(container, client.player)) {
				Button dropButton = Button.builder(Component.translatable("dropallplus.button"), b -> DropAll.dropAll(container.getMenu()))
						.size(BUTTON_WIDTH, BUTTON_HEIGHT)
						.tooltip(Tooltip.create(Component.translatable("dropallplus.button.tooltip")))
						.build();
				Button menuButton = Button.builder(Component.literal("⚙"), b -> client.gui.setScreen(new DropAllMenu(screen)))
						.size(BUTTON_HEIGHT, BUTTON_HEIGHT)
						.tooltip(Tooltip.create(Component.translatable("dropallplus.menu.tooltip")))
						.build();
				Screens.getWidgets(screen).add(dropButton);
				Screens.getWidgets(screen).add(menuButton);
				((ButtonHolder) container).dropall$setButtons(dropButton, menuButton);
			}
		});
	}
}
