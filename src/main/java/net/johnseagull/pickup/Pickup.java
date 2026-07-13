package net.johnseagull.pickup;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

@Mod(modid = Pickup.MODID, name = "Pickup", version = "1.0", acceptableRemoteVersions = "*")
public class Pickup {
    public static final String MODID = "pickup";
    private int serverTicks = 0;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PickupConfig.init(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static float getReach(EntityPlayer player) {
        if (PickupConfig.USE_CUSTOM_REACH) {
            return (float) PickupConfig.CUSTOM_REACH;
        }
        return player.capabilities.isCreativeMode ? 5.0F : 3.0F;
    }

    public static EntityItem getTargetedItem(EntityPlayerMP player, float range) {
        Vec3 eyePos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 lookAngle = player.getLook(1.0F);
        Vec3 end = eyePos.addVector(lookAngle.xCoord * range, lookAngle.yCoord * range, lookAngle.zCoord * range);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eyePos, end, false, true, false);
        Vec3 newEnd = (hit != null && hit.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) ? hit.hitVec : end;

        AxisAlignedBB searchBox = player.getEntityBoundingBox()
                .addCoord(lookAngle.xCoord * range, lookAngle.yCoord * range, lookAngle.zCoord * range)
                .expand(1.0D, 1.0D, 1.0D);
                
        List<EntityItem> items = player.worldObj.getEntitiesWithinAABB(EntityItem.class, searchBox);
        EntityItem coolItem = null;
        double cd = 99999.0D;

        for (EntityItem item : items) {
            MovingObjectPosition clipResult = item.getEntityBoundingBox().calculateIntercept(eyePos, newEnd);
            if (clipResult != null) {
                double dist = clipResult.hitVec.distanceTo(eyePos);
                if (dist < cd) {
                    cd = dist;
                    coolItem = item;
                }
            }
        }
        return coolItem;
    }

    private boolean tryPickup(EntityPlayerMP player, EntityItem item) {
        if (item == null || item.isDead) return false;

        if (PickupConfig.NEED_EMPTY_HAND && player.getHeldItem() != null) {
            return false;
        }

        ItemStack itemStack = item.getEntityItem();
        if (itemStack == null || itemStack.stackSize <= 0) return false;

        int originalCount = itemStack.stackSize;
        boolean addedAny = false;

        if (player.getHeldItem() == null && PickupConfig.USE_CURRENT_SLOT) {
            player.setCurrentItemOrArmor(0, itemStack.copy());
            itemStack.stackSize = 0;
            addedAny = true;
        } else {
            if (player.inventory.addItemStackToInventory(itemStack)) {
                addedAny = true;
            }
        }

        if (!addedAny) return false;

        player.worldObj.playSoundAtEntity(player, "random.pop", 0.2F, ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);

        int pickedUpCount = originalCount - itemStack.stackSize;

        if (PickupConfig.ENABLE_PARTICLES) {
            if (player.worldObj instanceof WorldServer) {
                WorldServer serverWorld = (WorldServer) player.worldObj;
                serverWorld.spawnParticle(
                        EnumParticleTypes.ITEM_CRACK,
                        item.posX, item.posY + (double)(item.height / 2.0F), item.posZ,
                        PickupConfig.PARTICLES,
                        (Math.random() - 0.5) * 0.2,
                        Math.random() * 0.2,
                        (Math.random() - 0.5) * 0.2,
                        0.05,
                        Item.getIdFromItem(itemStack.getItem()), itemStack.getMetadata()
                );
            }
        }

        player.onItemPickup(item, pickedUpCount);
        player.triggerAchievement(StatList.getObjectsPickUpStats(itemStack.getItem()));

        if (itemStack.stackSize <= 0) {
            item.setDead();
        } else {
            item.setEntityItemStack(itemStack);
        }

        return true;
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            if (PickupConfig.NEW_BEHAVIOR && event.entityPlayer instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
                float range = getReach(player);
                EntityItem item = getTargetedItem(player, range);
                if (item != null && tryPickup(player, item)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
        if (PickupConfig.NEW_BEHAVIOR && event.target instanceof EntityItem && event.entityPlayer instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
            if (tryPickup(player, (EntityItem) event.target)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        serverTicks++;
        if (serverTicks % PickupConfig.VISUAL_TICK_RATE != 0) return;

        List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            float range = PickupConfig.USE_PLAYER_RANGE ? getReach(player) : (float) PickupConfig.OVERLAY_RANGE;
            EntityItem coolItem = getTargetedItem(player, range);

            AxisAlignedBB searchBox = player.getEntityBoundingBox()
                    .addCoord(player.getLook(1.0F).xCoord * range, player.getLook(1.0F).yCoord * range, player.getLook(1.0F).zCoord * range)
                    .expand(1.0D, 1.0D, 1.0D);
            List<EntityItem> items = player.worldObj.getEntitiesWithinAABB(EntityItem.class, searchBox);

            for (EntityItem item : items) {
                ((ItemEntityInterface) item).pickup$setPickup(!PickupConfig.NEW_BEHAVIOR);
                ((ItemEntityInterface) item).pickup$setBigHitbox(PickupConfig.ENABLE_MODIFIED_HITBOX);

                if (item == coolItem) {
                    if (PickupConfig.ITEM_TAGS) {
                        String name = item.getEntityItem().getDisplayName() + " x" + item.getEntityItem().stackSize;
                        if (item.getCustomNameTag() == null || !item.getCustomNameTag().equals(name)) {
                            item.setCustomNameTag(name);
                        }
                        if (!item.getAlwaysRenderNameTag()) {
                            item.setAlwaysRenderNameTag(true);
                        }
                    }
                } else {
                    if (item.getAlwaysRenderNameTag()) {
                        item.setCustomNameTag("");
                        item.setAlwaysRenderNameTag(false);
                    }
                }
            }
        }
    }
}
