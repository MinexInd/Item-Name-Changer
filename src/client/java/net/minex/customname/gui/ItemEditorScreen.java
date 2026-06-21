package net.minex.customname.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minex.customname.core.CustomNameManager;
import net.minex.customname.matching.ItemFingerprint;
import net.minex.customname.storage.StorageManager;
import net.minex.customname.storage.StoredItem;

import java.util.ArrayList;
import java.util.List;

public class ItemEditorScreen extends Screen {

    private static final int PAD = 12;
    private static final int COL_SW = 16;
    private static final int COL_GAP = 2;
    private static final int COLS = 8;
    private static final int MAX_LORE = 30;
    private static final int LORE_VISIBLE = 5;
    private static final int LORE_ROW_H = 24;

    private static final Formatting[] COLORS = {
        Formatting.BLACK, Formatting.DARK_BLUE, Formatting.DARK_GREEN, Formatting.DARK_AQUA,
        Formatting.DARK_RED, Formatting.DARK_PURPLE, Formatting.GOLD, Formatting.GRAY,
        Formatting.DARK_GRAY, Formatting.BLUE, Formatting.GREEN, Formatting.AQUA,
        Formatting.RED, Formatting.LIGHT_PURPLE, Formatting.YELLOW, Formatting.WHITE
    };

    private final Screen parent;
    private final ItemStack itemStack;
    private final String fingerprint;
    private boolean dataLoaded = false;

    // Layout
    private int leftW;
    private int innerW;
    private int contentH;
    private int startY;

    // Name
    private TextFieldWidget nameField;
    private String savedNameText = "";
    private int focusedTicks = 0;
    private TextFieldWidget lastFocusedField = null;

    // Lore
    private final List<LoreLine> loreLines = new ArrayList<>();
    private int loreScroll = 0;

    // Color hover
    private int colorHover = -1;

    // Layout coords for left panel
    private int nameFieldY;
    private int loreLabelY;
    private int loreAreaY;
    private int loreAreaH;
    private int loreAreaEndY;
    private int addBtnY;
    private int paletteLabelY;
    private int paletteY;
    private int bottomBtnY;

    // Inline format buttons (drawn manually so they never steal focus)
    private static final String[] FMT_CODES  = {"&l", "&o", "&n", "&m", "&k", "&r"};
    private static final String[] FMT_LABELS = {"§lB", "§oI", "§nU", "§mS", "§kO", "r"};
    private int fmtBtnX, fmtBtnY; // top-left of first format button

    public ItemEditorScreen(Screen parent, ItemStack itemStack) {
        super(Text.literal("Item Editor"));
        this.parent = parent;
        this.itemStack = itemStack.copy();
        this.fingerprint = ItemFingerprint.getFingerprint(this.itemStack);
    }

