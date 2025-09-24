package net.minex.customname.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minex.customname.storage.ItemDataStorage;
import net.minex.customname.util.ItemModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class ClientPlayerInventoryMixin {
    
    /**
     * Intercept when items are set in the inventory and restore custom names if needed
     */
    @Inject(method = "setStack", at = @At("TAIL"))
    private void onSetStack(int slot, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty()) return;
        
        // Check if this item should have custom name restored
        ItemDataStorage.ItemData storedData = ItemDataStorage.getStoredItem(stack);
        if (storedData != null && storedData.displayName != null && !ItemModifier.hasCustomName(stack)) {
            // Restore the item's custom name
            ItemModifier.setDisplayName(stack, storedData.displayName);
        }
    }
}
