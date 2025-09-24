package net.minex.customname.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minex.customname.ItemNameChanger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemDataStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ItemData> STORED_ITEMS = new ConcurrentHashMap<>();
    private static final File STORAGE_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "item-name-changer");
    private static String currentWorldKey = "";
    
    public static class ItemData {
        public String displayName;
        public long timestamp;
        
        public ItemData(String displayName) {
            this.displayName = displayName;
            this.timestamp = System.currentTimeMillis();
        }
        
        public ItemData() {} // For Gson
    }
    
    static {
        if (!STORAGE_DIR.exists()) {
            STORAGE_DIR.mkdirs();
        }
    }
    
    /**
     * Generate a unique key for an item based on its properties
     */
    public static String generateItemKey(ItemStack stack) {
        if (stack.isEmpty()) return null;
        
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(stack.getItem().toString());
        keyBuilder.append("_");
        keyBuilder.append(stack.getCount());
        
        // Include item components for unique identification (excluding custom ones)
        int componentsHash = 0;
        
        // Get the item's damage/durability if it has any
        if (stack.isDamageable()) {
            componentsHash += stack.getDamage();
        }
        
        // Get the item's enchantments if any
        if (stack.hasEnchantments()) {
            componentsHash += stack.getEnchantments().toString().hashCode();
        }
        
        // Add other non-custom components to make items more unique
        componentsHash += stack.getItem().hashCode();
        
        keyBuilder.append("_");
        keyBuilder.append(Math.abs(componentsHash));
        
        return keyBuilder.toString();
    }
    
    /**
     * Store item data for the current world
     */
    public static void storeItem(ItemStack stack, String displayName) {
        String itemKey = generateItemKey(stack);
        if (itemKey == null) return;
        
        String fullKey = getCurrentWorldKey() + ":" + itemKey;
        STORED_ITEMS.put(fullKey, new ItemData(displayName));
        
        // Save to disk asynchronously
        saveDataAsync();
    }
    
    /**
     * Get stored item data for the current world
     */
    public static ItemData getStoredItem(ItemStack stack) {
        String itemKey = generateItemKey(stack);
        if (itemKey == null) return null;
        
        String fullKey = getCurrentWorldKey() + ":" + itemKey;
        return STORED_ITEMS.get(fullKey);
    }
    
    /**
     * Remove item from storage
     */
    public static void removeItem(ItemStack stack) {
        String itemKey = generateItemKey(stack);
        if (itemKey == null) return;
        
        String fullKey = getCurrentWorldKey() + ":" + itemKey;
        STORED_ITEMS.remove(fullKey);
        
        saveDataAsync();
    }
    
    /**
     * Get current world key for storage separation
     */
    private static String getCurrentWorldKey() {
        if (currentWorldKey.isEmpty()) {
            updateWorldKey();
        }
        return currentWorldKey;
    }
    
    /**
     * Update the world key when joining a new world/server
     */
    public static void updateWorldKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() != null) {
            // Multiplayer server
            currentWorldKey = "server_" + client.getCurrentServerEntry().address.replace(":", "_");
        } else if (client.getServer() != null && client.getServer().isSingleplayer()) {
            // Singleplayer world
            String worldName = client.getServer().getSaveProperties().getLevelName();
            currentWorldKey = "world_" + worldName;
        } else {
            currentWorldKey = "unknown";
        }
        
        // Load data for this world
        loadData();
    }
    
    /**
     * Save data to disk
     */
    private static void saveDataAsync() {
        // Run on a separate thread to avoid blocking the game
        new Thread(() -> {
            try {
                File dataFile = new File(STORAGE_DIR, "item_data.json");
                try (FileWriter writer = new FileWriter(dataFile)) {
                    GSON.toJson(STORED_ITEMS, writer);
                }
            } catch (IOException e) {
                ItemNameChanger.LOGGER.error("Failed to save item data", e);
            }
        }).start();
    }
    
    /**
     * Load data from disk
     */
    private static void loadData() {
        try {
            File dataFile = new File(STORAGE_DIR, "item_data.json");
            if (dataFile.exists()) {
                try (FileReader reader = new FileReader(dataFile)) {
                    Type type = new TypeToken<Map<String, ItemData>>() {}.getType();
                    Map<String, ItemData> loadedData = GSON.fromJson(reader, type);
                    if (loadedData != null) {
                        STORED_ITEMS.clear();
                        STORED_ITEMS.putAll(loadedData);
                    }
                }
            }
        } catch (IOException e) {
            ItemNameChanger.LOGGER.error("Failed to load item data", e);
        }
    }
    
    /**
     * Clean up old entries (older than 30 days)
     */
    public static void cleanup() {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
        STORED_ITEMS.entrySet().removeIf(entry -> entry.getValue().timestamp < thirtyDaysAgo);
        saveDataAsync();
    }
    
    /**
     * Clear all data for current world
     */
    public static void clearCurrentWorld() {
        String worldPrefix = getCurrentWorldKey() + ":";
        STORED_ITEMS.entrySet().removeIf(entry -> entry.getKey().startsWith(worldPrefix));
        saveDataAsync();
    }
}