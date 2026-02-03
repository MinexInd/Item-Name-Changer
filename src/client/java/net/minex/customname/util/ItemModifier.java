package net.minex.customname.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemModifier {
    
    /**
     * Sets the display name of an item stack
     * @param stack The item stack to modify
     * @param displayName The new display name (can contain formatting codes)
     */
    public static void setDisplayName(ItemStack stack, String displayName) {
        if (stack.isEmpty()) return;
        
        // Parse the text to support formatting codes
        Text parsedName = TextParser.parse(displayName);
        stack.set(DataComponentTypes.CUSTOM_NAME, parsedName);
    }

    /**
     * Checks if the item's custom name differs from a raw display name string.
     * @param stack The item stack to check
     * @param displayName The raw display name (supports formatting codes)
     * @return true if the custom name is missing or different
     */
    public static boolean isCustomNameDifferent(ItemStack stack, String displayName) {
        if (stack.isEmpty()) return false;
        Text expected = TextParser.parse(displayName);
        Text current = stack.get(DataComponentTypes.CUSTOM_NAME);
        return !Objects.equals(current, expected);
    }

    /**
     * Sets the lore lines for an item stack
     * @param stack The item stack to modify
     * @param loreLines The raw lore lines (supports formatting codes)
     */
    public static void setLoreLines(ItemStack stack, List<String> loreLines) {
        if (stack.isEmpty()) return;
        if (loreLines == null || loreLines.isEmpty()) {
            stack.remove(DataComponentTypes.LORE);
            return;
        }
        List<Text> parsedLines = new ArrayList<>();
        for (String line : loreLines) {
            parsedLines.add(TextParser.parse(line));
        }
        stack.set(DataComponentTypes.LORE, new LoreComponent(parsedLines));
    }

    /**
     * Adds a lore line to an item stack
     * @param stack The item stack to modify
     * @param loreLine The raw lore line (supports formatting codes)
     */
    public static void addLoreLine(ItemStack stack, String loreLine) {
        if (stack.isEmpty()) return;
        List<Text> lines = new ArrayList<>();
        LoreComponent existing = stack.get(DataComponentTypes.LORE);
        if (existing != null) {
            lines.addAll(existing.lines());
        }
        lines.add(TextParser.parse(loreLine));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lines));
    }

    /**
     * Check if the item's lore differs from raw lore lines.
     * @param stack The item stack to check
     * @param loreLines The raw lore lines
     * @return true if lore is missing or different
     */
    public static boolean isLoreDifferent(ItemStack stack, List<String> loreLines) {
        if (stack.isEmpty()) return false;
        List<Text> expected = new ArrayList<>();
        if (loreLines != null) {
            for (String line : loreLines) {
                expected.add(TextParser.parse(line));
            }
        }
        LoreComponent existing = stack.get(DataComponentTypes.LORE);
        if (expected.isEmpty()) {
            return existing != null && !existing.lines().isEmpty();
        }
        if (existing == null || existing.lines().size() != expected.size()) {
            return true;
        }
        List<Text> current = existing.lines();
        for (int i = 0; i < expected.size(); i++) {
            if (!Objects.equals(current.get(i), expected.get(i))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Resets an item to its original state by removing custom name
     * @param stack The item stack to reset
     */
    public static void resetItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        stack.remove(DataComponentTypes.CUSTOM_NAME);
        stack.remove(DataComponentTypes.LORE);
    }
    
    /**
     * Checks if an item has a custom name
     * @param stack The item stack to check
     * @return true if the item has a custom name
     */
    public static boolean hasCustomName(ItemStack stack) {
        return stack.contains(DataComponentTypes.CUSTOM_NAME);
    }

    /**
     * Checks if an item has lore
     * @param stack The item stack to check
     * @return true if the item has lore
     */
    public static boolean hasLore(ItemStack stack) {
        return stack.contains(DataComponentTypes.LORE);
    }
}
