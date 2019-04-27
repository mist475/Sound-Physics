package com.sonicether.soundphysics;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Collections;
import java.nio.FloatBuffer;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;
import org.lwjgl.openal.EFX10;
import org.lwjgl.BufferUtils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.audio.SoundCategory;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Text;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundBuffer;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;  
import java.util.TimerTask;

import org.objectweb.asm.Type;

@Mod(modid = SoundPhysics.modid, acceptedMinecraftVersions = SoundPhysics.mcVersion, version = SoundPhysics.version, guiFactory = "com.sonicether.soundphysics.SPGuiFactory",
	dependencies="before:computronics") // Dependencies to make sure that SP's config is loaded before patching Computronics
public class SoundPhysics {

	public static final String modid = "soundphysics";
	public static final String version = "1.0.6";
	public static final String mcVersion = "1.7.10";

	private static final Pattern rainPattern = Pattern.compile(".*rain.*");
	private static final Pattern stepPattern = Pattern.compile(".*step.*");
	private static final Pattern blockPattern = Pattern.compile(".*block.*");
	private static final Pattern uiPattern = Pattern.compile(".*\\/ui\\/.*");
	private static final Pattern clickPattern = Pattern.compile(".*random.click.*");
	private static final Pattern noteBlockPattern = Pattern.compile(".*note/.*");

	@Mod.EventHandler
	public void preInit(final FMLPreInitializationEvent event) {
		Config.instance.preInit(event);
	}

	@Mod.EventHandler
	public void init(final FMLInitializationEvent event) {
		Config.instance.init(event);
	}

	private static final String logPrefix = "[SOUND PHYSICS]";
	private static int auxFXSlot0;
	private static int auxFXSlot1;
	private static int auxFXSlot2;
	private static int auxFXSlot3;
	private static int reverb0;
	private static int reverb1;
	private static int reverb2;
	private static int reverb3;
	private static int directFilter0;
	private static int sendFilter0;
	private static int sendFilter1;
	private static int sendFilter2;
	private static int sendFilter3;

	private static Minecraft mc;

	private static SoundCategory lastSoundCategory;
	private static String lastSoundName;

	private static ProcThread proc_thread;
	private static volatile boolean thread_alive;
	private static volatile boolean thread_signal_death;
	private static volatile List<Source> source_list;

	// THESE VARIABLES ARE CONSTANTLY ACCESSED AND USED BY ASM INJECTED CODE! DO
	// NOT REMOVE!
	public static int attenuationModel = SoundSystemConfig.ATTENUATION_ROLLOFF;
	public static float globalRolloffFactor = Config.rolloffFactor;
	public static float globalVolumeMultiplier = 4.0f;
	public static float globalReverbMultiplier = 0.7f * Config.globalReverbGain;
	public static double soundDistanceAllowance = Config.soundDistanceAllowance;

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void init() {
		setupEFX();
		mc = Minecraft.getMinecraft();
		setupThread();
	}

	public static class Source {
		public int sourceID;
		public float posX;
		public float posY;
		public float posZ;
		public SoundCategory category;
		public String name;
		public int frequency;
		public int size;
		public int bufferID;

		public Source(int sid,float px,float py,float pz,SoundCategory cat,String n) {
			this.sourceID = sid;
			this.posX = px;
			this.posY = py;
			this.posZ = pz;
			this.category = cat;
			this.name = n;
			bufferID = AL10.alGetSourcei(sid, AL10.AL_BUFFER);
			size = AL10.alGetBufferi(bufferID, AL10.AL_SIZE);
			frequency = AL10.alGetBufferi(bufferID, AL10.AL_FREQUENCY);
		}
	}

