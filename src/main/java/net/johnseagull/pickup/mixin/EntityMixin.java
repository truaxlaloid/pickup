package net.johnseagull.pickup.mixin;

import net.johnseagull.pickup.PickupConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void onCanBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof EntityItem) {
            boolean isClient = ((Entity) (Object) this).worldObj.isRemote;
            if (isClient && PickupConfig.HIT_THROUGH_ITEMS) {
                cir.setReturnValue(false);
            } else {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "hitByEntity", at = @At("HEAD"), cancellable = true)
    private void onHitByEntity(Entity attacker, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof EntityItem) {
            cir.setReturnValue(true);
        }
    }
}
