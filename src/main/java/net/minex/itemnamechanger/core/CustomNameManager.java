package net.minex.itemnamechanger.core;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minex.itemnamechanger.storage.StorageManager;
import net.minex.itemnamechanger.storage.StoredItem;
import net.minex.itemnamechanger.matching.ItemFingerprint;

public class CustomNameManager {

    private static final Minecraft client = Minecraft.getInstance();
    public static boolean DEBUG = false;

    /**
     * Creates a styled Text from a section-sign-formatted string.
     * Supports multiple inline formatting codes.
     */
    public static Component createStyledText(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = null;
        ChatFormatting currentColor = null;
        boolean bold = false, italic = false, underline = false, strikethrough = false, obfuscated = false;

        String[] parts = formatted.split("\u00A7");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                if (!part.isEmpty()) {
                    result = Component.literal(part);
                }
                continue;
            }

            if (part.isEmpty()) continue;

            char code = part.charAt(0);
            String textPart = part.substring(1);

            ChatFormatting f = getFormatting(code);
            if (f != null) {
                if (f.isColor()) {
                    currentColor = f;
                    bold = italic = underline = strikethrough = obfuscated = false;
                } else if (f == ChatFormatting.BOLD) bold = true;
                else if (f == ChatFormatting.ITALIC) italic = true;
                else if (f == ChatFormatting.UNDERLINE) underline = true;
                else if (f == ChatFormatting.STRIKETHROUGH) strikethrough = true;
                else if (f == ChatFormatting.OBFUSCATED) obfuscated = true;
                else if (f == ChatFormatting.RESET) {
                    currentColor = null;
                    bold = italic = underline = strikethrough = obfuscated = false;
                }
            }

            if (!textPart.isEmpty()) {
                MutableComponent chunk = Component.literal(textPart);
                if (currentColor != null) chunk = chunk.withStyle(currentColor);
                if (bold) chunk = chunk.withStyle(ChatFormatting.BOLD);
                if (italic) chunk = chunk.withStyle(ChatFormatting.ITALIC);
                if (underline) chunk = chunk.withStyle(ChatFormatting.UNDERLINE);
                if (strikethrough) chunk = chunk.withStyle(ChatFormatting.STRIKETHROUGH);
                if (obfuscated) chunk = chunk.withStyle(ChatFormatting.OBFUSCATED);

                if (result == null) {
                    result = chunk;
                } else {
                    result.append(chunk);
                }
            }
        }
        return result == null ? Component.empty() : result;
    }

    private static ChatFormatting getFormatting(char code) {
        return switch (code) {
            case '0' -> ChatFormatting.BLACK;
            case '1' -> ChatFormatting.DARK_BLUE;
            case '2' -> ChatFormatting.DARK_GREEN;
            case '3' -> ChatFormatting.DARK_AQUA;
            case '4' -> ChatFormatting.DARK_RED;
            case '5' -> ChatFormatting.DARK_PURPLE;
            case '6' -> ChatFormatting.GOLD;
            case '7' -> ChatFormatting.GRAY;
            case '8' -> ChatFormatting.DARK_GRAY;
            case '9' -> ChatFormatting.BLUE;
            case 'a' -> ChatFormatting.GREEN;
            case 'b' -> ChatFormatting.AQUA;
            case 'c' -> ChatFormatting.RED;
            case 'd' -> ChatFormatting.LIGHT_PURPLE;
            case 'e' -> ChatFormatting.YELLOW;
            case 'f' -> ChatFormatting.WHITE;
            case 'l' -> ChatFormatting.BOLD;
            case 'o' -> ChatFormatting.ITALIC;
            case 'n' -> ChatFormatting.UNDERLINE;
            case 'm' -> ChatFormatting.STRIKETHROUGH;
            case 'k' -> ChatFormatting.OBFUSCATED;
            case 'r' -> ChatFormatting.RESET;
            default -> null;
        };
    }

    public static ItemStack getHeldItem() {
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        return client.player.getMainHandItem();
    }

    public static boolean hasCustomName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.has(DataComponents.CUSTOM_NAME);
    }

    public static String getName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        return customName != null ? getRawFormattedString(customName) : null;
    }

    private static String getRawFormattedString(Component text) {
        return text.getString();
    }

    public static void applyName(ItemStack stack, String name) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(DataComponents.CUSTOM_NAME, createStyledText(name));
    }

    public static List<String> getLore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        ItemLore loreComponent = stack.get(DataComponents.LORE);
        if (loreComponent == null) {
            return List.of();
        }
        List<Component> textLines = loreComponent.lines();
        List<String> result = new ArrayList<>();
        for (Component text : textLines) {
            result.add(text.getString());
        }
        return result;
    }

    public static void clearLore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(DataComponents.LORE);
    }

    public static void applyName(String name) {
        ItemStack stack = getHeldItem();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(DataComponents.CUSTOM_NAME, createStyledText(name));
        String fingerprint = ItemFingerprint.getFingerprint(stack);
        if (!fingerprint.isEmpty() && !fingerprint.equals("empty")) {
            StorageManager.setItem(fingerprint, name);
        }
    }

    public static void resetName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(DataComponents.CUSTOM_NAME);
    }

    public static void resetName() {
        ItemStack stack = getHeldItem();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(DataComponents.CUSTOM_NAME);
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
        List<Component> textLines = new ArrayList<>();
        for (String line : loreLines) {
            textLines.add(createStyledText(line));
        }
        ItemLore loreComponent = new ItemLore(textLines);
        stack.set(DataComponents.LORE, loreComponent);
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
            stack.set(DataComponents.CUSTOM_NAME, createStyledText(stored.getName()));
        }
    }

    public static void restoreAllItems() {
        if (client.player == null) {
            return;
        }
        var inventory = client.player.getInventory();
        Map<String, StoredItem> storedItems = StorageManager.getAllItems();

        for (int i = 0; i < 45; i++) {
            ItemStack stack = inventory.getItem(i);
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
                    stack.set(DataComponents.CUSTOM_NAME, createStyledText(stored.getName()));
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
