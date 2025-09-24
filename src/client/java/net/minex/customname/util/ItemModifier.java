package net.minex.customname.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ItemModifier {
    
    /**
     * Sets the display name of an item stack
     * @param stack The item stack to modify
     * @param displayName The new display name (can contain formatting codes)
     */
    public static void setDisplayName(ItemStack stack, String displayName) {
        if (stack.isEmpty()) return;
        
        // Parse the text to support formatting codes like §c, §l, etc.
        Text parsedName = Text.literal(displayName);
        stack.set(DataComponentTypes.CUSTOM_NAME, parsedName);
    }
    
    /**
     * Resets an item to its original state by removing custom name
     * @param stack The item stack to reset
     */
    public static void resetItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        stack.remove(DataComponentTypes.CUSTOM_NAME);
    }
    
    /**
     * Checks if an item has a custom name
     * @param stack The item stack to check
     * @return true if the item has a custom name
     */
    public static boolean hasCustomName(ItemStack stack) {
        return stack.contains(DataComponentTypes.CUSTOM_NAME);
    }
}
