package net.minex.customname.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minex.customname.storage.StorageManager;
import net.minex.customname.storage.StoredItem;
import net.minex.customname.matching.ItemFingerprint;

public class CustomNameManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    public static boolean DEBUG = false;

    /**
     * Creates a styled Text from a §-formatted string.
     * Supports multiple inline formatting codes.
     */
    public static Text createStyledText(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return Text.empty();
        }

        MutableText result = null;
        Formatting currentColor = null;
        boolean bold = false, italic = false, underline = false, strikethrough = false, obfuscated = false;

        String[] parts = formatted.split("\u00A7");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                if (!part.isEmpty()) {
                    result = Text.literal(part);
                }
                continue;
            }

            if (part.isEmpty()) continue;

            char code = part.charAt(0);
            String textPart = part.substring(1);

            Formatting f = getFormatting(code);
            if (f != null) {
                if (f.isColor()) {
                    currentColor = f;
                    bold = italic = underline = strikethrough = obfuscated = false;
                } else if (f == Formatting.BOLD) bold = true;
                else if (f == Formatting.ITALIC) italic = true;
                else if (f == Formatting.UNDERLINE) underline = true;
                else if (f == Formatting.STRIKETHROUGH) strikethrough = true;
                else if (f == Formatting.OBFUSCATED) obfuscated = true;
                else if (f == Formatting.RESET) {
                    currentColor = null;
                    bold = italic = underline = strikethrough = obfuscated = false;
                }
            }

            if (!textPart.isEmpty()) {
                MutableText chunk = Text.literal(textPart);
                if (currentColor != null) chunk = chunk.formatted(currentColor);
                if (bold) chunk = chunk.formatted(Formatting.BOLD);
                if (italic) chunk = chunk.formatted(Formatting.ITALIC);
                if (underline) chunk = chunk.formatted(Formatting.UNDERLINE);
                if (strikethrough) chunk = chunk.formatted(Formatting.STRIKETHROUGH);
                if (obfuscated) chunk = chunk.formatted(Formatting.OBFUSCATED);
                
                if (result == null) {
                    result = chunk;
                } else {
                    result.append(chunk);
                }
            }
        }
        return result == null ? Text.empty() : result;
    }

    private static Formatting getFormatting(char code) {
        return switch (code) {
            case '0' -> Formatting.BLACK;
            case '1' -> Formatting.DARK_BLUE;
            case '2' -> Formatting.DARK_GREEN;
            case '3' -> Formatting.DARK_AQUA;
            case '4' -> Formatting.DARK_RED;
            case '5' -> Formatting.DARK_PURPLE;
            case '6' -> Formatting.GOLD;
            case '7' -> Formatting.GRAY;
            case '8' -> Formatting.DARK_GRAY;
            case '9' -> Formatting.BLUE;
            case 'a' -> Formatting.GREEN;
            case 'b' -> Formatting.AQUA;
            case 'c' -> Formatting.RED;
            case 'd' -> Formatting.LIGHT_PURPLE;
            case 'e' -> Formatting.YELLOW;
            case 'f' -> Formatting.WHITE;
            case 'l' -> Formatting.BOLD;
            case 'o' -> Formatting.ITALIC;
            case 'n' -> Formatting.UNDERLINE;
            case 'm' -> Formatting.STRIKETHROUGH;
            case 'k' -> Formatting.OBFUSCATED;
            case 'r' -> Formatting.RESET;
            default -> null;
        };
    }

    public static ItemStack getHeldItem() {
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        return client.player.getMainHandStack();
    }

    public static boolean hasCustomName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.contains(DataComponentTypes.CUSTOM_NAME);
    }

    public static String getName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        // Note: For actual NBT name, getting the formatted string involves serializing.
        // We only return the literal string from the top level text if it doesn't have siblings
        // Actually, since 1.20, getString() gets the plain text.
        return customName != null ? getRawFormattedString(customName) : null;
    }

    private static String getRawFormattedString(Text text) {
        // Since we might need the § codes back if we read directly from item stack...
        // For simplicity, we just use getString() if no better option, or rely on storage for accurate restore.
        // But to be complete, one might visit the text components. 
        // Here we just use what was in the original code, but since original stripped § codes, 
        // it only ever returned unformatted text!
        return text.getString(); 
    }

    public static void applyName(ItemStack stack, String name) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(DataComponentTypes.CUSTOM_NAME, createStyledText(name));
    }

    public static List<String> getLore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        LoreComponent loreComponent = stack.get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return List.of();
        }
        List<Text> textLines = loreComponent.lines();
        List<String> result = new ArrayList<>();
        for (Text text : textLines) {
            result.add(text.getString());
        }
        return result;
    }

    public static void clearLore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(DataComponentTypes.LORE);
    }

    public static void applyName(String name) {
        ItemStack stack = getHeldItem();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(DataComponentTypes.CUSTOM_NAME, createStyledText(name));
        String fingerprint = ItemFingerprint.getFingerprint(stack);
        if (!fingerprint.isEmpty() && !fingerprint.equals("empty")) {
            StorageManager.setItem(fingerprint, name);
        }
    }

    public static void resetName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(DataComponentTypes.CUSTOM_NAME);
    }

    public static void resetName() {
        ItemStack stack = getHeldItem();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(DataComponentTypes.CUSTOM_NAME);
        String fingerprint = ItemFingerprint.getFingerprint(stack);
        if (!fingerprint.isEmpty() && !fingerprint.equals("empty")) {
            StorageManager.removeItem(fingerprint);
        }
    }

    public static void applyLore(List<String> loreLines) {
        ItemStack stack = getHeldItem();
        applyLore(stack, loreLines);
    }

    public static void applyLore(ItemStack stack, List<String> loreLines) {
        if (stack == null || stack.isEmpty() || loreLines == null || loreLines.isEmpty()) {
            return;
        }
        List<Text> textLines = new ArrayList<>();
        for (String line : loreLines) {
            textLines.add(createStyledText(line));
        }
        LoreComponent loreComponent = new LoreComponent(textLines);
        stack.set(DataComponentTypes.LORE, loreComponent);
    }

    public static void resetLore() {
        ItemStack stack = getHeldItem();
        clearLore(stack);
    }

    public static void restoreName() {
        ItemStack stack = getHeldItem();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String fingerprint = ItemFingerprint.getFingerprint(stack);
        if (fingerprint.isEmpty() || fingerprint.equals("empty")) {
            return;
        }
        StoredItem stored = StorageManager.getItem(fingerprint);
        if (stored != null && stored.getName() != null && !stored.getName().isEmpty()) {
            stack.set(DataComponentTypes.CUSTOM_NAME, createStyledText(stored.getName()));
        }
    }

    public static void restoreAllItems() {
        if (client.player == null) {
            return;
        }
        var inventory = client.player.getInventory();
        Map<String, StoredItem> storedItems = StorageManager.getAllItems();

        for (int i = 0; i < 45; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String fingerprint = ItemFingerprint.getFingerprint(stack);
            if (fingerprint.isEmpty() || fingerprint.equals("empty")) {
                continue;
            }
            StoredItem stored = storedItems.get(fingerprint);
            if (stored != null && stored.getName() != null && !stored.getName().isEmpty()) {
                String currentName = getName(stack);
                if (currentName == null || !currentName.equals(stored.getName())) {
                    stack.set(DataComponentTypes.CUSTOM_NAME, createStyledText(stored.getName()));
                }
            }
            if (stored != null && stored.lore != null && !stored.lore.isEmpty()) {
                List<String> currentLore = getLore(stack);
                if (!currentLore.equals(stored.lore)) {
                    applyLore(stack, stored.lore);
                }
            }
        }
    }
}
