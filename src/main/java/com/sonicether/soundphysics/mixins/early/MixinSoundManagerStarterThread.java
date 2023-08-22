package com.sonicether.soundphysics.mixins.early;

import com.sonicether.soundphysics.SoundPhysics;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

@Mixin(targets = "net/minecraft/client/audio/SoundManager$SoundSystemStarterThread")
public abstract class MixinSoundManagerStarterThread extends SoundSystem {
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void initSoundPhysics(SoundManager manager, CallbackInfo info) {
        SoundPhysics.init();
    }
}
