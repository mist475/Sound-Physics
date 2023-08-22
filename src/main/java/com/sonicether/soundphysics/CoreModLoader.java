package com.sonicether.soundphysics;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@IFMLLoadingPlugin.Name("CoreModLoader")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(10001)
public class CoreModLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { CoreModInjector.class.getName() };
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(final Map<String, Object> data) {
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	@Override
	public String getMixinConfig() {
		return "mixins.soundphysics.early.json";
	}

	@Override
	public List<String> getMixins(Set<String> loadedCoreMods) {
		boolean client = FMLLaunchHandler.side().isClient();
		List<String> mixins = new ArrayList<>();
		if (client) {
			mixins.add("MixinSoundManagerStarterThread");
			mixins.add("MixinSoundManager");
			mixins.add("MixinSourceLWJGLOpenAL");
			//TODO: respect Config.AutoDownMix option
			mixins.add("MixinLibraryLWJGLOpenAL");
			mixins.add("MixinSoundSystem");
			mixins.add("MixinWorld");
		}
		return mixins;
	}
}
