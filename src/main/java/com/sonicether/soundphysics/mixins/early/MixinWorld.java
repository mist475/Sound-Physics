package com.sonicether.soundphysics.mixins.early;

import com.sonicether.soundphysics.SoundPhysics;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(World.class)
public abstract class MixinWorld {

    @ModifyArgs(method = "playSoundAtEntity(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorldAccess;playSound(Ljava/lang/String;DDDFF)V"))
    private void playSoundWithOffsetEntity(Args args, Entity p_72956_1_, String p_72956_2_, float p_72956_3_, float p_72956_4_) {
        double temp = args.get(2);
        temp += SoundPhysics.calculateEntitySoundOffset(p_72956_1_, p_72956_2_);
        args.set(2, temp);
    }

    /**
     * Disabled in the asm, might have to disable here as well
     */
    @ModifyArgs(method = "playSoundToNearExcept(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorldAccess;playSoundToNearExcept(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;DDDFF)V"))
    private void playSoundWithOffsetPlayer(Args args, EntityPlayer p_85173_1_, String p_85173_2_, float p_85173_3_, float p_85173_4_) {
        double temp = args.get(2);
        //Calls non-existent method calculateEntitySoundOffsetPlayer in asm, probably from 1.12.2 version
        temp += SoundPhysics.calculateEntitySoundOffset(p_85173_1_, p_85173_2_);
        args.set(2, temp);
    }
}
