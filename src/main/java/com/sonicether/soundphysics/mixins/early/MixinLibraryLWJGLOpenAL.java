package com.sonicether.soundphysics.mixins.early;

import com.sonicether.soundphysics.SoundPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import paulscode.sound.FilenameURL;
import paulscode.sound.SoundBuffer;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

@Mixin(value = LibraryLWJGLOpenAL.class, remap = false)
public abstract class MixinLibraryLWJGLOpenAL {
    @ModifyVariable(method = "loadSound(Lpaulscode/sound/FilenameURL;)Z",
            at = @At(value = "INVOKE", target = "Lpaulscode/sound/ICodec;cleanup()V",
                    shift = At.Shift.AFTER,
                    remap = false),
            remap = false
    )
    private SoundBuffer buffer(SoundBuffer buffer, FilenameURL filenameURL) {
        return SoundPhysics.onLoadSound(buffer, filenameURL.getFilename());
    }
}
