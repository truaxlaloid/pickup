package net.johnseagull.pickup.mixin;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundInteractPacket.class)
public abstract class ServerboundInteractPacketMixin {
    @Shadow
    private int entityId;

    private static final ThreadLocal<Boolean> IN_DISPATCH = ThreadLocal.withInitial(() -> false);

    @Inject(method = "dispatch", at = @At("HEAD"), cancellable = true)
    private void onDispatch(ServerboundInteractPacket.Handler handler, CallbackInfo ci) {
        // Prevent infinite recursion loops
        if (IN_DISPATCH.get()) {
            return;
        }

        IN_DISPATCH.set(true);
        try {
            // Create a custom handler wrapper to intercept the onAttack() call
            ServerboundInteractPacket.Handler customHandler = new ServerboundInteractPacket.Handler() {
                @Override
                public void onInteraction(net.minecraft.world.InteractionHand hand) {
                    handler.onInteraction(hand);
                }

                @Override
                public void onInteraction(net.minecraft.world.InteractionHand hand, Vec3 interactionLocation) {
                    handler.onInteraction(hand, interactionLocation);
                }

                @Override
                public void onAttack() {
                    Entity target = null;
                    var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        for (var level : server.getAllLevels()) {
                            Entity entity = level.getEntity(entityId);
                            if (entity != null) {
                                target = entity;
                                break;
                            }
                        }
                    }

                    if (target instanceof ItemEntity) {
                        // Suppress the attack logic on the server to bypass the disconnect check
                    } else {
                        handler.onAttack();
                    }
                }
            };

            // Re-dispatch using the custom wrapper and cancel the original check
            ((ServerboundInteractPacket) (Object) this).dispatch(customHandler);
            ci.cancel();
        } finally {
            IN_DISPATCH.set(false);
        }
    }
}