    @Override
    protected void init() {
        clearChildren();
        if (!dataLoaded) {
            loadFromStorage();
            dataLoaded = true;
        }

        leftW = width / 2;
        innerW = leftW - PAD * 2;
        int fx = PAD;

        // Calculate heights
        int rNameField = 24;
        int rLoreLabel = rNameField + 28;
        int rLoreArea = rLoreLabel + 14;
        int visibleLore = Math.min(Math.max(loreLines.size(), 1), LORE_VISIBLE);
        int rLoreAreaH = visibleLore * LORE_ROW_H + 4;
        int rLoreAreaEnd = rLoreArea + rLoreAreaH;
        int rAddBtn = rLoreAreaEnd + 6;
        int rPaletteLabel = rAddBtn + 26;
        int rPalette = rPaletteLabel + 12;
        int rBottomBtn = rPalette + 42;

        contentH = rBottomBtn + 30;
        startY = Math.max(0, (height - contentH) / 2);

        nameFieldY = startY + rNameField;
        loreLabelY = startY + rLoreLabel;
        loreAreaY = startY + rLoreArea;
        loreAreaH = rLoreAreaH;
        loreAreaEndY = startY + rLoreAreaEnd;
        addBtnY = startY + rAddBtn;
        paletteLabelY = startY + rPaletteLabel;
        paletteY = startY + rPalette;
        bottomBtnY = startY + rBottomBtn;

        // Name field
        nameField = new TextFieldWidget(textRenderer, fx + 1, nameFieldY + 1, innerW - 2, 18, Text.empty());
        nameField.setMaxLength(255);
        nameField.setDrawsBackground(false);
        if (!savedNameText.isEmpty()) nameField.setText(savedNameText);
        addSelectableChild(nameField);

        buildLoreWidgets();

        // Add Lore button
        ButtonWidget addBtn = ButtonWidget.builder(Text.literal("+ Add Lore Line"), b -> addLoreLine()).dimensions(fx, addBtnY, 120, 20).build();
        addDrawableChild(addBtn);

        // Format buttons are drawn manually (no ButtonWidget = no focus theft)
        fmtBtnX = fx + 150;
        fmtBtnY = paletteY;

        // Action buttons
        int bw = 80;
        int totalBw = bw * 3 + 12;
        int bx = fx + (innerW - totalBw) / 2;
        ButtonWidget applyBtn = ButtonWidget.builder(Text.literal("Apply").formatted(Formatting.GREEN), b -> apply()).dimensions(bx, bottomBtnY, bw, 22).build();
        ButtonWidget cancelBtn = ButtonWidget.builder(Text.literal("Cancel"), b -> close()).dimensions(bx + bw + 6, bottomBtnY, bw, 22).build();
        ButtonWidget rstBtn = ButtonWidget.builder(Text.literal("Reset").formatted(Formatting.RED), b -> reset()).dimensions(bx + (bw + 6) * 2, bottomBtnY, bw, 22).build();
        addDrawableChild(applyBtn);
        addDrawableChild(cancelBtn);
        addDrawableChild(rstBtn);
    }

    @Override
    public void tick() {
        focusedTicks++;
        if (nameField != null && nameField.isFocused()) lastFocusedField = nameField;
        for (LoreLine ll : loreLines) {
            if (ll.field != null && ll.field.isFocused()) lastFocusedField = ll.field;
        }
    }

    private void insertCode(String code) {
        if (lastFocusedField != null) {
            lastFocusedField.write(code);
            // Restore focus so the user can keep typing immediately
            setFocused(lastFocusedField);
            lastFocusedField.setFocused(true);
        }
    }

    private void saveAllText() {
        if (nameField != null) savedNameText = nameField.getText();
        for (LoreLine ll : loreLines) {
            if (ll.field != null) ll.savedText = ll.field.getText();
        }
    }

    private void buildLoreWidgets() {
        for (LoreLine ll : loreLines) {
            if (ll.field != null) { super.remove(ll.field); ll.field = null; }
            if (ll.removeBtn != null) { super.remove(ll.removeBtn); ll.removeBtn = null; }
        }

        int fx = PAD;
        int rmW = 18;
        int fieldW = innerW - rmW - 4;

        for (int i = 0; i < loreLines.size(); i++) {
            LoreLine ll = loreLines.get(i);
            int vi = i - loreScroll;
            if (vi < 0 || vi >= LORE_VISIBLE) continue;

            int y = loreAreaY + 2 + vi * LORE_ROW_H;

            ll.field = new TextFieldWidget(textRenderer, fx + 1, y, fieldW, 18, Text.empty());
            ll.field.setMaxLength(255);
            ll.field.setDrawsBackground(false);
            if (!ll.savedText.isEmpty()) ll.field.setText(ll.savedText);
            addSelectableChild(ll.field);

            int bx = fx + fieldW + 4;
            ll.removeBtn = ButtonWidget.builder(Text.literal("§cX"), btn -> removeLoreLine(ll.index)).dimensions(bx, y, rmW, 18).build();
            addDrawableChild(ll.removeBtn);
        }
    }

    private void addLoreLine() {
        saveAllText();
        if (loreLines.size() >= MAX_LORE) return;
        loreLines.add(new LoreLine(loreLines.size()));
        loreScroll = Math.max(0, loreLines.size() - LORE_VISIBLE);
        init();
    }

