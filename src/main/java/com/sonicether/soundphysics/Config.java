package com.sonicether.soundphysics;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.client.config.IConfigElement;

public class Config {

	public static final Config instance = new Config();
	private Configuration forgeConfig;

	// general
	public static float rolloffFactor;
	public static float globalReverbGain;
	public static float globalReverbBrightness;
	public static float soundDistanceAllowance;
	public static float globalBlockAbsorption;
	public static float globalBlockReflectance;
	public static float airAbsorption;
	public static float snowAirAbsorptionFactor;
	public static float underwaterFilter;
	public static boolean noteBlockEnable;

	// performance
	public static boolean skipRainOcclusionTracing;
	public static int environmentEvaluationRays;
	public static int environmentEvaluationRaysBounces;
	public static boolean simplerSharedAirspaceSimulation;
	public static boolean dynamicEnvironementEvalutaion;
	public static int dynamicEnvironementEvalutaionFrequency;

	// block properties
	public static float stoneReflectivity;
	public static float woodReflectivity;
	public static float groundReflectivity;
	public static float plantReflectivity;
	public static float metalReflectivity;
	public static float glassReflectivity;
	public static float clothReflectivity;
	public static float sandReflectivity;
	public static float snowReflectivity;

	// compatibility
	public static boolean computronicsPatching;
	public static boolean autoSteroDownmix;
	
	// misc
	public static boolean autoSteroDownmixLogging;
	public static boolean debugInfoShow;
	public static boolean injectorLogging;

	private static final String categoryGeneral = "General";
	private static final String categoryPerformance = "Performance";
	private static final String categoryMaterialProperties = "Material properties";
	private static final String categoryCompatibility = "Compatibility";
	private static final String categoryMisc = "Misc";

	private Config() {
	}

	public void setConfig(Configuration config) {
		this.forgeConfig = config;
		syncConfig();
	}

	@SuppressWarnings("rawtypes")
	public List<IConfigElement> getConfigElements() {
		final ArrayList<IConfigElement> list = new ArrayList<>();

		list.add(new ConfigElement(this.forgeConfig.getCategory(Config.categoryGeneral)));
		list.add(new ConfigElement(this.forgeConfig.getCategory(Config.categoryPerformance)));
		list.add(new ConfigElement(this.forgeConfig.getCategory(Config.categoryMaterialProperties)));
		list.add(new ConfigElement(this.forgeConfig.getCategory(Config.categoryCompatibility)));
		list.add(new ConfigElement(this.forgeConfig.getCategory(Config.categoryMisc)));

		return list;
	}

