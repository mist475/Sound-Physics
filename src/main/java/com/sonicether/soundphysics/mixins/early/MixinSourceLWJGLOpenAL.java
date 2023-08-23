package com.sonicether.soundphysics.mixins.early;

import com.sonicether.soundphysics.Config;
import com.sonicether.soundphysics.SoundPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.Channel;
import paulscode.sound.FilenameURL;
import paulscode.sound.SoundBuffer;
import paulscode.sound.Source;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;
import paulscode.sound.libraries.SourceLWJGLOpenAL;

@Mixin(value = SourceLWJGLOpenAL.class, remap = false)
public abstract class MixinSourceLWJGLOpenAL extends Source {
    @Shadow(remap = false)
    private ChannelLWJGLOpenAL channelOpenAL;

    public MixinSourceLWJGLOpenAL(boolean priority, boolean toStream, boolean toLoop, String sourcename, FilenameURL filenameURL, SoundBuffer soundBuffer, float x, float y, float z, int attModel, float distOrRoll, boolean temporary) {
        super(priority, toStream, toLoop, sourcename, filenameURL, soundBuffer, x, y, z, attModel, distOrRoll, temporary);
    }

    @Inject(method = "play(Lpaulscode/sound/Channel;)V",
            at = @At(value = "INVOKE", target = "Lpaulscode/sound/Channel;play()V", shift = At.Shift.AFTER, remap = false),
            remap = false)
    private void injectOnPlaySound(Channel c, CallbackInfo ci) {
        //Couldn't figure out how to load option in time so this will do for now
        if (Config.autoSteroDownmix) {
            SoundPhysics.onPlaySound(this.position.x, this.position.y, this.position.z, this.channelOpenAL.ALSource.get(0));
        }
    }
}
