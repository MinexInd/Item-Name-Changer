package net.minex.customname.sync;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minex.customname.ItemNameChanger;
import net.minex.customname.storage.ItemDataStorage;
import net.minex.customname.util.ItemModifier;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InventorySyncManager {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ItemNameChanger-InventorySync");
        t.setDaemon(true);
        return t;
    });
    
    private static boolean isRunning = false;
    
    /**
     * Start monitoring inventory for items that need restoration
     */
    public static void start() {
        if (isRunning) return;
        isRunning = true;
        
        // Check inventory every 500ms for items that need restoration
        EXECUTOR.scheduleWithFixedDelay(InventorySyncManager::checkAndRestoreItems, 1, 500, TimeUnit.MILLISECONDS);
        
        ItemNameChanger.LOGGER.info("Inventory sync manager started");
    }
    
    /**
     * Stop the inventory monitoring
     */
    public static void stop() {
        isRunning = false;
        EXECUTOR.shutdown();
        ItemNameChanger.LOGGER.info("Inventory sync manager stopped");
    }
    
    /**
     * Check all inventory items and restore custom names/model data if needed
     */
    private static void checkAndRestoreItems() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            
            if (player == null || client.world == null) return;
            
            // Check main inventory
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                restoreItemIfNeeded(stack);
            }
            
            // Check offhand
            ItemStack offhand = player.getOffHandStack();
            restoreItemIfNeeded(offhand);
            
        } catch (Exception e) {
            // Silently catch exceptions to avoid spamming logs
            // Only log if debug mode is enabled
            if (System.getProperty("itemnamechanger.debug") != null) {
                ItemNameChanger.LOGGER.debug("Error during inventory sync: " + e.getMessage());
            }
        }
    }
    
    /**
     * Restore an item's custom name if it exists in storage
     * but is missing from the actual item
     */
    private static void restoreItemIfNeeded(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        ItemDataStorage.ItemData storedData = ItemDataStorage.getStoredItem(stack);
        if (storedData == null) return;
        
        // Check if display name needs restoration
        if (storedData.displayName != null && !ItemModifier.hasCustomName(stack)) {
            // Restore the item's custom name
            ItemModifier.setDisplayName(stack, storedData.displayName);
        }
    }
    
    /**
     * Manually trigger a full inventory restore
     */
    public static void restoreAllItems() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) return;
        
        int restoredCount = 0;
        
        // Restore main inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (restoreItemForcefully(stack)) {
                restoredCount++;
            }
        }
        
        // Restore offhand
        if (restoreItemForcefully(player.getOffHandStack())) {
            restoredCount++;
        }
        
        if (restoredCount > 0) {
            ItemNameChanger.LOGGER.info("Restored {} items from storage", restoredCount);
        }
    }
    
    /**
     * Force restore an item regardless of current state
     */
    private static boolean restoreItemForcefully(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        ItemDataStorage.ItemData storedData = ItemDataStorage.getStoredItem(stack);
        if (storedData == null) return false;
        
        if (storedData.displayName != null) {
            ItemModifier.setDisplayName(stack, storedData.displayName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if the sync manager is running
     */
    public static boolean isRunning() {
        return isRunning;
    }
}
