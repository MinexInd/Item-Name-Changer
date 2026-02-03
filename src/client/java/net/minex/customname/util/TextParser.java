package net.minex.customname.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class TextParser {
    private TextParser() {}

    public static Text parse(String input) {
        if (input == null || input.isEmpty()) {
            return Text.empty();
        }

        MutableText result = Text.empty();
        StringBuilder buffer = new StringBuilder();
        Style style = Style.EMPTY;

        int i = 0;
        while (i < input.length()) {
            char current = input.charAt(i);
            if (current == '\\') {
                int nextIndex = i + 1;
                if (nextIndex >= input.length()) {
                    buffer.append('\\');
                    i++;
                    continue;
                }
                char next = input.charAt(nextIndex);
                if (next == 'n') {
                    buffer.append('\n');
                    i += 2;
                    continue;
                }
                if (next == '&' || next == '\\') {
                    buffer.append(next);
                    i += 2;
                    continue;
                }
                if (next == 'x' && nextIndex + 2 < input.length()) {
                    String hex = input.substring(nextIndex + 1, Math.min(nextIndex + 3, input.length()));
                    Character parsed = parseHexChar(hex, 2);
                    if (parsed != null) {
                        buffer.append(parsed);
                        i += 4;
                        continue;
                    }
                }
                if (next == 'u' && nextIndex + 4 < input.length()) {
                    String hex = input.substring(nextIndex + 1, Math.min(nextIndex + 5, input.length()));
                    Character parsed = parseHexChar(hex, 4);
                    if (parsed != null) {
                        buffer.append(parsed);
                        i += 6;
                        continue;
                    }
                }
                buffer.append(next);
                i += 2;
                continue;
            }

            if (current == '&') {
                if (i + 1 >= input.length()) {
                    buffer.append('&');
                    i++;
                    continue;
                }
                char code = input.charAt(i + 1);

                if (code == '_') {
                    buffer.append(' ');
                    i += 2;
                    continue;
                }

                if (code == '#') {
                    if (i + 7 < input.length()) {
                        String hex = input.substring(i + 2, i + 8);
                        Integer color = parseHexColor(hex);
                        if (color != null) {
                            flushBuffer(result, buffer, style);
                            style = style.withColor(TextColor.fromRgb(color));
                            i += 8;
                            continue;
                        }
                    }
                }

                if (code == '<') {
                    int end = input.indexOf('>', i + 2);
                    if (end != -1) {
                        flushBuffer(result, buffer, style);
                        String keybind = input.substring(i + 2, end);
                        result.append(Text.keybind(keybind).setStyle(style));
                        i = end + 1;
                        continue;
                    }
                }

                if (code == '[') {
                    int end = input.indexOf(']', i + 2);
                    if (end != -1) {
                        flushBuffer(result, buffer, style);
                        String translationKey = input.substring(i + 2, end);
                        result.append(Text.translatable(translationKey).setStyle(style));
                        i = end + 1;
                        continue;
                    }
                }

                Formatting formatting = Formatting.byCode(code);
                if (formatting != null) {
                    flushBuffer(result, buffer, style);
                    if (formatting == Formatting.RESET) {
                        style = Style.EMPTY;
                    } else {
                        style = style.withFormatting(formatting);
                    }
                    i += 2;
                    continue;
                }
            }

            buffer.append(current);
            i++;
        }

        flushBuffer(result, buffer, style);
        return result;
    }

    private static void flushBuffer(MutableText result, StringBuilder buffer, Style style) {
        if (buffer.length() == 0) {
            return;
        }
        result.append(Text.literal(buffer.toString()).setStyle(style));
        buffer.setLength(0);
    }

    private static Integer parseHexColor(String hex) {
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Character parseHexChar(String hex, int expectedLength) {
        if (hex.length() != expectedLength) {
            return null;
        }
        try {
            int value = Integer.parseInt(hex, 16);
            return (char) value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