    private void removeLoreLine(int idx) {
        if (idx < 0 || idx >= loreLines.size()) return;
        saveAllText();
        loreLines.remove(idx);
        for (int i = 0; i < loreLines.size(); i++) loreLines.get(i).index = i;
        loreScroll = Math.min(loreScroll, Math.max(0, loreLines.size() - LORE_VISIBLE));
        init();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int fx = PAD;
        if (mx >= fx && mx <= fx + innerW && my >= loreAreaY && my <= loreAreaEndY) {
            if (vAmt < 0 && loreScroll < loreLines.size() - LORE_VISIBLE) {
                saveAllText(); loreScroll++; buildLoreWidgets(); return true;
            }
            if (vAmt > 0 && loreScroll > 0) {
                saveAllText(); loreScroll--; buildLoreWidgets(); return true;
            }
        }
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int fx = PAD;

        // Color palette (handled manually — no focus theft)
        int palW = COLS * (COL_SW + COL_GAP);
        if (mx >= fx && mx < fx + palW && my >= paletteY && my < paletteY + 2 * (COL_SW + COL_GAP)) {
            pickColor(mx, my, fx, paletteY);
            return true;
        }

        // Format buttons (handled manually — no focus theft)
        for (int i = 0; i < FMT_CODES.length; i++) {
            int bx = fmtBtnX + i * 20;
            if (mx >= bx && mx < bx + 18 && my >= fmtBtnY && my < fmtBtnY + 18) {
                insertCode(FMT_CODES[i]);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void pickColor(double mx, double my, int sx, int sy) {
        for (int i = 0; i < COLORS.length; i++) {
            int c = i % COLS, r = i / COLS;
            int x = sx + c * (COL_SW + COL_GAP), y = sy + r * (COL_SW + COL_GAP);
            if (mx >= x && mx < x + COL_SW && my >= y && my < y + COL_SW) {
                char code = getColorCode(COLORS[i]);
                if (code != '\0') {
                    insertCode("&" + code);
                }
                break;
            }
        }
    }

    private char getColorCode(Formatting f) {
        return switch (f) {
            case BLACK -> '0'; case DARK_BLUE -> '1'; case DARK_GREEN -> '2'; case DARK_AQUA -> '3';
            case DARK_RED -> '4'; case DARK_PURPLE -> '5'; case GOLD -> '6'; case GRAY -> '7';
            case DARK_GRAY -> '8'; case BLUE -> '9'; case GREEN -> 'a'; case AQUA -> 'b';
            case RED -> 'c'; case LIGHT_PURPLE -> 'd'; case YELLOW -> 'e'; case WHITE -> 'f';
            default -> '\0';
        };
    }

    // ========== RENDER ==========

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int fx = PAD;

        // Left Panel Background
        ctx.fill(0, 0, leftW, height, 0xF010101C);
        ctx.fill(leftW - 2, 0, leftW, height, 0xFF4444CC); // divider

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "§l§fItem Editor", leftW / 2, startY - 12, 0xFFFFFFFF);

        // ===== NAME SECTION =====
        ctx.drawTextWithShadow(textRenderer, "§bName", fx, nameFieldY - 12, 0xFF88BBFF);
        drawFieldBg(ctx, nameField);

        // ===== LORE SECTION =====
        ctx.drawTextWithShadow(textRenderer, "§bLore §8(" + loreLines.size() + " lines)", fx, loreLabelY, 0xFF88BBFF);
        ctx.fill(fx, loreAreaY, fx + innerW, loreAreaEndY, 0xFF141420);
        ctx.fill(fx, loreAreaY, fx + innerW, loreAreaY + 1, 0xFF333355);
        ctx.fill(fx, loreAreaEndY - 1, fx + innerW, loreAreaEndY, 0xFF333355);

        if (loreLines.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§8No lore lines yet", leftW / 2, loreAreaY + loreAreaH / 2 - 4, 0xFF555555);
        }

        for (LoreLine ll : loreLines) {
            if (ll.field != null) drawFieldBg(ctx, ll.field);
        }

        // Scroll bar
        if (loreLines.size() > LORE_VISIBLE) {
            int sbX = fx + innerW;
            ctx.fill(sbX, loreAreaY + 1, sbX + 6, loreAreaEndY - 1, 0xFF1A1A28);
            float ratio = (float) loreScroll / (loreLines.size() - LORE_VISIBLE);
            int thumbH = Math.max(16, (int) (loreAreaH * ((float) LORE_VISIBLE / loreLines.size())));
            int thumbY = loreAreaY + (int) ((loreAreaH - thumbH) * ratio);
            ctx.fill(sbX + 1, thumbY, sbX + 5, thumbY + thumbH, 0xFF7777BB);
        }

        // Palette
        ctx.drawTextWithShadow(textRenderer, "§7Insert Color Code:", fx, paletteLabelY, 0xFF888888);
        drawPalette(ctx, fx, paletteY, mouseX, mouseY);

        // Format buttons (drawn manually so they never steal focus)
        ctx.drawTextWithShadow(textRenderer, "§7Style:", fmtBtnX, paletteLabelY, 0xFF888888);
        for (int i = 0; i < FMT_CODES.length; i++) {
            int bx = fmtBtnX + i * 20;
            boolean hovered = mouseX >= bx && mouseX < bx + 18 && mouseY >= fmtBtnY && mouseY < fmtBtnY + 18;
            ctx.fill(bx, fmtBtnY, bx + 18, fmtBtnY + 18, hovered ? 0xFF555577 : 0xFF2A2A3A);
            ctx.fill(bx, fmtBtnY, bx + 18, fmtBtnY + 1, 0xFF555555);
            ctx.fill(bx, fmtBtnY + 17, bx + 18, fmtBtnY + 18, 0xFF555555);
            ctx.fill(bx, fmtBtnY, bx + 1, fmtBtnY + 18, 0xFF555555);
            ctx.fill(bx + 17, fmtBtnY, bx + 18, fmtBtnY + 18, 0xFF555555);
            ctx.drawCenteredTextWithShadow(textRenderer, FMT_LABELS[i], bx + 9, fmtBtnY + 5, 0xFFFFFFFF);
        }

        // ===== RIGHT PANEL (PREVIEW) =====
        renderPreview(ctx);

        super.render(ctx, mouseX, mouseY, delta);

        // Draw custom text fields
        renderCustomTextFields(ctx);

        // Tooltip
        if (colorHover >= 0) {
            String cn = COLORS[colorHover].getName() + " (&" + getColorCode(COLORS[colorHover]) + ")";
            ctx.fill(mouseX + 10, mouseY - 14, mouseX + 16 + textRenderer.getWidth(cn), mouseY - 2, 0xEE000000);
            ctx.drawTextWithShadow(textRenderer, cn, mouseX + 13, mouseY - 12, 0xFFFFFFFF);
        }
    }

    private void renderPreview(DrawContext ctx) {
        int prX = leftW + PAD;
        int prY = height / 2 - 60;
        
        ctx.drawTextWithShadow(textRenderer, "§bLive Preview", prX, prY - 16, 0xFF88BBFF);
        
        // Let's render like a tooltip
        String dn = nameField.getText().isEmpty() ? strip(itemStack.getName().getString()) : nameField.getText();
        dn = dn.replace("&", "§"); // Convert & to § for rendering preview
        
        List<String> renderedLore = new ArrayList<>();
        for (LoreLine ll : loreLines) {
            String lt = (ll.field != null && !ll.field.getText().isEmpty()) ? ll.field.getText() : ll.savedText;
            if (!lt.isEmpty()) {
                renderedLore.add(lt.replace("&", "§"));
            }
        }
        
        int tooltipH = 28 + renderedLore.size() * 12;
        int tooltipW = textRenderer.getWidth(dn) + 32; // 32 to leave space for item icon
        for (String l : renderedLore) {
            int w = textRenderer.getWidth(l) + 8;
            if (w > tooltipW) tooltipW = w;
        }

        ctx.fill(prX, prY, prX + tooltipW, prY + tooltipH, 0xDD100010);
        // Draw Border manually
        ctx.fill(prX - 1, prY - 1, prX + tooltipW + 1, prY, 0xFF3300AA); // Top
        ctx.fill(prX - 1, prY + tooltipH, prX + tooltipW + 1, prY + tooltipH + 1, 0xFF3300AA); // Bottom
        ctx.fill(prX - 1, prY, prX, prY + tooltipH, 0xFF3300AA); // Left
        ctx.fill(prX + tooltipW, prY, prX + tooltipW + 1, prY + tooltipH, 0xFF3300AA); // Right

        // Draw item icon
        ctx.drawItem(itemStack, prX + 4, prY + 4);

        // Draw name text (shifted right to account for 16x16 item icon + 4px padding)
        ctx.drawText(textRenderer, CustomNameManager.createStyledText(dn), prX + 24, prY + 8, 0xFFFFFFFF, false);

        int lpY = prY + 24; // shifted down to account for the icon height
        for (int i = 0; i < renderedLore.size(); i++) {
            ctx.drawText(textRenderer, CustomNameManager.createStyledText(renderedLore.get(i)), prX + 4, lpY + i * 12, 0xFFFFFFFF, false);
        }
    }

    private void renderCustomTextFields(DrawContext ctx) {
        // Name field
        if (nameField != null) {
            int textY = nameField.getY() + 5;
            String text = nameField.getText();
            if (!text.isEmpty() || nameField.isFocused()) {
                int color = nameField.isFocused() ? 0xFFE0E0E0 : 0xFFAAAAAA;
                ctx.drawText(textRenderer, text, nameField.getX() + 4, textY, color, false);
            }
            if (nameField.isFocused() && (focusedTicks / 6) % 2 == 0) {
                int cursorPos = nameField.getCursor();
                String textBeforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cx = nameField.getX() + 4 + textRenderer.getWidth(textBeforeCursor);
                ctx.fill(cx, nameField.getY() + 4, cx + 1, nameField.getY() + 14, 0xFFFFFFFF);
            }
            if (text.isEmpty() && !nameField.isFocused()) {
                ctx.drawTextWithShadow(textRenderer, "§7Type item name...", nameField.getX() + 4, textY, 0xFF555555);
            }
        }

        // Lore fields
        for (LoreLine ll : loreLines) {
            if (ll.field == null) continue;
            int ly = ll.field.getY() + 5;
            String text = ll.field.getText();
            if (!text.isEmpty() || ll.field.isFocused()) {
                int color = ll.field.isFocused() ? 0xFFE0E0E0 : 0xFFAAAAAA;
                ctx.drawText(textRenderer, text, ll.field.getX() + 4, ly, color, false);
            }
            if (ll.field.isFocused() && (focusedTicks / 6) % 2 == 0) {
                int cursorPos = ll.field.getCursor();
                String textBeforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cx = ll.field.getX() + 4 + textRenderer.getWidth(textBeforeCursor);
                ctx.fill(cx, ll.field.getY() + 4, cx + 1, ll.field.getY() + 14, 0xFFFFFFFF);
            }
            if (text.isEmpty() && !ll.field.isFocused()) {
                ctx.drawTextWithShadow(textRenderer, "§7Line " + (ll.index + 1) + "...", ll.field.getX() + 4, ly, 0xFF555555);
            }
        }
    }

    private void drawFieldBg(DrawContext ctx, TextFieldWidget field) {
        int x = field.getX() - 2;
        int y = field.getY() - 2;
        int w = field.getWidth() + 4;
        int h = field.getHeight() + 4;
        ctx.fill(x, y, x + w, y + h, 0x80000000);
        ctx.fill(x, y, x + w, y + 1, 0xFF555555);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        ctx.fill(x, y, x + 1, y + h, 0xFF555555);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
    }

    private void drawPalette(DrawContext ctx, int sx, int sy, int mx, int my) {
        colorHover = -1;
        for (int i = 0; i < COLORS.length; i++) {
            int c = i % COLS, r = i / COLS;
            int x = sx + c * (COL_SW + COL_GAP), y = sy + r * (COL_SW + COL_GAP);
            boolean h = mx >= x && mx < x + COL_SW && my >= y && my < y + COL_SW;
            if (h) colorHover = i;
            ctx.fill(x, y, x + COL_SW, y + COL_SW, h ? 0xFF777777 : 0xFF2A2A2A);
            ctx.fill(x + 2, y + 2, x + COL_SW - 2, y + COL_SW - 2, colVal(COLORS[i]) | 0xFF000000);
        }
    }

    // ========== DATA ==========

    private void loadFromStorage() {
        loreLines.clear();
        savedNameText = "";

        StoredItem stored = StorageManager.getItem(fingerprint);
        if (stored != null) {
            if (stored.getName() != null && !stored.getName().isEmpty()) {
                savedNameText = stored.getName().replace("§", "&");
            }
            if (stored.getLore() != null) {
                for (int i = 0; i < stored.getLore().size(); i++) {
                    LoreLine ll = new LoreLine(i);
                    ll.savedText = stored.getLore().get(i).replace("§", "&");
                    loreLines.add(ll);
                }
            }
            return;
        }

        var cn = itemStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
        if (cn != null) {
            String raw = cn.getString(); // Note: if the text had siblings with styling, original logic lost it anyway.
            // Actually, in previous code, `cn.getString()` returns plain text without § if we don't have stored. 
            // So we just load plain string. 
            // Wait, previous code used `parseFmt(raw)` and `strip(raw)`. 
            savedNameText = strip(raw);
        }
        var lc = itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
        if (lc != null) {
            for (int i = 0; i < lc.lines().size(); i++) {
                LoreLine ll = new LoreLine(i);
                ll.savedText = strip(lc.lines().get(i).getString());
                loreLines.add(ll);
            }
        }
    }

    private void apply() {
        MinecraftClient cl = MinecraftClient.getInstance();
        if (cl.player == null) { close(); return; }
        ItemStack held = cl.player.getMainHandStack();
        if (held.isEmpty()) { close(); return; }

        String fp = ItemFingerprint.getFingerprint(held);
        String nt = nameField.getText().trim();

        if (!nt.isEmpty()) {
            String fmt = nt.replace("&", "§");
            CustomNameManager.applyName(fmt);
        }

        List<String> fl = new ArrayList<>();
        for (LoreLine ll : loreLines) {
            String t = ll.field != null ? ll.field.getText().trim() : ll.savedText.trim();
            if (!t.isEmpty()) fl.add(t.replace("&", "§"));
        }
        if (!fl.isEmpty()) CustomNameManager.applyLore(fl);

        String store = nt.isEmpty() ? null : nt.replace("&", "§");
        StorageManager.storeItem(fp, store, fl.isEmpty() ? null : fl);
        close();
    }

    private void reset() {
        MinecraftClient cl = MinecraftClient.getInstance();
        if (cl.player == null) { close(); return; }
        ItemStack held = cl.player.getMainHandStack();
        if (held.isEmpty()) { close(); return; }
        CustomNameManager.resetName();
        CustomNameManager.resetLore();
        StorageManager.removeItem(ItemFingerprint.getFingerprint(held));
        close();
    }

    private static String strip(String t) { return t == null ? "" : t.replaceAll("§.", ""); }

    private static int colVal(Formatting f) {
        return switch (f) {
            case BLACK -> 0x000000; case DARK_BLUE -> 0x0000AA; case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA; case DARK_RED -> 0xAA0000; case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00; case GRAY -> 0xAAAAAA; case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF; case GREEN -> 0x55FF55; case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555; case LIGHT_PURPLE -> 0xFF55FF; case YELLOW -> 0xFFFF55;
            case WHITE -> 0xFFFFFF; default -> 0xFFFFFF;
        };
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    private static class LoreLine {
        int index;
        String savedText = "";
        TextFieldWidget field;
        ButtonWidget removeBtn;

        LoreLine(int index) { this.index = index; }
    }
}
