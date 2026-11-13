package de.lennard.dropall;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Einstellungen im Aufbau des Jarvis-Menues (Sodium-Stil): Seitenliste, flache Zeilen, Tooltip daneben. */
public final class DropAllMenu extends Screen {
	private static final int THEME = 0xFF94E4D3;
	private static final int FOREGROUND = 0xFFFFFFFF;
	private static final int DISABLED = 0xFFAAAAAA;
	private static final int ROW_BACKGROUND = 0x90000000;
	private static final int ROW_HOVER = 0xE0000000;
	private static final int PANEL = 0x90000000;
	private static final int OVERLAY = 0xEA000000;
	private static final int SCROLL_TRACK = 0x60000000;
	private static final int SCROLL_THUMB = 0xFFAAAAAA;
	private static final int SLOT_BACKGROUND = 0x70000000;
	private static final int SLOT_HOVER = 0x50FFFFFF;
	private static final int SLOT_LOCKED = 0x40F2BE2E;

	private static final int MARGIN = 6;
	private static final int ROW = 20;
	private static final int GROUP_GAP = 3;
	private static final int PAGE_WIDTH = 125;
	private static final int OPTION_WIDTH = 230;
	private static final int TEXT_PAD = 6;
	private static final int BUTTON_WIDTH = 65;
	private static final int TOOLTIP_PAD = 5;
	private static final int MIN_TOOLTIP_WIDTH = 100;

	private static final int CELL = 18;
	private static final int GRID_PAD = 5;
	private static final int GRID_GAP = 4;
	private static final int GRID_HEIGHT = GRID_PAD * 2 + CELL * 5 + GRID_GAP * 2;
	/** Ruestung von oben nach unten wie im Inventar: Kopf, Brust, Beine, Fuesse. */
	private static final int[] ARMOR = {39, 38, 37, 36};

	/** Übersetzter Text. Die Texte stehen in assets/dropallplus/lang. */
	private static String t(String key, Object... args) {
		return Component.translatable(key, args).getString();
	}

	private static String gridTooltip() {
		return t("dropallplus.menu.grid.tooltip");
	}

	private abstract static class Row {
		final String label;
		final String tooltip;

		Row(String label, String tooltip) {
			this.label = label;
			this.tooltip = tooltip;
		}

		abstract String value();

		void click() {
		}
	}

	private final class Toggle extends Row {
		final Supplier<Boolean> getter;
		final Consumer<Boolean> setter;

		Toggle(String label, String tooltip, Supplier<Boolean> getter, Consumer<Boolean> setter) {
			super(label, tooltip);
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		String value() {
			return "";
		}

		@Override
		void click() {
			setter.accept(!getter.get());
			DropAllMod.config.save();
		}
	}

	private final class KeyRow extends Row {
		final KeyMapping mapping;

		KeyRow(String label, String tooltip, KeyMapping mapping) {
			super(label, tooltip);
			this.mapping = mapping;
		}

		@Override
		String value() {
			if (capturing == this) {
				return t("dropallplus.menu.key.press");
			}
			return mapping.isUnbound() ? t("dropallplus.menu.key.unbound") : mapping.getTranslatedKeyMessage().getString();
		}

		@Override
		void click() {
			capturing = this;
		}
	}

	private record Page(String name, List<List<Row>> groups, boolean lockGrid) {
	}

	private record Hit(String label, int x, int y, int w, int h, Runnable action) {
		boolean contains(double px, double py) {
			return px >= x && px < x + w && py >= y && py < y + h;
		}
	}

	private final Screen parent;
	private final List<Page> pages = new ArrayList<>();
	private final List<Hit> hits = new ArrayList<>();
	private int page;
	private int scroll;
	private int contentHeight;
	private KeyRow capturing;
	private GuiGraphicsExtractor g;

