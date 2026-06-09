package net.johnseagull.pickup;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@Mod(Pickup.MODID)
public class Pickup {
    public static final String MODID = "pickup";
    public static final Logger LOGGER = LoggerFactory.getLogger("Pickup");

    public Pickup(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Pickup");

        // Register NeoForge Native config
        modContainer.registerConfig(ModConfig.Type.COMMON, PickupConfig.SPEC);

        // Register events to game bus
        NeoForge.EVENT_BUS.register(this);
    }

    public static ItemEntity getTargetedItem(ServerPlayer player, float range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 end = eyePos.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePos, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 newEnd = (hit.getType() != HitResult.Type.MISS) ? hit.getLocation() : end;

        List<ItemEntity> items = player.level().getEntitiesOfClass(
                ItemEntity.class,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(range))
        );
        ItemEntity coolItem = null;
        float cd = 99999;
        for (ItemEntity item : items) {
            var clipResult = item.getBoundingBox().clip(eyePos, newEnd);
            if (clipResult.isPresent()) {
                float dist = (float) clipResult.get().distanceTo(eyePos);
                if (dist < cd) {
                    cd = dist;
                    coolItem = item;
                }
            }
        }
        return coolItem;
    }

    private boolean tryPickup(ServerPlayer player, ItemEntity item, InteractionHand hand) {
        if (item == null || !item.isAlive()) return false;

        // Check hand restriction configurations
        if (PickupConfig.NEED_EMPTY_HAND.get() && !player.getMainHandItem().isEmpty()) {
            return false;
        }

        ItemStack itemStack = item.getItem();
        if (itemStack.isEmpty()) return false;

        int originalCount = itemStack.getCount();
        boolean addedAny = false;

        // Process inventory checks before playing sound or discarding the entity
        if (player.getMainHandItem().isEmpty() && PickupConfig.USE_CURRENT_SLOT.get()) {
            player.setItemInHand(hand, itemStack.copy());
            itemStack.setCount(0);
            addedAny = true;
        } else {
            // Standard vanilla inventory insertion (returns true if at least 1 item was added)
            if (player.getInventory().add(itemStack)) {
                addedAny = true;
            }
        }

        // If the inventory is completely full and didn't accept any items, block the pickup
        if (!addedAny) {
            return false;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, player.getSoundSource(), 1.0F, 1.0F);

        int pickedUpCount = originalCount - itemStack.getCount();

        if (PickupConfig.ENABLE_PARTICLES.get()) {
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, item.getItem()),
                        item.getX(), item.getY() + (item.getBbHeight() / 2), item.getZ(),
                        PickupConfig.PARTICLES.get(),
                        (Math.random() - 0.5) * 0.2,
                        Math.random() * 0.2,
                        (Math.random() - 0.5) * 0.2,
                        0.05
                );
            }
        }

        player.take(item, pickedUpCount);
        player.onItemPickup(item);
        player.awardStat(Stats.ITEM_PICKED_UP.get(itemStack.getItem()), pickedUpCount);

        // Only discard the item entity if the entire stack was picked up.
        // Otherwise, keep the entity on the ground with the updated stack size.
        if (itemStack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(itemStack);
        }

        return true;
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && PickupConfig.NEW_BEHAVIOR.get()) {
            if (event.getTarget() instanceof ItemEntity item && event.getEntity() instanceof ServerPlayer player) {
                if (tryPickup(player, item, event.getHand())) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    // Handles picking up items when clicking on block occlusions (grass, flowers, ground)
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && PickupConfig.NEW_BEHAVIOR.get() && event.getEntity() instanceof ServerPlayer player) {
            float range = PickupConfig.OVERLAY_RANGE.get().floatValue();
            if (PickupConfig.USE_PLAYER_RANGE.get()) {
                range = (float) player.entityInteractionRange();
            }
            ItemEntity item = getTargetedItem(player, range);
            if (item != null) {
                if (tryPickup(player, item, event.getHand())) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    // Handles picking up items when right-clicking the air (while holding an item)
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && PickupConfig.NEW_BEHAVIOR.get() && event.getEntity() instanceof ServerPlayer player) {
            float range = PickupConfig.OVERLAY_RANGE.get().floatValue();
            if (PickupConfig.USE_PLAYER_RANGE.get()) {
                range = (float) player.entityInteractionRange();
            }
            ItemEntity item = getTargetedItem(player, range);
            if (item != null) {
                if (tryPickup(player, item, event.getHand())) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            float range = PickupConfig.OVERLAY_RANGE.get().floatValue();
            if (PickupConfig.USE_PLAYER_RANGE.get()) {
                range = (float) player.entityInteractionRange();
            }

            ItemEntity coolItem = getTargetedItem(player, range);

            List<ItemEntity> items = player.level().getEntitiesOfClass(
                    ItemEntity.class,
                    player.getBoundingBox().expandTowards(player.getLookAngle().scale(range))
            );

            for (ItemEntity item : items) {
                ((ItemEntityInterface) item).pickup$setPickup(!PickupConfig.NEW_BEHAVIOR.get());
                ((ItemEntityInterface) item).pickup$setBigHitbox(PickupConfig.ENABLE_MODIFIED_HITBOX.get());
                item.refreshDimensions();

                if (item == coolItem) {
                    if (PickupConfig.ITEM_TAGS.get()) {
                        Component name = Component.empty()
                                .append(item.getItem().getHoverName())
                                .append(Component.literal(" x" + item.getItem().getCount()).withStyle(ChatFormatting.GRAY));
                        item.setCustomName(name);
                        item.setCustomNameVisible(true);
                    }
                    if (PickupConfig.ITEM_GLOW.get()) {
                        item.setGlowingTag(true);
                    }
                } else {
                    item.setCustomName(Component.literal(""));
                    item.setCustomNameVisible(false);
                    item.setGlowingTag(false);
                }
            }
        }
    }
}
