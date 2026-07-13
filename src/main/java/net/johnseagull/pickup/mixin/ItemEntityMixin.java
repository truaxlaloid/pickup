package net.johnseagull.pickup.mixin;

import net.johnseagull.pickup.ItemEntityInterface;
import net.johnseagull.pickup.PickupConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityItem.class)
public abstract class ItemEntityMixin extends Entity implements ItemEntityInterface {
    
    @Shadow private int delayBeforeCanPickup;
    @Shadow private String owner;

    @Unique public boolean canPickup = false;
    @Unique public boolean bigHitbox = false;

    public ItemEntityMixin(World worldIn) {
        super(worldIn);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    private void onInit(World worldIn, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        updateCustomDimensions();
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void onUpdateTick(CallbackInfo ci) {
        updateCustomDimensions();
    }

    @Unique
    private void updateCustomDimensions() {
        if (PickupConfig.ENABLE_MODIFIED_HITBOX && this.bigHitbox) {
            float width = (float) PickupConfig.HITBOX_WIDTH;
            float height = (float) PickupConfig.HITBOX_HEIGHT;
            if (this.width != width || this.height != height) {
                this.setSize(width, height);
            }
        } else {
            if (this.width != 0.25F || this.height != 0.25F) {
                this.setSize(0.25F, 0.25F);
            }
        }
    }

    @Overwrite
    public void onCollideWithPlayer(EntityPlayer entityIn) {
        if (this.canPickup || PickupConfig.VANILLA_BEHAVIOR) {
            if (!this.worldObj.isRemote) {
                ItemStack itemstack = ((EntityItem) (Object) this).getEntityItem();
                int i = itemstack.stackSize;

                if (this.delayBeforeCanPickup == 0 && (this.owner == null || 6000 - ((EntityItem)(Object)this).age <= 200 || this.owner.equals(entityIn.getName())) && entityIn.inventory.addItemStackToInventory(itemstack)) {
                    this.worldObj.playSoundAtEntity(entityIn, "random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    entityIn.onItemPickup(((EntityItem) (Object) this), i);

                    if (itemstack.stackSize <= 0) {
                        this.setDead();
                    }
                }
            }
        }
    }

    @Redirect(method = "searchForOtherItemsNearby", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/AxisAlignedBB;expand(DDD)Lnet/minecraft/util/AxisAlignedBB;"))
    private AxisAlignedBB redirectExpand(AxisAlignedBB instance, double x, double y, double z) {
        double radius = PickupConfig.ITEM_GROUPING_RADIUS;
        return instance.expand(radius, y, radius);
    }

    @Override
    public void pickup$setPickup(boolean value) {
        this.canPickup = value;
    }

    @Override
    public void pickup$setBigHitbox(boolean value) {
        this.bigHitbox = value;
        updateCustomDimensions();
    }

    @Override
    public boolean pickup$bigHitbox() {
        return this.bigHitbox;
    }
}