	public DropAllMenu(Screen parent) {
		super(Component.literal("Drop All"));
		this.parent = parent;
		buildPages();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- Einstellungen ----

	private void buildPages() {
		DropAllConfig c = DropAllMod.config;
		pages.clear();
		pages.add(new Page(t("dropallplus.menu.page.general"), List.of(
				List.of(
						toggle(t("dropallplus.menu.show_button"), t("dropallplus.menu.show_button.tooltip"), () -> c.showButton, v -> c.showButton = v),
						cycle(t("dropallplus.menu.button_position"), t("dropallplus.menu.button_position.tooltip"),
								List.of(DropAllConfig.POSITION_BELOW, DropAllConfig.POSITION_RIGHT), v -> v == DropAllConfig.POSITION_RIGHT ? t("dropallplus.menu.position.right") : t("dropallplus.menu.position.below"),
								() -> c.buttonPosition, v -> c.buttonPosition = v)),
				List.of(
						new KeyRow(t("dropallplus.menu.drop_key"), t("dropallplus.menu.drop_key.tooltip"), DropAllMod.drop),
						toggle(t("dropallplus.menu.confirm"), t("dropallplus.menu.confirm.tooltip"),
								() -> c.hotkeyConfirm, v -> c.hotkeyConfirm = v)),
				List.of(
						new KeyRow(t("dropallplus.menu.lock_key"), t("dropallplus.menu.lock_key.tooltip"), DropAllMod.lock),
						new KeyRow(t("dropallplus.menu.menu_key"), t("dropallplus.menu.menu_key.tooltip"), DropAllMod.menu))),
				false));

		pages.add(new Page(t("dropallplus.menu.page.drop"), List.of(
				List.of(
						toggle(t("dropallplus.menu.drop.hotbar"), t("dropallplus.menu.drop.hotbar.tooltip"), () -> c.dropHotbar, v -> c.dropHotbar = v),
						toggle(t("dropallplus.menu.drop.inventory"), t("dropallplus.menu.drop.inventory.tooltip"), () -> c.dropInventory, v -> c.dropInventory = v),
						toggle(t("dropallplus.menu.drop.offhand"), t("dropallplus.menu.drop.offhand.tooltip"), () -> c.dropOffhand, v -> c.dropOffhand = v),
						toggle(t("dropallplus.menu.drop.armor"), t("dropallplus.menu.drop.armor.tooltip"),
								() -> c.dropArmor, v -> c.dropArmor = v))),
				false));

		pages.add(new Page(t("dropallplus.menu.page.locked"), List.of(
				List.of(
						info(t("dropallplus.menu.locked"), t("dropallplus.menu.locked.tooltip"),
								() -> Locks.count() == 1 ? t("dropallplus.menu.locked.one") : t("dropallplus.menu.locked.many", Locks.count())),
						action(t("dropallplus.menu.unlock_all"), t("dropallplus.menu.unlock_all.tooltip"), Locks::clear))),
				true));
	}

	private Row toggle(String label, String tooltip, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return new Toggle(label, tooltip, getter, setter);
	}

	private <T> Row cycle(String label, String tooltip, List<T> values, Function<T, String> text, Supplier<T> getter, Consumer<T> setter) {
		return new Row(label, tooltip) {
			@Override
			String value() {
				return text.apply(getter.get());
			}

			@Override
			void click() {
				setter.accept(values.get((values.indexOf(getter.get()) + 1) % values.size()));
				DropAllMod.config.save();
			}
		};
	}

	private Row action(String label, String tooltip, Runnable run) {
		return new Row(label, tooltip) {
			@Override
			String value() {
				return t("dropallplus.menu.run");
			}

			@Override
			void click() {
				run.run();
			}
		};
	}

	private Row info(String label, String tooltip, Supplier<String> value) {
		return new Row(label, tooltip) {
			@Override
			String value() {
				return value.get();
			}
		};
	}

	// ---- Zeichnen ----

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		g = graphics;
		hits.clear();
		int top = MARGIN;
		int bottom = height - MARGIN - ROW - MARGIN;
		pageList(mouseX, mouseY, top, bottom);
		String tip = optionList(mouseX, mouseY, top, bottom);
		if (tip != null) {
			tooltip(tip, top, bottom);
		}
		footer(mouseX, mouseY);
	}

	private void pageList(int mx, int my, int top, int bottom) {
		g.fill(MARGIN, top, MARGIN + PAGE_WIDTH, bottom, PANEL);
		g.text(font, "Drop All", MARGIN + TEXT_PAD, top + 6, THEME, true);
		int y = top + ROW;
		for (int i = 0; i < pages.size(); i++) {
			int index = i;
			boolean selected = page == i;
			if (selected || over(mx, my, MARGIN, y, PAGE_WIDTH, ROW)) {
				g.fill(MARGIN, y, MARGIN + PAGE_WIDTH, y + ROW, ROW_HOVER);
			}
			g.text(font, pages.get(i).name(), MARGIN + TEXT_PAD + 4, y + 6, selected ? THEME : FOREGROUND, true);
			hits.add(new Hit(pages.get(i).name(), MARGIN, y, PAGE_WIDTH, ROW, () -> {
				page = index;
				scroll = 0;
				capturing = null;
			}));
			y += ROW;
		}
	}