	/**
	 * @return If the configuration has changed.
	 */
	public boolean syncConfig() {
		// General
		rolloffFactor = this.forgeConfig.getFloat("Attenuation Factor", categoryGeneral, 1.0f, 0.2f, 1.0f,
				"Affects how quiet a sound gets based on distance. Lower values mean distant sounds are louder. 1.0 is the physically correct value.");
		globalReverbGain = this.forgeConfig.getFloat("Global Reverb Gain", categoryGeneral, 1.0f, 0.1f, 2.0f,
				"The global volume of simulated reverberations.");
		globalReverbBrightness = this.forgeConfig.getFloat("Global Reverb Brightness", categoryGeneral, 1.0f, 0.1f,
				2.0f,
				"The brightness of reverberation. Higher values result in more high frequencies in reverberation. Lower values give a more muffled sound to the reverb.");
		globalBlockAbsorption = this.forgeConfig.getFloat("Global Block Absorption", categoryGeneral, 1.0f, 0.1f, 4.0f,
				"The global amount of sound that will be absorbed when traveling through blocks.");
		globalBlockReflectance = this.forgeConfig.getFloat("Global Block Reflectance", categoryGeneral, 1.0f, 0.1f,
				4.0f,
				"The global amount of sound reflectance energy of all blocks. Lower values result in more conservative reverb simulation with shorter reverb tails. Higher values result in more generous reverb simulation with higher reverb tails.");
		soundDistanceAllowance = this.forgeConfig.getFloat("Sound Distance Allowance", categoryGeneral, 4.0f, 1.0f,
				6.0f,
				"Minecraft won't allow sounds to play past a certain distance. This parameter is a multiplier for how far away a sound source is allowed to be in order for it to actually play. Values too high can cause polyphony issues.");
		airAbsorption = this.forgeConfig.getFloat("Air Absorption", categoryGeneral, 1.0f, 0.0f, 5.0f,
				"A value controlling the amount that air absorbs high frequencies with distance. A value of 1.0 is physically correct for air with normal humidity and temperature. Higher values mean air will absorb more high frequencies with distance. 0 disables this effect.");
		snowAirAbsorptionFactor = this.forgeConfig.getFloat("Max Snow Air Absorption Factor", categoryGeneral, 5.0f, 0.0f, 10.0f,
				"The maximum air absorption factor when it's snowing. The real absorption factor will depend on the snow's intensity. Set to 1 or lower to disable");
		underwaterFilter = this.forgeConfig.getFloat("Underwater Filter", categoryGeneral, 0.8f, 0.0f, 1.0f,
				"How much sound is filtered when the player is underwater. 0.0 means no filter. 1.0 means fully filtered.");
		noteBlockEnable = this.forgeConfig.getBoolean("Affect Note Blocks", categoryGeneral, true,
				"If true, note blocks will be processed.");

		// performance
		skipRainOcclusionTracing = this.forgeConfig.getBoolean("Skip Rain Occlusion Tracing", categoryPerformance, true,
				"If true, rain sound sources won't trace for sound occlusion. This can help performance during rain.");
		environmentEvaluationRays = this.forgeConfig.getInt("Environment Evaluation Rays", categoryPerformance, 32, 8,
				64,
				"The number of rays to trace to determine reverberation for each sound source. More rays provides more consistent tracing results but takes more time to calculate. Decrease this value if you experience lag spikes when sounds play.");
		environmentEvaluationRaysBounces = this.forgeConfig.getInt("Environment Evaluation Rays Bounces", categoryPerformance, 4, 1,
				64,
				"The number of rays bounces to determine reverberation for each sound source. More bounces provides more consistent tracing results but takes more time to calculate. Decrease this value if you experience lag spikes when sounds play.");
		simplerSharedAirspaceSimulation = this.forgeConfig.getBoolean("Simpler Shared Airspace Simulation",
				categoryPerformance, false,
				"If true, enables a simpler technique for determining when the player and a sound source share airspace. Might sometimes miss recognizing shared airspace, but it's faster to calculate.");
		dynamicEnvironementEvalutaion = this.forgeConfig.getBoolean("Dynamic environment evaluation", categoryPerformance, false,
				"WARNING it's implemented really badly so i'd recommend not always using it.If true, the environment will keep getting evaluated for every sound that is currently playing. This may affect performance");
		dynamicEnvironementEvalutaionFrequency = this.forgeConfig.getInt("Frequency of environment evaluation", categoryPerformance, 30, 1, 60,
				"The frequency at witch to update environment of sounds if dynamic environment evaluation is enabled");

		// material properties
		stoneReflectivity = this.forgeConfig.getFloat("Stone Reflectivity", categoryMaterialProperties, 0.95f, 0.0f,
				1.0f, "Sound reflectivity for stone blocks.");
		woodReflectivity = this.forgeConfig.getFloat("Wood Reflectivity", categoryMaterialProperties, 0.7f, 0.0f, 1.0f,
				"Sound reflectivity for wooden blocks.");
		groundReflectivity = this.forgeConfig.getFloat("Ground Reflectivity", categoryMaterialProperties, 0.3f, 0.0f,
				1.0f, "Sound reflectivity for ground blocks (dirt, gravel, etc).");
		plantReflectivity = this.forgeConfig.getFloat("Foliage Reflectivity", categoryMaterialProperties, 0.2f, 0.0f,
				1.0f, "Sound reflectivity for foliage blocks (leaves, grass, etc.).");
		metalReflectivity = this.forgeConfig.getFloat("Metal Reflectivity", categoryMaterialProperties, 0.97f, 0.0f,
				1.0f, "Sound reflectivity for metal blocks.");
		glassReflectivity = this.forgeConfig.getFloat("Glass Reflectivity", categoryMaterialProperties, 0.5f, 0.0f,
				1.0f, "Sound reflectivity for glass blocks.");
		clothReflectivity = this.forgeConfig.getFloat("Cloth Reflectivity", categoryMaterialProperties, 0.25f, 0.0f,
				1.0f, "Sound reflectivity for cloth blocks (carpet, wool, etc).");
		sandReflectivity = this.forgeConfig.getFloat("Sand Reflectivity", categoryMaterialProperties, 0.2f, 0.0f, 1.0f,
				"Sound reflectivity for sand blocks.");
		snowReflectivity = this.forgeConfig.getFloat("Snow Reflectivity", categoryMaterialProperties, 0.2f, 0.0f, 1.0f,
				"Sound reflectivity for snow blocks.");

		// compatibility
		computronicsPatching = this.forgeConfig.getBoolean("Patch Computronics", categoryCompatibility, true,
				"MAY REQUIRE RESTART.If true, patches the computronics sound sources so it works with sound physics.");
		autoSteroDownmix = this.forgeConfig.getBoolean("Auto stereo downmix", categoryCompatibility, true,
				"MAY REQUIRE RESTART.If true, Automatically downmix stereo sounds that are loaded to mono");

		// misc
		autoSteroDownmixLogging = this.forgeConfig.getBoolean("Stereo downmix Logging", categoryMisc, false,
				"If true, Prints sound name and format of the sounds that get converted");
		debugInfoShow = this.forgeConfig.getBoolean("Dynamic env. info in F3", categoryMisc, false,
				"If true, Shows sources currently playing in the F3 debug info");
		injectorLogging = this.forgeConfig.getBoolean("Injector Logging", categoryMisc, false,
				"If true, Logs debug info about the injector");

		if (this.forgeConfig.hasChanged()) {
			this.forgeConfig.save();
			return true;
		}
		return false;
	}

}
