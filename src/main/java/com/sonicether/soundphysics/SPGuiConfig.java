package com.sonicether.soundphysics;

import net.minecraft.client.gui.GuiScreen;
import cpw.mods.fml.client.config.GuiConfig;

public class SPGuiConfig extends GuiConfig {

	public SPGuiConfig(final GuiScreen parent) {
		super(parent, Config.instance.getConfigElements(), SoundPhysics.modid, false, false,
				"Sound Physics Configuration");
	}

}