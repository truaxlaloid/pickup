package net.johnseagull.pickup;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && PickupConfig.NEW_BEHAVIOR.get()) {
            if (event.getTarget() instanceof ItemEntity item) {
                var player = event.getEntity();
                var level = event.getLevel();
                var hand = event.getHand();

                player.playSound(SoundEvents.ITEM_PICKUP);
                if (PickupConfig.ENABLE_PARTICLES.get()) {
                    for (int i = 0; i < PickupConfig.PARTICLES.get(); i++) {
                        level.addParticle(
                                new ItemParticleOption(ParticleTypes.ITEM, item.getItem()),
                                true, true,
                                item.getX(), item.getY() + (item.getBbHeight() / 2), item.getZ(),
                                (Math.random() - 0.5) * 0.2,
                                Math.random() * 0.2,
                                (Math.random() - 0.5) * 0.2
                        );
                    }
                }
                player.onItemPickup(item);
                if (player.getMainHandItem().isEmpty() && PickupConfig.USE_CURRENT_SLOT.get()) {
                    item.discard();
                    if (player.getInventory().hasAnyOf(Set.of(item.getItem().getItem()))) {
                        player.addItem(item.getItem());
                    } else {
                        player.setItemInHand(hand, item.getItem());
                    }
                    player.awardStat(Stats.ITEM_PICKED_UP.get(item.getItem().getItem()), item.getItem().getCount());
                } else {
                    item.discard();
                    player.awardStat(Stats.ITEM_PICKED_UP.get(item.getItem().getItem()), item.getItem().getCount());
                    player.addItem(item.getItem());
                }

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        float range = PickupConfig.OVERLAY_RANGE.get().floatValue();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (PickupConfig.USE_PLAYER_RANGE.get()) {
                range = (float) player.entityInteractionRange();
            }
            Vec3 eyePos = player.getEyePosition();
            Vec3 end = eyePos.add(player.getLookAngle().scale(range));
            BlockHitResult hit = player.level().clip(new ClipContext(
                    eyePos, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY,
                    player
            ));
            Vec3 newEnd;
            ItemEntity coolItem = null;
            if (hit.getType() == HitResult.Type.BLOCK) {
                newEnd = hit.getLocation();
            } else if (hit.getType() == HitResult.Type.ENTITY) {
                newEnd = hit.getLocation();
            } else {
                newEnd = end;
            }
            List<ItemEntity> items = player.level().getEntitiesOfClass(
                    ItemEntity.class,
                    player.getBoundingBox().expandTowards(player.getLookAngle().scale(range))
            );
            float cd = 99999;
            for (ItemEntity item : items) {
                ((ItemEntityInterface) item).pickup$setPickup(!PickupConfig.NEW_BEHAVIOR.get());
                ((ItemEntityInterface) item).pickup$setBigHitbox(PickupConfig.ENABLE_MODIFIED_HITBOX.get());
                item.refreshDimensions();

                if (item.getBoundingBox().clip(eyePos, newEnd).isPresent()) {
                    float dist = (float) item.getBoundingBox().clip(eyePos, newEnd).get().distanceTo(eyePos);
                    if (dist < cd) {
                        cd = dist;
                        coolItem = item;
                    }
                } else {
                    item.setCustomName(Component.literal(""));
                    item.setCustomNameVisible(false);
                    item.setGlowingTag(false);
                }
            }
            for (ItemEntity item : items) {
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
