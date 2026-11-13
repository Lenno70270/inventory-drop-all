package de.lennard.dropall.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.lennard.dropall.ButtonHolder;
import de.lennard.dropall.DropAll;
import de.lennard.dropall.DropAllConfig;
import de.lennard.dropall.DropAllMenu;
import de.lennard.dropall.DropAllMod;
import de.lennard.dropall.LockIcon;
import de.lennard.dropall.Locks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ButtonHolder {
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;
	@Shadow
	@Final
	protected int imageWidth;
	@Shadow
	@Final
	protected int imageHeight;
	@Shadow
	protected Slot hoveredSlot;
	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	@Unique
	private Button dropall$drop;
	@Unique
	private Button dropall$menu;

	@Override
	public void dropall$setButtons(Button drop, Button menu) {
		dropall$drop = drop;
		dropall$menu = menu;
		dropall$place();
	}

	/**
	 * Unter dem Menue rechtsbuendig, Zahnrad links daneben. Oder rechts neben dem Menue, Zahnrad dahinter. Passt es
	 * unten nicht hin (grosse Kiste, kleines Fenster), kommt es auch nach rechts.
	 */
	@Unique
	private void dropall$place() {
		if (dropall$drop == null) {
			return;
		}
		Screen self = (Screen) (Object) this;
		int w = dropall$drop.getWidth();
		int h = dropall$drop.getHeight();
		int x = leftPos + imageWidth - w;
		int y = topPos + imageHeight + 2;
		boolean right = DropAllMod.config.buttonPosition == DropAllConfig.POSITION_RIGHT || y + h > self.height - 2;
		if (right) {
			x = leftPos + imageWidth + 2;
			y = topPos + imageHeight - h;
		}
		dropall$drop.visible = DropAllMod.config.showButton;
		dropall$drop.setX(x);
		dropall$drop.setY(y);
		if (!DropAllMod.config.showButton) {
			dropall$menu.setX(right ? x : leftPos + imageWidth - dropall$menu.getWidth());
		} else {
			dropall$menu.setX(right ? x + w + 2 : x - dropall$menu.getWidth() - 2);
		}
		dropall$menu.setY(y);
	}

	// Jedes Bild neu setzen, weil sich das Menue verschiebt (Rezeptbuch) und die Einstellung sich aendern kann.
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void dropall$placeButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		dropall$place();
	}

	@Inject(method = "extractSlots", at = @At("TAIL"))
	private void dropall$drawLocks(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		Player player = Minecraft.getInstance().player;
		boolean any = false;
		for (Slot slot : menu.slots) {
			int index = DropAll.slotIndex(slot, player);
			if (index < 0 || !slot.isActive() || !Locks.isLocked(index)) {
				continue;
			}
			if (!any) {
				// Eigene Ebene, damit das Schloss ueber dem Item liegt.
				graphics.nextStratum();
				any = true;
			}
			LockIcon.draw(graphics, slot.x, slot.y);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void dropall$mouse(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (dropall$handle(DropAllMod.lock != null && DropAllMod.lock.matchesMouse(event),
				dropall$matches(DropAllMod.drop, event), dropall$matches(DropAllMod.menu, event))) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void dropall$key(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (dropall$handle(DropAllMod.lock != null && !DropAllMod.lock.isUnbound() && DropAllMod.lock.matches(event),
				DropAllMod.drop != null && !DropAllMod.drop.isUnbound() && DropAllMod.drop.matches(event),
				DropAllMod.menu != null && !DropAllMod.menu.isUnbound() && DropAllMod.menu.matches(event))) {
			cir.setReturnValue(true);
		}
	}

	@Unique
	private static boolean dropall$matches(KeyMapping mapping, MouseButtonEvent event) {
		return mapping != null && !mapping.isUnbound() && mapping.matchesMouse(event);
	}

	@Unique
	private boolean dropall$handle(boolean lock, boolean drop, boolean openMenu) {
		Minecraft mc = Minecraft.getInstance();
		AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
		if (lock) {
			int index = DropAll.slotIndex(hoveredSlot, mc.player);
			if (index >= 0) {
				Locks.toggle(index);
				return true;
			}
		}
		if (drop && DropAll.supports(self, mc.player)) {
			DropAll.fromHotkey(menu);
			return true;
		}
		if (openMenu) {
			mc.gui.setScreen(new DropAllMenu(self));
			return true;
		}
		return false;
	}
}