	public static class ProcThread extends Thread {
		@Override
		public synchronized void run() {
			while (thread_alive) {
				while (!Config.dynamicEnvironementEvalutaion) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						logError(String.valueOf(e));
					}
				}
				synchronized (source_list) {
					//log("Updating env " + String.valueOf(source_list.size()));
					ListIterator<Source> iter = source_list.listIterator();
					while (iter.hasNext()) {
						Source source = iter.next();
						//log("Updating sound '" + source.name + "' SourceID:" + String.valueOf(source.sourceID));
						//boolean pl = sndHandler.isSoundPlaying(source.sound);
						//FloatBuffer pos = BufferUtils.createFloatBuffer(3);
						//AL10.alGetSource(source.sourceID,AL10.AL_POSITION,pos);
						//To try ^
						int state = AL10.alGetSourcei(source.sourceID, AL10.AL_SOURCE_STATE);
						//int byteoff = AL10.alGetSourcei(source.sourceID, AL11.AL_BYTE_OFFSET);
						//boolean finished = source.size == byteoff;
						if (state == AL10.AL_PLAYING) {
							FloatBuffer pos = BufferUtils.createFloatBuffer(3);
							AL10.alGetSource(source.sourceID,AL10.AL_POSITION,pos);
							source.posX = pos.get(0);
							source.posY = pos.get(1);
							source.posZ = pos.get(2);
							evaluateEnvironment(source.sourceID,source.posX,source.posY,source.posZ,source.category,source.name);
						} else /*if (state == AL10.AL_STOPPED)*/ {
							iter.remove();
						}
					}
				}
				try {
					Thread.sleep(1000/Config.dynamicEnvironementEvalutaionFrequency);
				} catch (Exception e) {
					logError(String.valueOf(e));
				}
			}
			thread_signal_death = true;
		}
	}

	public static boolean source_check(Source s) {
		synchronized (source_list) {
			ListIterator<Source> iter = source_list.listIterator();
			while (iter.hasNext()) {
				Source sn = iter.next();
				if (sn.sourceID == s.sourceID && sn.bufferID == s.bufferID &&
					sn.posX == s.posX && sn.posY == s.posY && sn.posZ == s.posZ) {
					return true;
				}
			}
		}
		return false;
	}

	/*@Mod.EventBusSubscriber
	public static class DebugDisplayEventHandler {
		@SubscribeEvent
		public static void onDebugOverlay(RenderGameOverlayEvent.Text event)
		{
			if(mc.gameSettings.showDebugInfo && Config.dynamicEnvironementEvalutaion && Config.debugInfoShow) {
				event.getLeft().add("");
				event.getLeft().add("[SoundPhysics] "+String.valueOf(source_list.size())+" Sources");
				event.getLeft().add("[SoundPhysics] Source list :");
				synchronized (source_list) {
					ListIterator<Source> iter = source_list.listIterator();
					while (iter.hasNext())  {
						Source s = iter.next();
						Vec3d tmp = new Vec3d(s.posX,s.posY,s.posZ);
						event.getLeft().add(String.valueOf(s.sourceID)+"-"+s.category.toString()+"-"+s.name+"-"+tmp.toString());
						/*int buffq = AL10.alGetSourcei(s.sourceID, AL10.AL_BUFFERS_QUEUED);
						int buffp = AL10.alGetSourcei(s.sourceID, AL10.AL_BUFFERS_PROCESSED);
						int sampoff = AL10.alGetSourcei(s.sourceID, AL11.AL_SAMPLE_OFFSET);
						int byteoff = AL10.alGetSourcei(s.sourceID, AL11.AL_BYTE_OFFSET);
						String k = "";
						if (sampoff!=0) {
							//k = String.valueOf(sampoff)+"/"+String.valueOf((byteoff/sampoff)*size)+" ";
							k = String.valueOf((float)sampoff/(float)s.frequency)+"/"+String.valueOf((float)((byteoff/sampoff)*s.size)/(float)s.frequency)+" ";
						} else {
							k = "0/? ";
						}
						event.getLeft().add(k+String.valueOf(buffp)+"/"+String.valueOf(buffq)+" "+String.valueOf(s.bufferID));
						event.getLeft().add("----");
					}
				}
			}
		}
	}*/

	private static synchronized void setupThread() {
		if (source_list == null) source_list = Collections.synchronizedList(new ArrayList<Source>());
		else source_list.clear();

		/*if (proc_thread != null) {
			thread_signal_death = false;
			thread_alive = false;
			while (!thread_signal_death);
		}*/
		if (proc_thread == null) {
			proc_thread = new ProcThread();
			thread_alive = true;
			proc_thread.start();
		}
	}

	public static void applyConfigChanges() {
		globalRolloffFactor = Config.rolloffFactor;
		globalReverbMultiplier = 0.7f * Config.globalReverbGain;
		soundDistanceAllowance = Config.soundDistanceAllowance;

		if (auxFXSlot0 != 0) {
			// Set the global reverb parameters and apply them to the effect and
			// effectslot
			setReverbParams(ReverbParams.getReverb0(), auxFXSlot0, reverb0);
			setReverbParams(ReverbParams.getReverb1(), auxFXSlot1, reverb1);
			setReverbParams(ReverbParams.getReverb2(), auxFXSlot2, reverb2);
			setReverbParams(ReverbParams.getReverb3(), auxFXSlot3, reverb3);
		}
	}

	private static void setupEFX() {
		// Get current context and device
		final ALCcontext currentContext = ALC10.alcGetCurrentContext();
		final ALCdevice currentDevice = ALC10.alcGetContextsDevice(currentContext);

		if (ALC10.alcIsExtensionPresent(currentDevice, "ALC_EXT_EFX")) {
			log("EFX Extension recognized.");
		} else {
			logError("EFX Extension not found on current device. Aborting.");
			return;
		}

		// Create auxiliary effect slots
		auxFXSlot0 = EFX10.alGenAuxiliaryEffectSlots();
		log("Aux slot " + auxFXSlot0 + " created");
		EFX10.alAuxiliaryEffectSloti(auxFXSlot0, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);

		auxFXSlot1 = EFX10.alGenAuxiliaryEffectSlots();
		log("Aux slot " + auxFXSlot1 + " created");
		EFX10.alAuxiliaryEffectSloti(auxFXSlot1, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);

		auxFXSlot2 = EFX10.alGenAuxiliaryEffectSlots();
		log("Aux slot " + auxFXSlot2 + " created");
		EFX10.alAuxiliaryEffectSloti(auxFXSlot2, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);

		auxFXSlot3 = EFX10.alGenAuxiliaryEffectSlots();
		log("Aux slot " + auxFXSlot3 + " created");
		EFX10.alAuxiliaryEffectSloti(auxFXSlot3, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
		checkErrorLog("Failed creating auxiliary effect slots!");

		reverb0 = EFX10.alGenEffects();
		EFX10.alEffecti(reverb0, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB);
		checkErrorLog("Failed creating reverb effect slot 0!");
		reverb1 = EFX10.alGenEffects();
		EFX10.alEffecti(reverb1, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB);
		checkErrorLog("Failed creating reverb effect slot 1!");
		reverb2 = EFX10.alGenEffects();
		EFX10.alEffecti(reverb2, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB);
		checkErrorLog("Failed creating reverb effect slot 2!");
		reverb3 = EFX10.alGenEffects();
		EFX10.alEffecti(reverb3, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB);
		checkErrorLog("Failed creating reverb effect slot 3!");

		// Create filters
		directFilter0 = EFX10.alGenFilters();
		EFX10.alFilteri(directFilter0, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);

		sendFilter0 = EFX10.alGenFilters();
		EFX10.alFilteri(sendFilter0, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);

		sendFilter1 = EFX10.alGenFilters();
		EFX10.alFilteri(sendFilter1, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);

		sendFilter2 = EFX10.alGenFilters();
		EFX10.alFilteri(sendFilter2, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);

		sendFilter3 = EFX10.alGenFilters();
		EFX10.alFilteri(sendFilter3, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);
		checkErrorLog("Error creating lowpass filters!");

		applyConfigChanges();
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void setLastSoundCategory(final SoundCategory sc) {
		lastSoundCategory = sc;
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void setLastSoundName(final String soundName) {
		lastSoundName = soundName;
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	// For sounds that get played normally
	public static void onPlaySound(final float posX, final float posY, final float posZ, final int sourceID) {
		onPlaySound(posX, posY, posZ, sourceID, lastSoundCategory, lastSoundName);
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	// For sounds that get played using OpenAL directly or just not using the minecraft sound system
	public static void onPlaySoundAL(final float posX, final float posY, final float posZ, final int sourceID) {
		onPlaySound(posX, posY, posZ, sourceID, SoundCategory.BLOCKS, "null");
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void onPlaySound(final float posX, final float posY, final float posZ, final int sourceID, SoundCategory soundCat, String soundName) {
		//log(String.valueOf(posX)+" "+String.valueOf(posY)+" "+String.valueOf(posZ)+" - "+String.valueOf(sourceID)+" - "+soundCat.toString()+" - "+soundName);
		if (Config.noteBlockEnable && soundCat == SoundCategory.RECORDS && noteBlockPattern.matcher(soundName).matches()) soundCat = SoundCategory.BLOCKS;
		evaluateEnvironment(sourceID, posX, posY, posZ,soundCat,soundName);
		if (!Config.dynamicEnvironementEvalutaion) return;
		if ((mc.thePlayer == null | mc.theWorld == null | posY <= 0 | soundCat == SoundCategory.RECORDS 
		| soundCat == SoundCategory.MUSIC) || (Config.skipRainOcclusionTracing && rainPattern.matcher(soundName).matches())) return;
		Source tmp = new Source(sourceID,posX,posY,posZ,soundCat,soundName);
		if (source_check(tmp)) return;
		source_list.add(tmp);
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static SoundBuffer onLoadSound(SoundBuffer buff, String filename) {
		if (buff == null || buff.audioFormat.getChannels() == 1 || !Config.autoSteroDownmix) return buff;
		if (mc.thePlayer == null | mc.theWorld == null | lastSoundCategory == SoundCategory.RECORDS 
		| lastSoundCategory == SoundCategory.MUSIC | uiPattern.matcher(filename).matches() | clickPattern.matcher(filename).matches()) {
			if (Config.autoSteroDownmixLogging) log("Not converting sound '"+filename+"'("+buff.audioFormat.toString()+")");
			return buff;
		}
		AudioFormat orignalformat = buff.audioFormat;
		int bits = orignalformat.getSampleSizeInBits();
		boolean bigendian = orignalformat.isBigEndian();
		AudioFormat monoformat = new AudioFormat(orignalformat.getEncoding(), orignalformat.getSampleRate(), bits,
												1, orignalformat.getFrameSize(), orignalformat.getFrameRate(), bigendian);
		if (Config.autoSteroDownmixLogging) log("Converting sound '"+filename+"'("+orignalformat.toString()+") to mono ("+monoformat.toString()+")");

		ByteBuffer bb = ByteBuffer.wrap(buff.audioData,0,buff.audioData.length);
		bb.order(bigendian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		if (bits == 8) {
			for (int i = 0; i < buff.audioData.length; i+=2) {
				bb.put(i/2,(byte)((bb.get(i)+bb.get(i+1))/2));
			}
		} else if (bits == 16) {
			for (int i = 0; i < buff.audioData.length; i+=4) {
				bb.putShort((i/2),(short)((bb.getShort(i)+bb.getShort(i+2))/2));
			}
		}
		buff.audioFormat = monoformat;
		buff.trimData(buff.audioData.length/2);
		return buff;
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static double calculateEntitySoundOffset(final Entity entity, final String name) {
		if (name != null) {
			if (stepPattern.matcher(name).matches()) {
				return 0;
			}
		}
		return entity.getEyeHeight();
	}

	// Unused
	private static int isSnowingAt(Vec3 position)
	{
		return isSnowingAt(position, true);
	}

	// Copy of isRainingAt (1.12.2)
	private static int isSnowingAt(Vec3 position, boolean check_rain)
	{
		if (check_rain && !mc.theWorld.isRaining()) {
			return 0;
		}
		else if (!mc.theWorld.canBlockSeeTheSky((int)position.xCoord,(int)position.yCoord,(int)position.zCoord))
		{
			return 0;
		}
		else if (mc.theWorld.getPrecipitationHeight((int)position.xCoord,(int)position.zCoord) > position.yCoord)
		{
			return 0;
		}
		else
		{
			/*boolean cansnow = mc.theWorld.canSnowAt(position, false);
			if (mc.theWorld.getBiome(position).getEnableSnow() && cansnow) return true;
			else if (cansnow) return true;
			else return false;*/
								//canSnowAt() but the name isn't there
			return (mc.theWorld.func_147478_e((int)position.xCoord,(int)position.yCoord,(int)position.zCoord, false) | 
				mc.theWorld.getBiomeGenForCoords((int)position.xCoord,(int)position.zCoord).getEnableSnow()) ? 1 : 0;
		}
	}

	@SuppressWarnings("deprecation")
	private static float getBlockReflectivity(final Block block) {
		final Block.SoundType soundType = block.stepSound;
		final Material blockMaterial = block.getMaterial();

		float reflectivity = 0.5f;

		if (soundType == Block.soundTypeStone || soundType == Block.soundTypePiston) {
			reflectivity = Config.stoneReflectivity;
		} else if (soundType == Block.soundTypeWood) {
			reflectivity = Config.woodReflectivity;
		} else if (soundType == Block.soundTypeGravel || soundType == Block.soundTypeGrass) {
			if (blockMaterial == Material.plants || blockMaterial == Material.vine
				|| blockMaterial == Material.grass || blockMaterial == Material.leaves) reflectivity = Config.plantReflectivity;
			else reflectivity = Config.groundReflectivity;
		} else if (soundType == Block.soundTypeMetal) {
			reflectivity = Config.metalReflectivity;
		} else if (soundType == Block.soundTypeGlass) {
			reflectivity = Config.glassReflectivity;
		} else if (soundType == Block.soundTypeCloth) {
			reflectivity = Config.clothReflectivity;
		} else if (soundType == Block.soundTypeSand) {
			reflectivity = Config.sandReflectivity;
		} else if (soundType == Block.soundTypeSnow) {
			reflectivity = Config.snowReflectivity;
		} else if (soundType == Block.soundTypeLadder) {
			reflectivity = Config.woodReflectivity;
		} else if (soundType == Block.soundTypeAnvil) {
			reflectivity = Config.metalReflectivity;
		}

		reflectivity *= Config.globalBlockReflectance;

		return reflectivity;
	}

	/*private static EnumFacing getFacingFromSide(final int side) {
		switch (side) {
			case 0: return EnumFacing.DOWN;
			case 1: return EnumFacing.UP;
			case 2: return EnumFacing.EAST;
			case 3: return EnumFacing.WEST;
			case 4: return EnumFacing.NORTH;
			case 5: return EnumFacing.SOUTH;
			default: return EnumFacing.UP;
		}
	}*/

	private static Vec3 getNormalFromFacing(final int sideHit) {
		EnumFacing facing = EnumFacing.getFront(sideHit);//getFacingFromSide(sideHit);
		return Vec3.createVectorHelper(facing.getFrontOffsetX(),facing.getFrontOffsetY(),facing.getFrontOffsetZ());
	}

	private static Vec3 reflect(final Vec3 dir, final Vec3 normal) {
		final double dot2 = dir.dotProduct(normal) * 2;

		final double x = dir.xCoord - dot2 * normal.xCoord;
		final double y = dir.yCoord - dot2 * normal.yCoord;
		final double z = dir.zCoord - dot2 * normal.zCoord;

		return Vec3.createVectorHelper(x, y, z);
	}

	private static Vec3 offsetSoundByName(final double soundX, final double soundY, final double soundZ,
			final Vec3 playerPos, final String name, final SoundCategory category) {
		double offsetX = 0.0;
		double offsetY = 0.0;
		double offsetZ = 0.0;
		double offsetTowardsPlayer = 0.0;

		double tempNormX = 0;
		double tempNormY = 0;
		double tempNormZ = 0;

		if (soundY % 1.0 < 0.001 || stepPattern.matcher(name).matches()) {
			offsetY = 0.13;
		}

		if (category == SoundCategory.BLOCKS || blockPattern.matcher(name).matches() || 
			!mc.theWorld.isAirBlock((int)Math.floor(soundX),(int)Math.floor(soundY),(int)Math.floor(soundZ))) {
			// The ray will probably hit the block that it's emitting from
			// before
			// escaping. Offset the ray start position towards the player by the
			// diagonal half length of a cube

			tempNormX = playerPos.xCoord - soundX;
			tempNormY = playerPos.yCoord - soundY;
			tempNormZ = playerPos.zCoord - soundZ;
			final double length = Math.sqrt(tempNormX * tempNormX + tempNormY * tempNormY + tempNormZ * tempNormZ);
			tempNormX /= length;
			tempNormY /= length;
			tempNormZ /= length;
			// 0.867 > square root of 0.5^2 * 3
			offsetTowardsPlayer = 0.867;
			offsetX += tempNormX * offsetTowardsPlayer;
			offsetY += tempNormY * offsetTowardsPlayer;
			offsetZ += tempNormZ * offsetTowardsPlayer;
		}

		return Vec3.createVectorHelper(soundX + offsetX, soundY + offsetY, soundZ + offsetZ);
	}

	@SuppressWarnings("deprecation")
	private static void evaluateEnvironment(final int sourceID, final float posX, final float posY, final float posZ, final SoundCategory category, final String name) {
		if (mc.thePlayer == null | mc.theWorld == null | posY <= 0 | category == SoundCategory.RECORDS
				| category == SoundCategory.MUSIC) {
			// posY <= 0 as a condition has to be there: Ingame
			// menu clicks do have a player and world present
			setEnvironment(sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
			return;
		}

		final boolean isRain = rainPattern.matcher(name).matches();

		if (Config.skipRainOcclusionTracing && isRain) {
			setEnvironment(sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
			return;
		}

		float directCutoff = 1.0f;
		final float absorptionCoeff = Config.globalBlockAbsorption * 3.0f;

		final Vec3 playerPos = Vec3.createVectorHelper(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
		final Vec3 soundPos = offsetSoundByName(posX, posY, posZ, playerPos, name, category);
		final Vec3 normalToPlayer = playerPos.subtract(soundPos).normalize();

		float snowFactor = 0.0f;

		if (mc.theWorld.isRaining()) {
			final Vec3 middlePos = playerPos.addVector(soundPos.xCoord,soundPos.yCoord,soundPos.zCoord);
			middlePos.xCoord = middlePos.xCoord*0.5d;
			middlePos.yCoord = middlePos.yCoord*0.5d;
			middlePos.zCoord = middlePos.zCoord*0.5d;
			final int snowingPlayer = isSnowingAt(playerPos,false);
			final int snowingSound = isSnowingAt(soundPos,false);
			final int snowingMiddle = isSnowingAt(middlePos,false);
			snowFactor = snowingPlayer * 0.25f + snowingMiddle * 0.5f + snowingSound * 0.25f;
		}

		float airAbsorptionFactor = 1.0f;

		if (snowFactor > 0.0f) {
			airAbsorptionFactor = Math.max(Config.snowAirAbsorptionFactor*mc.theWorld.getRainStrength(1.0f)*snowFactor,airAbsorptionFactor);
		}

		/*final double distance = playerPos.distanceTo(soundPos);
		final double time = (distance/343.3)*1000;
		AL10.alSourcePause(sourceID);
		log("paused, time "+String.valueOf(time));

		new java.util.Timer().schedule( 
			new java.util.TimerTask() {
				@Override
				public void run() {
					log("play, time "+String.valueOf(time));
					AL10.alSourcePlay(sourceID);
				}
			}, 
			(long)time 
		);*/

		Vec3 rayOrigin = soundPos;

		float occlusionAccumulation = 0.0f;

		for (int i = 0; i < 10; i++) {
			final MovingObjectPosition rayHit = mc.theWorld.rayTraceBlocks(rayOrigin, playerPos, true);

			if (rayHit == null) {
				break;
			}

			final Block blockHit = mc.theWorld.getBlock(rayHit.blockX,rayHit.blockY,rayHit.blockZ);

			float blockOcclusion = 1.0f;

			if (!blockHit.isOpaqueCube()) {
				// log("not a solid block!");
				blockOcclusion *= 0.15f;
			}

			occlusionAccumulation += blockOcclusion;

			rayOrigin = Vec3.createVectorHelper(rayHit.hitVec.xCoord + normalToPlayer.xCoord * 0.1, rayHit.hitVec.yCoord + normalToPlayer.yCoord * 0.1,
					rayHit.hitVec.zCoord + normalToPlayer.zCoord * 0.1);
		}

		directCutoff = (float) Math.exp(-occlusionAccumulation * absorptionCoeff);
		float directGain = (float) Math.pow(directCutoff, 0.1);

		// Calculate reverb parameters for this sound
		float sendGain0 = 0.0f;
		float sendGain1 = 0.0f;
		float sendGain2 = 0.0f;
		float sendGain3 = 0.0f;

		float sendCutoff0 = 1.0f;
		float sendCutoff1 = 1.0f;
		float sendCutoff2 = 1.0f;
		float sendCutoff3 = 1.0f;

		if (mc.thePlayer.isInsideOfMaterial(Material.water)) {
			directCutoff *= 1.0f - Config.underwaterFilter;
		}

		if (isRain) {
			setEnvironment(sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2,
					sendCutoff3, directCutoff, directGain, airAbsorptionFactor);
			return;
		}

		// Shoot rays around sound
		final float phi = 1.618033988f;
		final float gAngle = phi * (float) Math.PI * 2.0f;
		final float maxDistance = 256.0f;

		final int numRays = Config.environmentEvaluationRays;
		final int rayBounces = Config.environmentEvaluationRaysBounces;

		final float[] bounceReflectivityRatio = new float[rayBounces];

		float sharedAirspace = 0.0f;

		final float rcpTotalRays = 1.0f / (numRays * rayBounces);
		final float rcpPrimaryRays = 1.0f / numRays;

		for (int i = 0; i < numRays; i++) {
			final float fi = i;
			final float fiN = fi / numRays;
			final float longitude = gAngle * fi;
			final float latitude = (float) Math.asin(fiN * 2.0f - 1.0f);

			final Vec3 rayDir = Vec3.createVectorHelper(Math.cos(latitude) * Math.cos(longitude),
					Math.cos(latitude) * Math.sin(longitude), Math.sin(latitude));

			final Vec3 rayStart = Vec3.createVectorHelper(soundPos.xCoord, soundPos.yCoord, soundPos.zCoord);

			final Vec3 rayEnd = Vec3.createVectorHelper(rayStart.xCoord + rayDir.xCoord * maxDistance, rayStart.yCoord + rayDir.yCoord * maxDistance,
					rayStart.zCoord + rayDir.zCoord * maxDistance);

			final MovingObjectPosition rayHit = mc.theWorld.rayTraceBlocks(rayStart, rayEnd, true);

			if (rayHit != null) {
				final double rayLength = soundPos.distanceTo(rayHit.hitVec);

				// Additional bounces
				Block lastHitBlock = mc.theWorld.getBlock(rayHit.blockX,rayHit.blockY,rayHit.blockZ);
				Vec3 lastHitPos = rayHit.hitVec;
				Vec3 lastHitNormal = getNormalFromFacing(rayHit.sideHit);
				Vec3 lastRayDir = rayDir;

				float totalRayDistance = (float) rayLength;

				// Secondary ray bounces
				for (int j = 0; j < rayBounces; j++) {
					final Vec3 newRayDir = reflect(lastRayDir, lastHitNormal);
					// Vec3 newRayDir = lastHitNormal;
					final Vec3 newRayStart = Vec3.createVectorHelper(lastHitPos.xCoord + lastHitNormal.xCoord * 0.01,
							lastHitPos.yCoord + lastHitNormal.yCoord * 0.01, lastHitPos.zCoord + lastHitNormal.zCoord * 0.01);
					final Vec3 newRayEnd = Vec3.createVectorHelper(newRayStart.xCoord + newRayDir.xCoord * maxDistance,
							newRayStart.yCoord + newRayDir.yCoord * maxDistance, newRayStart.zCoord + newRayDir.zCoord * maxDistance);

					final MovingObjectPosition newRayHit = mc.theWorld.rayTraceBlocks(newRayStart, newRayEnd, true);

					float energyTowardsPlayer = 0.25f;
					final float blockReflectivity = getBlockReflectivity(lastHitBlock);
					energyTowardsPlayer *= blockReflectivity * 0.75f + 0.25f;

					if (newRayHit == null) {
						totalRayDistance += lastHitPos.distanceTo(playerPos);
					} else {
						final double newRayLength = lastHitPos.distanceTo(newRayHit.hitVec);

						bounceReflectivityRatio[j] += blockReflectivity;

						totalRayDistance += newRayLength;

						lastHitPos = newRayHit.hitVec;
						lastHitNormal = getNormalFromFacing(newRayHit.sideHit);
						lastRayDir = newRayDir;
						lastHitBlock = mc.theWorld.getBlock(newRayHit.blockX,newRayHit.blockY,newRayHit.blockZ);

						// Cast one final ray towards the player. If it's
						// unobstructed, then the sound source and the player
						// share airspace.
						if (Config.simplerSharedAirspaceSimulation && j == rayBounces - 1
								|| !Config.simplerSharedAirspaceSimulation) {
							final Vec3 finalRayStart = Vec3.createVectorHelper(lastHitPos.xCoord + lastHitNormal.xCoord * 0.01,
									lastHitPos.yCoord + lastHitNormal.yCoord * 0.01, lastHitPos.zCoord + lastHitNormal.zCoord * 0.01);

							final MovingObjectPosition finalRayHit = mc.theWorld.rayTraceBlocks(finalRayStart, playerPos, true);

							if (finalRayHit == null) {
								// log("Secondary ray hit the player!");
								sharedAirspace += 1.0f;
							}
						}
					}

					final float reflectionDelay = (float) Math.max(totalRayDistance, 0.0) * 0.12f * blockReflectivity;

					final float cross0 = 1.0f - MathHelper.clamp_float(Math.abs(reflectionDelay - 0.0f), 0.0f, 1.0f);
					final float cross1 = 1.0f - MathHelper.clamp_float(Math.abs(reflectionDelay - 1.0f), 0.0f, 1.0f);
					final float cross2 = 1.0f - MathHelper.clamp_float(Math.abs(reflectionDelay - 2.0f), 0.0f, 1.0f);
					final float cross3 = MathHelper.clamp_float(reflectionDelay - 2.0f, 0.0f, 1.0f);

					sendGain0 += cross0 * energyTowardsPlayer * 6.4f * rcpTotalRays;
					sendGain1 += cross1 * energyTowardsPlayer * 12.8f * rcpTotalRays;
					sendGain2 += cross2 * energyTowardsPlayer * 12.8f * rcpTotalRays;
					sendGain3 += cross3 * energyTowardsPlayer * 12.8f * rcpTotalRays;

					// Nowhere to bounce off of, stop bouncing!
					if (newRayHit == null) {
						break;
					}
				}
			}

		}

		// log("total reflectivity ratio: " + totalReflectivityRatio);

		bounceReflectivityRatio[0] = bounceReflectivityRatio[0] / numRays;
		bounceReflectivityRatio[1] = bounceReflectivityRatio[1] / numRays;
		bounceReflectivityRatio[2] = bounceReflectivityRatio[2] / numRays;
		bounceReflectivityRatio[3] = bounceReflectivityRatio[3] / numRays;

		sharedAirspace *= 64.0f;

		if (Config.simplerSharedAirspaceSimulation) {
			sharedAirspace *= rcpPrimaryRays;
		} else {
			sharedAirspace *= rcpTotalRays;
		}

		final float sharedAirspaceWeight0 = MathHelper.clamp_float(sharedAirspace / 20.0f, 0.0f, 1.0f);
		final float sharedAirspaceWeight1 = MathHelper.clamp_float(sharedAirspace / 15.0f, 0.0f, 1.0f);
		final float sharedAirspaceWeight2 = MathHelper.clamp_float(sharedAirspace / 10.0f, 0.0f, 1.0f);
		final float sharedAirspaceWeight3 = MathHelper.clamp_float(sharedAirspace / 10.0f, 0.0f, 1.0f);

		sendCutoff0 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.0f) * (1.0f - sharedAirspaceWeight0)
				+ sharedAirspaceWeight0;
		sendCutoff1 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.0f) * (1.0f - sharedAirspaceWeight1)
				+ sharedAirspaceWeight1;
		sendCutoff2 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.5f) * (1.0f - sharedAirspaceWeight2)
				+ sharedAirspaceWeight2;
		sendCutoff3 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.5f) * (1.0f - sharedAirspaceWeight3)
				+ sharedAirspaceWeight3;

		// attempt to preserve directionality when airspace is shared by
		// allowing some of the dry signal through but filtered
		final float averageSharedAirspace = (sharedAirspaceWeight0 + sharedAirspaceWeight1 + sharedAirspaceWeight2
				+ sharedAirspaceWeight3) * 0.25f;
		directCutoff = Math.max((float) Math.pow(averageSharedAirspace, 0.5) * 0.2f, directCutoff);

		directGain = (float) Math.pow(directCutoff, 0.1);

		sendGain1 *= bounceReflectivityRatio[1];
		sendGain2 *= (float) Math.pow(bounceReflectivityRatio[2], 3.0);
		sendGain3 *= (float) Math.pow(bounceReflectivityRatio[3], 4.0);

		sendGain0 = MathHelper.clamp_float(sendGain0, 0.0f, 1.0f);
		sendGain1 = MathHelper.clamp_float(sendGain1, 0.0f, 1.0f);
		sendGain2 = MathHelper.clamp_float(sendGain2 * 1.05f - 0.05f, 0.0f, 1.0f);
		sendGain3 = MathHelper.clamp_float(sendGain3 * 1.05f - 0.05f, 0.0f, 1.0f);

		sendGain0 *= (float) Math.pow(sendCutoff0, 0.1);
		sendGain1 *= (float) Math.pow(sendCutoff1, 0.1);
		sendGain2 *= (float) Math.pow(sendCutoff2, 0.1);
		sendGain3 *= (float) Math.pow(sendCutoff3, 0.1);

		if (mc.thePlayer.isInWater()) {
			sendCutoff0 *= 0.4f;
			sendCutoff1 *= 0.4f;
			sendCutoff2 *= 0.4f;
			sendCutoff3 *= 0.4f;
		}

		setEnvironment(sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2,
				sendCutoff3, directCutoff, directGain, airAbsorptionFactor);
	}

	private static void setEnvironment(final int sourceID, final float sendGain0, final float sendGain1,
			final float sendGain2, final float sendGain3, final float sendCutoff0, final float sendCutoff1,
			final float sendCutoff2, final float sendCutoff3, final float directCutoff, final float directGain,
			final float airAbsorptionFactor) {
		// Set reverb send filter values and set source to send to all reverb fx
		// slots
		EFX10.alFilterf(sendFilter0, EFX10.AL_LOWPASS_GAIN, sendGain0);
		EFX10.alFilterf(sendFilter0, EFX10.AL_LOWPASS_GAINHF, sendCutoff0);
		AL11.alSource3i(sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, auxFXSlot0, 0, sendFilter0);

		EFX10.alFilterf(sendFilter1, EFX10.AL_LOWPASS_GAIN, sendGain1);
		EFX10.alFilterf(sendFilter1, EFX10.AL_LOWPASS_GAINHF, sendCutoff1);
		AL11.alSource3i(sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, auxFXSlot1, 1, sendFilter1);

		EFX10.alFilterf(sendFilter2, EFX10.AL_LOWPASS_GAIN, sendGain2);
		EFX10.alFilterf(sendFilter2, EFX10.AL_LOWPASS_GAINHF, sendCutoff2);
		AL11.alSource3i(sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, auxFXSlot2, 2, sendFilter2);

		EFX10.alFilterf(sendFilter3, EFX10.AL_LOWPASS_GAIN, sendGain3);
		EFX10.alFilterf(sendFilter3, EFX10.AL_LOWPASS_GAINHF, sendCutoff3);
		AL11.alSource3i(sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, auxFXSlot3, 3, sendFilter3);

		EFX10.alFilterf(directFilter0, EFX10.AL_LOWPASS_GAIN, directGain);
		EFX10.alFilterf(directFilter0, EFX10.AL_LOWPASS_GAINHF, directCutoff);
		AL10.alSourcei(sourceID, EFX10.AL_DIRECT_FILTER, directFilter0);

		AL10.alSourcef(sourceID, EFX10.AL_AIR_ABSORPTION_FACTOR, MathHelper.clamp_float(Config.airAbsorption * airAbsorptionFactor,0.0f,10.0f));
	}

	/**
	 * Applies the parameters in the enum ReverbParams to the main reverb
	 * effect.
	 */
	protected static void setReverbParams(final ReverbParams r, final int auxFXSlot, final int reverbSlot) {
		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DENSITY, r.density);
		checkErrorLog("Error while assigning reverb density: " + r.density);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DIFFUSION, r.diffusion);
		checkErrorLog("Error while assigning reverb diffusion: " + r.diffusion);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_GAIN, r.gain);
		checkErrorLog("Error while assigning reverb gain: " + r.gain);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_GAINHF, r.gainHF);
		checkErrorLog("Error while assigning reverb gainHF: " + r.gainHF);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DECAY_TIME, r.decayTime);
		checkErrorLog("Error while assigning reverb decayTime: " + r.decayTime);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DECAY_HFRATIO, r.decayHFRatio);
		checkErrorLog("Error while assigning reverb decayHFRatio: " + r.decayHFRatio);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_REFLECTIONS_GAIN, r.reflectionsGain);
		checkErrorLog("Error while assigning reverb reflectionsGain: " + r.reflectionsGain);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_LATE_REVERB_GAIN, r.lateReverbGain);
		checkErrorLog("Error while assigning reverb lateReverbGain: " + r.lateReverbGain);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_LATE_REVERB_DELAY, r.lateReverbDelay);
		checkErrorLog("Error while assigning reverb lateReverbDelay: " + r.lateReverbDelay);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, r.airAbsorptionGainHF);
		checkErrorLog("Error while assigning reverb airAbsorptionGainHF: " + r.airAbsorptionGainHF);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, r.roomRolloffFactor);
		checkErrorLog("Error while assigning reverb roomRolloffFactor: " + r.roomRolloffFactor);

		// Attach updated effect object
		EFX10.alAuxiliaryEffectSloti(auxFXSlot, EFX10.AL_EFFECTSLOT_EFFECT, reverbSlot);
	}

	public static void log(final String message) {
		System.out.println(logPrefix.concat(" : ").concat(message));
	}

	public static void logError(final String errorMessage) {
		System.out.println(logPrefix.concat(" [ERROR] : ").concat(errorMessage));
	}

	protected static boolean checkErrorLog(final String errorMessage) {
		final int error = AL10.alGetError();
		if (error == AL10.AL_NO_ERROR) {
			return false;
		}

		String errorName;

		switch (error) {
		case AL10.AL_INVALID_NAME:
			errorName = "AL_INVALID_NAME";
			break;
		case AL10.AL_INVALID_ENUM:
			errorName = "AL_INVALID_ENUM";
			break;
		case AL10.AL_INVALID_VALUE:
			errorName = "AL_INVALID_VALUE";
			break;
		case AL10.AL_INVALID_OPERATION:
			errorName = "AL_INVALID_OPERATION";
			break;
		case AL10.AL_OUT_OF_MEMORY:
			errorName = "AL_OUT_OF_MEMORY";
			break;
		default:
			errorName = Integer.toString(error);
			break;
		}

		logError(errorMessage + " OpenAL error " + errorName);
		return true;
	}

}
