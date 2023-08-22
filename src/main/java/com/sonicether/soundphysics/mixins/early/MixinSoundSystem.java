package com.sonicether.soundphysics.mixins.early;

import com.sonicether.soundphysics.SoundPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import paulscode.sound.CommandObject;
import paulscode.sound.CommandThread;
import paulscode.sound.FilenameURL;
import paulscode.sound.SoundSystem;

import java.net.URL;

@Mixin(value = SoundSystem.class, remap = false)
public abstract class MixinSoundSystem {
    @Shadow(remap = false)
    public abstract boolean CommandQueue(CommandObject newCommand);

    @Shadow(remap = false)
    protected CommandThread commandThread;

    /**
     * @author Mist475 (adapted from Daipenger asm)
     * @reason If other mods mess with this they're bound to be incompatible either way
     */
    @Overwrite(remap = false)
    public void newSource(boolean priority, String sourcename, URL url, String identifier, boolean toLoop, float x, float y, float z, int attmodel, float distOrRoll) {
        this.CommandQueue(new CommandObject(10, priority, false, toLoop, sourcename, new FilenameURL(url, identifier), x, y, z, SoundPhysics.attenuationModel, SoundPhysics.globalRolloffFactor));
        this.commandThread.interrupt();
    }

    /**
     * @author Mist475 (adapted from Daipenger asm)
     * @reason If other mods mess with this they're bound to be incompatible either way
     * This overload didn't have asm, just to be sure I've included it here as well, but it probably doesn't get used
     */
    @Overwrite(remap = false)
    public void newSource(boolean priority, String sourcename, String filename,
                          boolean toLoop, float x, float y, float z,
                          int attmodel, float distOrRoll) {
        this.CommandQueue(new CommandObject(10, priority, false, toLoop, sourcename, new FilenameURL(filename), x, y, z, SoundPhysics.attenuationModel, SoundPhysics.globalRolloffFactor));
        this.commandThread.interrupt();
    }
}