	/** Zeichnet die Zeilen der Seite und gibt den Tooltip-Text des Elements unter der Maus zurueck. */
	private String optionList(int mx, int my, int top, int bottom) {
		int x = MARGIN + PAGE_WIDTH + MARGIN;
		int w = Math.min(OPTION_WIDTH, width - x - MARGIN - 8);
		int viewHeight = bottom - top;
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewHeight)));
		String tip = null;
		int y = top - scroll;
		boolean inView = my >= top && my < bottom;
		g.enableScissor(x, top, x + w, bottom);
		for (List<Row> group : pages.get(page).groups()) {
			for (Row row : group) {
				boolean visible = y + ROW > top && y < bottom;
				boolean hover = visible && inView && over(mx, my, x, y, w, ROW);
				if (visible) {
					drawRow(row, x, y, w, hover);
					hits.add(new Hit(row.label, x, Math.max(y, top), w, Math.min(y + ROW, bottom) - Math.max(y, top), row::click));
				}
				if (hover) {
					tip = row.tooltip;
				}
				y += ROW;
			}
			y += GROUP_GAP;
		}
		if (pages.get(page).lockGrid()) {
			String gridTip = lockGrid(x, y, w, mx, my, top, bottom, inView);
			if (gridTip != null) {
				tip = gridTip;
			}
			y += GRID_HEIGHT + GROUP_GAP;
		}
		g.disableScissor();
		contentHeight = y + scroll - top - GROUP_GAP;
		if (contentHeight > viewHeight) {
			int bar = x + w + 2;
			int thumb = Math.max(16, viewHeight * viewHeight / contentHeight);
			int offset = (viewHeight - thumb) * scroll / Math.max(1, contentHeight - viewHeight);
			g.fill(bar, top, bar + 5, bottom, SCROLL_TRACK);
			g.fill(bar + 1, top + offset, bar + 4, top + offset + thumb, SCROLL_THUMB);
		}
		return tip;
	}

	/** Das eigene Inventar zum Anklicken: oben Ruestung und Zweithand, dann Inventar, unten Hotbar. */
	private String lockGrid(int x, int y, int w, int mx, int my, int top, int bottom, boolean inView) {
		g.fill(x, y, x + w, y + GRID_HEIGHT, ROW_BACKGROUND);
		int left = x + (w - CELL * 9) / 2;
		int rowY = y + GRID_PAD;
		List<int[]> cells = new ArrayList<>();
		for (int i = 0; i < ARMOR.length; i++) {
			cells.add(new int[] {ARMOR[i], left + i * CELL, rowY});
		}
		cells.add(new int[] {Inventory.SLOT_OFFHAND, left + 8 * CELL, rowY});
		rowY += CELL + GRID_GAP;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				cells.add(new int[] {9 + row * 9 + col, left + col * CELL, rowY + row * CELL});
			}
		}
		rowY += CELL * 3 + GRID_GAP;
		for (int col = 0; col < 9; col++) {
			cells.add(new int[] {col, left + col * CELL, rowY});
		}

		Inventory inventory = minecraft != null && minecraft.player != null ? minecraft.player.getInventory() : null;
		String tip = null;
		for (int[] cell : cells) {
			int index = cell[0];
			int cx = cell[1];
			int cy = cell[2];
			boolean hover = inView && over(mx, my, cx, cy, CELL, CELL);
			g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, SLOT_BACKGROUND);
			if (Locks.isLocked(index)) {
				g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, SLOT_LOCKED);
			}
			if (hover) {
				g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, SLOT_HOVER);
				tip = slotTooltip(index, inventory);
			}
			if (inventory != null) {
				ItemStack stack = inventory.getItem(index);
				if (!stack.isEmpty()) {
					g.item(stack, cx + 1, cy + 1);
					g.itemDecorations(font, stack, cx + 1, cy + 1);
				}
			}
			if (cy + CELL > top && cy < bottom) {
				hits.add(new Hit("slot:" + index, cx, cy, CELL - 1, CELL - 1, () -> Locks.toggle(index)));
			}
		}
		// Eigene Ebene, damit das Schloss ueber dem Item liegt.
		g.nextStratum();
		for (int[] cell : cells) {
			if (Locks.isLocked(cell[0])) {
				LockIcon.draw(g, cell[1] + 1, cell[2] + 1);
			}
		}
		return tip;
	}

	private static String slotTooltip(int index, Inventory inventory) {
		String name;
		if (index < DropAll.HOTBAR_END) {
			name = t("dropallplus.menu.slot.hotbar", index + 1);
		} else if (index < DropAll.ARMOR_START) {
			name = t("dropallplus.menu.slot.inventory", index - DropAll.HOTBAR_END + 1);
		} else if (index == Inventory.SLOT_OFFHAND) {
			name = t("dropallplus.menu.drop.offhand");
		} else {
			name = switch (index) {
				case 39 -> t("dropallplus.menu.slot.helmet");
				case 38 -> t("dropallplus.menu.slot.chestplate");
				case 37 -> t("dropallplus.menu.slot.leggings");
				default -> t("dropallplus.menu.slot.boots");
			};
		}
		String item = "";
		if (inventory != null && !inventory.getItem(index).isEmpty()) {
			item = t("dropallplus.menu.slot.with", inventory.getItem(index).getHoverName().getString());
		}
		return t("dropallplus.menu.slot.tooltip", name + item, t(Locks.isLocked(index) ? "dropallplus.menu.slot.locked" : "dropallplus.menu.slot.free"))
				+ "\n\n" + gridTooltip();
	}

	private void drawRow(Row row, int x, int y, int w, boolean hover) {
		g.fill(x, y, x + w, y + ROW, hover ? ROW_HOVER : ROW_BACKGROUND);
		g.text(font, row.label, x + TEXT_PAD, y + 6, FOREGROUND, true);
		int right = x + w - TEXT_PAD;
		if (row instanceof Toggle toggle) {
			drawTick(right, y, toggle.getter.get());
		} else {
			String value = row.value();
			g.text(font, value, right - font.width(value), y + 6, capturing == row ? THEME : FOREGROUND, true);
		}
	}

	private void drawTick(int right, int y, boolean on) {
		int left = right - 10;
		int boxTop = y + 5;
		g.outline(left, boxTop, 10, 10, FOREGROUND);
		if (on) {
			g.fill(left + 2, boxTop + 2, left + 8, boxTop + 8, THEME);
		}
	}

	private void tooltip(String text, int top, int bottom) {
		int x = MARGIN + PAGE_WIDTH + MARGIN + OPTION_WIDTH + 10;
		int w = width - x - MARGIN;
		if (w < MIN_TOOLTIP_WIDTH) {
			return;
		}
		List<FormattedCharSequence> lines = new ArrayList<>();
		for (String paragraph : text.split("\n", -1)) {
			if (paragraph.isEmpty()) {
				lines.add(FormattedCharSequence.EMPTY);
			} else {
				lines.addAll(font.split(Component.literal(paragraph), w - TOOLTIP_PAD * 2));
			}
		}
		int h = lines.size() * (font.lineHeight + 2) + TOOLTIP_PAD * 2;
		g.fill(x, top, x + w, Math.min(top + h, bottom), OVERLAY);
		int y = top + TOOLTIP_PAD;
		for (FormattedCharSequence line : lines) {
			g.text(font, line, x + TOOLTIP_PAD, y, FOREGROUND, true);
			y += font.lineHeight + 2;
		}
	}

	private void footer(int mx, int my) {
		int y = height - MARGIN - ROW;
		button(t("dropallplus.menu.done"), width - MARGIN - BUTTON_WIDTH, y, mx, my, this::onClose);
		button(t("dropallplus.menu.defaults"), width - MARGIN - BUTTON_WIDTH * 2 - 4, y, mx, my, () -> {
			DropAllMod.config.resetToDefaults();
			DropAllMod.config.save();
		});
	}

	private void button(String label, int x, int y, int mx, int my, Runnable action) {
		g.fill(x, y, x + BUTTON_WIDTH, y + ROW, over(mx, my, x, y, BUTTON_WIDTH, ROW) ? ROW_HOVER : ROW_BACKGROUND);
		g.centeredText(font, label, x + BUTTON_WIDTH / 2, y + 6, FOREGROUND);
		hits.add(new Hit(label, x, y, BUTTON_WIDTH, ROW, action));
	}

	private static boolean over(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ---- Eingabe ----

	/** Neue Taste uebernehmen und wie im Steuerungsmenue speichern. */
	private void bind(InputConstants.Key key) {
		capturing.mapping.setKey(key);
		KeyMapping.resetMapping();
		if (minecraft != null) {
			minecraft.options.save();
		}
		capturing = null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (capturing != null) {
			bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		for (Hit hit : List.copyOf(hits)) {
			if (hit.contains(event.x(), event.y())) {
				hit.action().run();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (capturing != null) {
			bind(event.key() == InputConstants.KEY_ESCAPE ? InputConstants.UNKNOWN : InputConstants.getKey(event));
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		scroll -= (int) (vertical * ROW);
		return true;
	}

	@Override
	public void onClose() {
		DropAllMod.config.save();
		if (minecraft != null) {
			minecraft.gui.setScreen(parent);
		}
	}

	/** Fuer den Test: loest ein Element ueber seinen Namen aus. */
	public boolean press(String label) {
		for (Hit hit : List.copyOf(hits)) {
			if (label.equals(hit.label())) {
				hit.action().run();
				return true;
			}
		}
		return false;
	}

	/** Fuer den Test: Seite wechseln. */
	public boolean showPage(String name) {
		for (int i = 0; i < pages.size(); i++) {
			if (pages.get(i).name().equals(name)) {
				page = i;
				scroll = 0;
				return true;
			}
		}
		return false;
	}
}
