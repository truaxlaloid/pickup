package net.johnseagull.pickup.mixin;

import net.johnseagull.pickup.PickupConfig;
import net.johnseagull.pickup.ItemEntityInterface;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public class ItemEntityMixin implements ItemEntityInterface {
    @Shadow
    private int pickupDelay;
    @Shadow
    private @Nullable UUID target;

    @Unique
    @Mutable
    public boolean canPickup = false;

    @Unique
    @Mutable
    public boolean bigHitbox = false;

    /**
     * @author JohnSeagull
     * @reason Overwrite usual pickup behavior
     */
    @Overwrite
    public void playerTouch(final Player player) {
        if (this.canPickup || PickupConfig.VANILLA_BEHAVIOR.get()) {
            if (!((ItemEntity) (Object) this).level().isClientSide()) {
                ItemStack itemStack = ((ItemEntity) (Object) this).getItem();
                Item item = itemStack.getItem();
                int orgCount = itemStack.getCount();
                if (this.pickupDelay == 0 && (this.target == null || this.target.equals(player.getUUID())) && player.getInventory().add(itemStack)) {
                    player.take(((ItemEntity) (Object) this), orgCount);
                    if (itemStack.isEmpty()) {
                        ((ItemEntity) (Object) this).discard();
                        itemStack.setCount(orgCount);
                    }
                    player.awardStat(Stats.ITEM_PICKED_UP.get(item), orgCount);
                    player.onItemPickup(((ItemEntity) (Object) this));
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        ((ItemEntity)(Object)this).refreshDimensions();
    }

    @Override
    public void pickup$setPickup(boolean value) {
        this.canPickup = value;
    }

    @Override
    public void pickup$setBigHitbox(boolean value) {
        this.bigHitbox = value;
        ((ItemEntity)(Object)this).refreshDimensions();
    }

    @Override
    public boolean pickup$bigHitbox() {
        return this.bigHitbox;
    }
}
