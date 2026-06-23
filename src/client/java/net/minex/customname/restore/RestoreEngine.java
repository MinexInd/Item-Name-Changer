package net.minex.customname.restore;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.minex.customname.core.CustomNameManager;
import net.minex.customname.matching.ItemFingerprint;
import net.minex.customname.storage.StorageManager;
import net.minex.customname.storage.StoredItem;

import java.util.List;

public class RestoreEngine {

    private static final Minecraft client = Minecraft.getInstance();
    private static int inventorySlotCursor = 9;
    private static int tickCounter = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> tick());
        System.out.println("[CustomName] RestoreEngine initialized");
    }

    private static void tick() {
        if (client.player == null) {
            return;
        }

        restoreHotbar();

        tickCounter++;
        if (tickCounter >= 5) {
            tickCounter = 0;
            restoreInventorySlot();
        }
    }

    private static void restoreHotbar() {
        Inventory inv = client.player.getInventory();
        if (inv == null) return;
        for (int i = 0; i < 9; i++) {
            restoreItem(inv.getItem(i));
        }
    }

    private static void restoreInventorySlot() {
        if (inventorySlotCursor > 44) {
            inventorySlotCursor = 9;
        }

        Inventory inv = client.player.getInventory();
        if (inv == null) return;

        restoreItem(inv.getItem(inventorySlotCursor));
        inventorySlotCursor++;
    }

    private static void restoreItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        String fingerprint = ItemFingerprint.getFingerprint(stack);
        if (fingerprint.isEmpty() || fingerprint.equals("empty")) {
            return;
        }

        StoredItem stored = StorageManager.getItem(fingerprint);
        if (stored == null || stored.getName() == null || stored.getName().isEmpty()) {
            return;
        }

        String currentName = CustomNameManager.getName(stack);
        if (currentName == null || !currentName.equals(stored.getName())) {
            CustomNameManager.applyName(stack, stored.getName());
        }

        if (stored.getLore() != null && !stored.getLore().isEmpty()) {
            List<String> currentLore = CustomNameManager.getLore(stack);
            if (!currentLore.equals(stored.getLore())) {
                CustomNameManager.applyLore(stack, stored.getLore());
            }
        }
    }
}
