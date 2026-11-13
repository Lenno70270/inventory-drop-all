package de.lennard.dropall;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Zahnrad in Mod Menu. Wird nur geladen, wenn Mod Menu installiert ist. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return DropAllMenu::new;
	}
}
