package net.johnseagull.pickup;

import net.neoforged.neoforge.common.ModConfigSpec;

public class PickupConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // General settings
    public static final ModConfigSpec.BooleanValue NEW_BEHAVIOR;
    public static final ModConfigSpec.BooleanValue VANILLA_BEHAVIOR;
    public static final ModConfigSpec.BooleanValue USE_CURRENT_SLOT;
    public static final ModConfigSpec.BooleanValue NEED_EMPTY_HAND;
    public static final ModConfigSpec.BooleanValue USE_CUSTOM_REACH;
    public static final ModConfigSpec.DoubleValue CUSTOM_REACH;

    // Visual settings
    public static final ModConfigSpec.BooleanValue ITEM_GLOW;
    public static final ModConfigSpec.BooleanValue ITEM_TAGS;
    public static final ModConfigSpec.DoubleValue OVERLAY_RANGE;
    public static final ModConfigSpec.BooleanValue USE_PLAYER_RANGE;
    public static final ModConfigSpec.IntValue PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLES;
    public static final ModConfigSpec.IntValue VISUAL_TICK_RATE;

    // Hitbox settings
    public static final ModConfigSpec.BooleanValue ENABLE_MODIFIED_HITBOX;
    public static final ModConfigSpec.DoubleValue HITBOX_WIDTH;
    public static final ModConfigSpec.DoubleValue HITBOX_HEIGHT;
    public static final ModConfigSpec.BooleanValue HIT_THROUGH_ITEMS;

    // Gameplay adjustments
    public static final ModConfigSpec.DoubleValue ITEM_GROUPING_RADIUS;

    static {
        BUILDER.push("General Settings");
        NEW_BEHAVIOR = BUILDER.comment("Enable right click to pickup - Lets players right click to pick items up instead of walking on top of them")
                .define("newBehavior", true);
        VANILLA_BEHAVIOR = BUILDER.comment("Vanilla pickup behavior - Lets players also pick up items normally")
                .define("vanillaBehavior", false);
        USE_CURRENT_SLOT = BUILDER.comment("Pickup to current slot - If right click pickup is enabled, picked up items will be placed in the player's main hand if empty.")
                .define("useCurrentSlot", true);
        NEED_EMPTY_HAND = BUILDER.comment("Need empty hand - If true, players must have an empty main hand to right-click and pick up items")
                .define("needEmptyHand", false);
        USE_CUSTOM_REACH = BUILDER.comment("Use Custom Reach - If true, the mod will use the configured custom reach distance for right-click pickups instead of the player's vanilla entity interaction range")
                .define("useCustomReach", false);
        CUSTOM_REACH = BUILDER.comment("Custom Reach - The maximum distance (in blocks) from which a player can right-click to pick up items. Only active if [Use Custom Reach] is enabled")
                .defineInRange("customReach", 4.5, 1.0, 10.0);
        BUILDER.pop();

        BUILDER.push("Visual Settings");
        ITEM_GLOW = BUILDER.comment("Item Glow - Makes the targeted item entity have a glowing effect")
                .define("itemGlow", false);
        ITEM_TAGS = BUILDER.comment("Item Tags - Shows item details (<name> x<count>) above the targeted item entity")
                .define("itemTags", true);
        OVERLAY_RANGE = BUILDER.comment("Overlay Range - The number of blocks the player will have to be within in order for glow effects/tags to display. Only effective if [Use Player's Range] is disabled")
                .defineInRange("overlayRange", 4.5, -1.0, 32.0);
        USE_PLAYER_RANGE = BUILDER.comment("Use Player's Range - Use the player's entity interaction range instead of the provided fixed value")
                .define("usePlayerRange", true);
        PARTICLES = BUILDER.comment("Particle count - How many particles to show when picking up an item")
                .defineInRange("particles", 5, 0, 100);
        ENABLE_PARTICLES = BUILDER.comment("Particles - Show particles when a player picks up an item by right-clicking it")
                .define("enableParticles", false);
        VISUAL_TICK_RATE = BUILDER.comment("Visual Tick Rate - How often (in ticks) the server recalculates item outlines and name tags. Increase this value (e.g. 2 or 4) to save server CPU on busy servers. 1 = every tick")
                .defineInRange("visualTickRate", 2, 1, 20);
        BUILDER.pop();

        BUILDER.push("Hitbox Settings");
        ENABLE_MODIFIED_HITBOX = BUILDER.comment("Modified Hitbox - Modify hitboxes of item entities to make interaction easier")
                .define("enableModifiedHitbox", true);
        HITBOX_WIDTH = BUILDER.comment("Width [Diameter] - Diameter of the custom hitbox. Default is 0.25F")
                .defineInRange("hitboxWidth", 0.3, 0.1, 1.0);
        HITBOX_HEIGHT = BUILDER.comment("Height - Height of the custom hitbox. Default is 0.25F")
                .defineInRange("hitboxHeight", 0.5, 0.1, 1.0);
        HIT_THROUGH_ITEMS = BUILDER.comment("Hit Through Items - If true, left-clicking while looking at an item will pass through to break blocks or hit mobs behind it")
                .define("hitThroughItems", true);
        BUILDER.pop();

        BUILDER.push("Gameplay Adjustments");
        ITEM_GROUPING_RADIUS = BUILDER.comment("Item Grouping Radius - The horizontal distance (in blocks) at which dropped items will merge together on the ground. Default is 0.5")
                .defineInRange("itemGroupingRadius", 0.5, 0.0, 16.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
