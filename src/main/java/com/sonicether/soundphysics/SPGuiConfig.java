package com.sonicether.soundphysics;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import cpw.mods.fml.client.config.GuiConfig;

public class SPGuiConfig extends GuiConfig {

	public SPGuiConfig(final GuiScreen parent) {
		super(parent, Config.instance.getConfigElements(), SoundPhysics.modid, false, false,
				"Sound Physics Configuration");
	}

	@Override
	public void initGui()
	{
		super.initGui();
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button)
	{
		super.actionPerformed(button);
	}
}