package de.lennard.dropall;

import java.util.Properties;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Echter Client gegen echten Server: Knopf, Taste, Menue. Gesperrte Slots und Ruestung bleiben. */
public class DropAllClientTest implements FabricClientGameTest {
	private static final int SECONDS = 20;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestDedicatedServerContext server = ctx.worldBuilder().createServer(new Properties());
				TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			server.runCommand("op @a");
			server.runCommand("gamemode survival @a");
			server.runCommand("time set noon");
			server.runCommand("gamerule spawn_mobs false");
			ctx.waitTicks(20);
			ctx.runOnClient(mc -> {
				DropAllMod.config.resetToDefaults();
				Locks.clear();
				Locks.toggle(1);
			});

			// 1. Knopf im normalen Inventar.
			fill(server, ctx);
			ctx.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			ctx.waitTicks(10);
			ctx.takeScreenshot("dropall-inventar");
			ctx.clickScreenButton("Drop All");
			ctx.waitTicks(30);
			verify(ctx, "Knopf im Inventar", false);

			// 2. Knopf in einer Kiste. Hier hat die Zweithand keinen Slot.
			ctx.setScreen(() -> null);
			fill(server, ctx);
			BlockPos chest = ctx.computeOnClient(mc -> mc.player.blockPosition().offset(2, 0, 0));
			server.runCommand("setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " chest");
			server.runCommand("item replace block " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " container.0 with gold_ingot 3");
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(chest).add(0, 0.5, 0), Direction.UP, chest, false)));
			ctx.waitForScreen(ContainerScreen.class);
			ctx.waitTicks(10);
			ctx.takeScreenshot("dropall-kiste");
			ctx.clickScreenButton("Drop All");
			ctx.waitTicks(30);
			verify(ctx, "Knopf in Kiste", false);
			check(ctx.computeOnClient(mc -> mc.player.containerMenu.getSlot(0).getItem().is(Items.GOLD_INGOT)), "Kisteninhalt bleibt");
			ctx.runOnClient(mc -> mc.player.closeContainer());
			ctx.waitTicks(5);

			// 3. Taste im Spiel, ohne offenes Menue.
			ctx.runOnClient(mc -> bind(DropAllMod.drop, "key.keyboard.g"));
			fill(server, ctx);
			ctx.getInput().pressKey(DropAllMod.drop);
			ctx.waitTicks(30);
			verify(ctx, "Taste im Spiel", false);

			// 4. Taste im Inventar mit Sicherheitsabfrage: einmal passiert nichts, zweimal wirft.
			ctx.runOnClient(mc -> DropAllMod.config.hotkeyConfirm = true);
			fill(server, ctx);
			ctx.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			ctx.waitTicks(10);
			ctx.getInput().pressKey(DropAllMod.drop);
			ctx.waitTicks(20);
			check(ctx.computeOnClient(mc -> has(mc, 9, Items.STONE)), "Einmal drücken wirft noch nichts");
			ctx.getInput().pressKey(DropAllMod.drop);
			ctx.waitTicks(30);
			verify(ctx, "Taste im Inventar, zweimal", false);
			ctx.runOnClient(mc -> DropAllMod.config.hotkeyConfirm = false);
			ctx.setScreen(() -> null);

			// 5. Mit Ruestung.
			ctx.runOnClient(mc -> DropAllMod.config.dropArmor = true);
			fill(server, ctx);
			ctx.getInput().pressKey(DropAllMod.drop);
			ctx.waitTicks(30);
			verify(ctx, "Taste mit Rüstung", true);
			ctx.runOnClient(mc -> DropAllMod.config.dropArmor = false);

			// 6. Menue ueber das Zahnrad im Inventar.
			fill(server, ctx);
			ctx.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			ctx.waitTicks(10);
			ctx.clickScreenButton("⚙");
			ctx.waitForScreen(DropAllMenu.class);
			ctx.waitTicks(10);
			ctx.takeScreenshot("dropall-menue-allgemein");
			DropAllMenu menu = ctx.computeOnClient(mc -> (DropAllMenu) mc.gui.screen());
			ctx.runOnClient(mc -> menu.showPage("Werfen"));
			ctx.waitTicks(5);
			ctx.takeScreenshot("dropall-menue-werfen");
			ctx.runOnClient(mc -> menu.showPage("Gesperrte Slots"));
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> menu.press("slot:9")), "Slot 9 im Raster anklickbar");
			check(ctx.computeOnClient(mc -> Locks.isLocked(9)), "Slot 9 im Menü gesperrt");
			ctx.waitTicks(5);
			ctx.takeScreenshot("dropall-menue-slots");
			check(ctx.computeOnClient(mc -> menu.press("slot:9")) && ctx.computeOnClient(mc -> !Locks.isLocked(9)), "Slot 9 wieder frei");
			System.out.println("[DropAllTest] OK Menü: Seiten, Sperren per Klick");

			// Taste im Menue neu belegen.
			ctx.runOnClient(mc -> menu.showPage("Allgemein"));
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> menu.press("Drop-All-Taste")), "Tastenzeile anklickbar");
			ctx.getInput().pressKey(InputConstants.getKey("key.keyboard.h"));
			ctx.waitTicks(5);
			check(ctx.computeOnClient(mc -> DropAllMod.drop.saveString().equals("key.keyboard.h")), "Taste im Menü auf H gesetzt, war " + ctx.computeOnClient(mc -> DropAllMod.drop.saveString()));
			System.out.println("[DropAllTest] OK Taste im Menü neu belegt");
			check(ctx.computeOnClient(mc -> menu.press("Fertig")), "Fertig anklickbar");
			ctx.waitForScreen(InventoryScreen.class);
			System.out.println("[DropAllTest] OK Fertig führt zurück ins Inventar");

			ctx.setScreen(() -> null);
			ctx.runOnClient(mc -> {
				Locks.clear();
				DropAllMod.config.resetToDefaults();
				bind(DropAllMod.drop, "key.keyboard.unknown");
			});
		}
	}

	private static void bind(KeyMapping mapping, String key) {
		mapping.setKey(InputConstants.getKey(key));
		KeyMapping.resetMapping();
	}

	private static void fill(TestDedicatedServerContext server, ClientGameTestContext ctx) {
		server.runCommand("kill @e[type=item]");
		server.runCommand("clear @a");
		server.runCommand("item replace entity @a hotbar.0 with diamond 5");
		server.runCommand("item replace entity @a hotbar.1 with dirt 32");
		server.runCommand("item replace entity @a hotbar.8 with bread 4");
		server.runCommand("item replace entity @a inventory.0 with stone 64");
		server.runCommand("item replace entity @a inventory.26 with cobblestone 10");
		server.runCommand("item replace entity @a armor.head with iron_helmet");
		server.runCommand("item replace entity @a armor.chest with iron_chestplate");
		server.runCommand("item replace entity @a weapon.offhand with shield");
		ctx.waitFor(mc -> mc.player.getOffhandItem().is(Items.SHIELD) && has(mc, 9, Items.STONE)
				&& mc.player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), 5 * SECONDS);
		for (int attempt = 0; ; attempt++) {
			if (ctx.computeOnClient(mc -> mc.level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(8)).isEmpty())) {
				return;
			}
			if (attempt == 5) {
				throw new AssertionError("Items liegen noch: " + ctx.computeOnClient(mc -> mc.level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(8))
						.stream().map(e -> e.getItem() + " @ " + e.blockPosition() + " alive=" + e.isAlive()).toList()));
			}
			server.runCommand("kill @e[type=item]");
			ctx.waitTicks(10);
		}
	}

	private static void verify(ClientGameTestContext ctx, String where, boolean armorDropped) {
		check(ctx.computeOnClient(mc -> has(mc, 1, Items.DIRT)), where + ": gesperrte Erde bleibt");
		for (int slot : new int[] {0, 8, 9, 35}) {
			check(ctx.computeOnClient(mc -> mc.player.getInventory().getItem(slot).isEmpty()), where + ": Slot " + slot + " geleert");
		}
		boolean armorWorn = ctx.computeOnClient(mc -> mc.player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
				&& mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE));
		check(armorWorn != armorDropped, where + (armorDropped ? ": Rüstung geworfen" : ": Rüstung bleibt"));
		check(ctx.computeOnClient(mc -> mc.player.getInventory().getItem(Inventory.SLOT_OFFHAND).isEmpty()), where + ": Zweithand geworfen");
		ctx.waitFor(mc -> dropped(mc, Items.SHIELD) && dropped(mc, Items.STONE) && dropped(mc, Items.DIAMOND)
				&& dropped(mc, Items.BREAD) && dropped(mc, Items.COBBLESTONE) && (!armorDropped || dropped(mc, Items.IRON_HELMET)), 5 * SECONDS);
		check(ctx.computeOnClient(mc -> !dropped(mc, Items.DIRT)), where + ": gesperrte Erde nicht am Boden");
		System.out.println("[DropAllTest] OK " + where);
	}

	private static boolean has(Minecraft mc, int slot, Item item) {
		return mc.player.getInventory().getItem(slot).is(item);
	}

	private static boolean dropped(Minecraft mc, Item item) {
		return !mc.level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(8), e -> e.getItem().is(item)).isEmpty();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}
}
