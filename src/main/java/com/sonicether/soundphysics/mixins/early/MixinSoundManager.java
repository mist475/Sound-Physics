package com.sonicether.soundphysics.mixins.early;

import com.sonicether.soundphysics.SoundPhysics;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {

    @Inject(method = "playSound(Lnet/minecraft/client/audio/ISound;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/SoundManager$SoundSystemStarterThread;play(Ljava/lang/String;)V", shift = At.Shift.AFTER, remap = false),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void setLastNameAndCategory(ISound p_148611_1_, CallbackInfo ci, SoundEventAccessorComposite soundeventaccessorcomposite, SoundPoolEntry soundpoolentry, float f, float f1, SoundCategory soundcategory, float f2, double d0, ResourceLocation resourcelocation, boolean flag, String s) {
        SoundPhysics.setLastSoundName(resourcelocation.toString());
        SoundPhysics.setLastSoundCategory(soundcategory);
    }

    @Redirect(method = "playSound(Lnet/minecraft/client/audio/ISound;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/SoundManager;getNormalizedVolume(Lnet/minecraft/client/audio/ISound;Lnet/minecraft/client/audio/SoundPoolEntry;Lnet/minecraft/client/audio/SoundCategory;)F"))
    private float multiplyVolumeByGlobalMultiplier(SoundManager instance, ISound p_148594_1_, SoundPoolEntry p_148594_2_, SoundCategory p_148594_3_) {
        return this.getNormalizedVolume(p_148594_1_, p_148594_2_, p_148594_3_) * SoundPhysics.globalVolumeMultiplier;
    }

    @Shadow
    protected abstract float getNormalizedVolume(ISound p_148594_1_, SoundPoolEntry p_148594_2_, SoundCategory p_148594_3_);
}
