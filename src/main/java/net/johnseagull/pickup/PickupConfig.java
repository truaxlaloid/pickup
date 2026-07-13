package net.johnseagull.pickup;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class PickupConfig {
    public static Configuration config;

    // General settings
    public static boolean NEW_BEHAVIOR = true;
    public static boolean VANILLA_BEHAVIOR = false;
    public static boolean USE_CURRENT_SLOT = true;
    public static boolean NEED_EMPTY_HAND = false;
    public static boolean USE_CUSTOM_REACH = false;
    public static double CUSTOM_REACH = 4.5D;

    // Visual settings
    public static boolean ITEM_TAGS = true;
    public static double OVERLAY_RANGE = 4.5D;
    public static boolean USE_PLAYER_RANGE = true;
    public static int PARTICLES = 5;
    public static boolean ENABLE_PARTICLES = false;
    public static int VISUAL_TICK_RATE = 2;

    // Hitbox settings
    public static boolean ENABLE_MODIFIED_HITBOX = true;
    public static double HITBOX_WIDTH = 0.3D;
    public static double HITBOX_HEIGHT = 0.5D;
    public static boolean HIT_THROUGH_ITEMS = true;

    // Gameplay adjustments
    public static double ITEM_GROUPING_RADIUS = 0.5D;

    public static void init(File configFile) {
        config = new Configuration(configFile);
        syncConfig();
    }

    public static void syncConfig() {
        try {
            config.load();

            NEW_BEHAVIOR = config.getBoolean("newBehavior", "General Settings", true, 
                "Enable right click to pickup - Lets players right click to pick items up instead of walking on top of them");
            VANILLA_BEHAVIOR = config.getBoolean("vanillaBehavior", "General Settings", false, 
                "Vanilla pickup behavior - Lets players also pick up items normally");
            USE_CURRENT_SLOT = config.getBoolean("useCurrentSlot", "General Settings", true, 
                "Pickup to current slot - If right click pickup is enabled, picked up items will be placed in the player's main hand if empty.");
            NEED_EMPTY_HAND = config.getBoolean("needEmptyHand", "General Settings", false, 
                "Need empty hand - If true, players must have an empty main hand to right-click and pick up items");
            USE_CUSTOM_REACH = config.getBoolean("useCustomReach", "General Settings", false, 
                "Use Custom Reach - If true, the mod will use the configured custom reach distance for right-click pickups instead of the player's vanilla entity interaction range");
            CUSTOM_REACH = config.get("General Settings", "customReach", 4.5D, 
                "Custom Reach - The maximum distance (in blocks) from which a player can right-click to pick up items. Only active if [Use Custom Reach] is enabled").getDouble();

            ITEM_TAGS = config.getBoolean("itemTags", "Visual Settings", true, 
                "Item Tags - Shows item details (<name> x<count>) above the targeted item entity");
            OVERLAY_RANGE = config.get("Visual Settings", "overlayRange", 4.5D, 
                "Overlay Range - The number of blocks the player will have to be within in order for tags to display. Only effective if [Use Player's Range] is disabled").getDouble();
            USE_PLAYER_RANGE = config.getBoolean("usePlayerRange", "Visual Settings", true, 
                "Use Player's Range - Use the player's entity interaction range instead of the provided fixed value");
            PARTICLES = config.getInt("particles", "Visual Settings", 5, 0, 100, 
                "Particle count - How many particles to show when picking up an item");
            ENABLE_PARTICLES = config.getBoolean("enableParticles", "Visual Settings", false, 
                "Particles - Show particles when a player picks up an item by right-clicking it");
            VISUAL_TICK_RATE = config.getInt("visualTickRate", "Visual Settings", 2, 1, 20, 
                "Visual Tick Rate - How often (in ticks) the server recalculates item name tags. Increase this value (e.g. 2 or 4) to save server CPU on busy servers. 1 = every tick");

            ENABLE_MODIFIED_HITBOX = config.getBoolean("enableModifiedHitbox", "Hitbox Settings", true, 
                "Modified Hitbox - Modify hitboxes of item entities to make interaction easier");
            HITBOX_WIDTH = config.get("Hitbox Settings", "hitboxWidth", 0.3D, 
                "Width [Diameter] - Diameter of the custom hitbox. Default is 0.25F").getDouble();
            HITBOX_HEIGHT = config.get("Hitbox Settings", "hitboxHeight", 0.5D, 
                "Height - Height of the custom hitbox. Default is 0.25F").getDouble();
            HIT_THROUGH_ITEMS = config.getBoolean("hitThroughItems", "Hitbox Settings", true, 
                "Hit Through Items - If true, left-clicking while looking at an item will pass through to break blocks or hit mobs behind it");

            ITEM_GROUPING_RADIUS = config.get("Gameplay Adjustments", "itemGroupingRadius", 0.5D, 
                "Item Grouping Radius - The horizontal distance (in blocks) at which dropped items will merge together on the ground. Default is 0.5").getDouble();
        } catch (Exception e) {
            // Fallback for configuration failures
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
