package net.minex.itemnamechanger.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minex.itemnamechanger.core.CustomNameManager;
import net.minex.itemnamechanger.matching.ItemFingerprint;
import net.minex.itemnamechanger.storage.StorageManager;
import net.minex.itemnamechanger.storage.StoredItem;

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
    private static final int PANEL_W = 380;

    private static final ChatFormatting[] COLORS = {
        ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_AQUA,
        ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE, ChatFormatting.GOLD, ChatFormatting.GRAY,
        ChatFormatting.DARK_GRAY, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
        ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW, ChatFormatting.WHITE
    };

    private final Screen parent;
    private final ItemStack itemStack;
    private final String fingerprint;
    private boolean dataLoaded = false;

    private int panelX;
    private int innerW;
    private int contentH;
    private int startY;

    private EditBox nameField;
    private String savedNameText = "";
    private int focusedTicks = 0;
    private EditBox lastFocusedField = null;

    private final List<LoreLine> loreLines = new ArrayList<>();
    private int loreScroll = 0;

    private int colorHover = -1;

    private int previewY;
    private int previewH;
    private int nameFieldY;
    private int loreLabelY;
    private int loreAreaY;
    private int loreAreaH;
    private int loreAreaEndY;
    private int addBtnY;
    private int paletteLabelY;
    private int paletteY;
    private int bottomBtnY;

    private static final String[] FMT_CODES  = {"&l", "&o", "&n", "&m", "&k", "&r"};
    private static final String[] FMT_LABELS = {"\u00A7lB", "\u00A7oI", "\u00A7nU", "\u00A7mS", "\u00A7kO", "r"};
    private int fmtBtnX, fmtBtnY;

    public ItemEditorScreen(Screen parent, ItemStack itemStack) {
        super(Component.literal("Item Editor"));
        this.parent = parent;
        this.itemStack = itemStack.copy();
        this.fingerprint = ItemFingerprint.getFingerprint(this.itemStack);
    }

    @Override
    protected void init() {
        clearWidgets();
        if (!dataLoaded) {
            loadFromStorage();
            dataLoaded = true;
        }

        panelX = (width - PANEL_W) / 2;
        innerW = PANEL_W - PAD * 2;
        int fx = panelX + PAD;

        String dn = nameField != null ? nameField.getValue() : savedNameText;
        if (dn == null) dn = "";
        int previewLines = 0;
        if (!dn.isEmpty() || !loreLines.isEmpty()) previewLines = 1;
        previewH = 28 + Math.max(previewLines, 0) * 12;
        for (LoreLine ll : loreLines) {
            String lt = (ll.field != null && !ll.field.getValue().isEmpty()) ? ll.field.getValue() : ll.savedText;
            if (lt != null && !lt.isEmpty()) previewH += 12;
        }
        if (previewH < 40) previewH = 40;

        int rPreview = 28;
        int rPreviewEnd = rPreview + previewH + 8;
        int rNameField = rPreviewEnd + 4;
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

        previewY = startY + rPreview;
        nameFieldY = startY + rNameField;
        loreLabelY = startY + rLoreLabel;
        loreAreaY = startY + rLoreArea;
        loreAreaH = rLoreAreaH;
        loreAreaEndY = startY + rLoreAreaEnd;
        addBtnY = startY + rAddBtn;
        paletteLabelY = startY + rPaletteLabel;
        paletteY = startY + rPalette;
        bottomBtnY = startY + rBottomBtn;

        nameField = new EditBox(font, fx + 1, nameFieldY + 1, innerW - 2, 18, Component.empty());
        nameField.setMaxLength(255);
        nameField.setBordered(false);
        if (!savedNameText.isEmpty()) nameField.setValue(savedNameText);
        addWidget(nameField);

        buildLoreWidgets();

        Button addBtn = Button.builder(Component.literal("+ Add Lore Line"), b -> addLoreLine()).bounds(fx, addBtnY, 120, 20).build();
        addRenderableWidget(addBtn);

        fmtBtnX = fx + 150;
        fmtBtnY = paletteY;

        int bw = 80;
        int totalBw = bw * 3 + 12;
        int bx = fx + (innerW - totalBw) / 2;
        Button applyBtn = Button.builder(Component.literal("Apply").withStyle(ChatFormatting.GREEN), b -> apply()).bounds(bx, bottomBtnY, bw, 22).build();
        Button cancelBtn = Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(bx + bw + 6, bottomBtnY, bw, 22).build();
        Button rstBtn = Button.builder(Component.literal("Reset").withStyle(ChatFormatting.RED), b -> reset()).bounds(bx + (bw + 6) * 2, bottomBtnY, bw, 22).build();
        addRenderableWidget(applyBtn);
        addRenderableWidget(cancelBtn);
        addRenderableWidget(rstBtn);
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
            lastFocusedField.insertText(code);
            setFocused(lastFocusedField);
            lastFocusedField.setFocused(true);
        }
    }

    private void saveAllText() {
        if (nameField != null) savedNameText = nameField.getValue();
        for (LoreLine ll : loreLines) {
            if (ll.field != null) ll.savedText = ll.field.getValue();
        }
    }

    private void buildLoreWidgets() {
        for (LoreLine ll : loreLines) {
            if (ll.field != null) { super.removeWidget(ll.field); ll.field = null; }
            if (ll.removeBtn != null) { super.removeWidget(ll.removeBtn); ll.removeBtn = null; }
        }

        int fx = panelX + PAD;
        int rmW = 18;
        int fieldW = innerW - rmW - 4;

        for (int i = 0; i < loreLines.size(); i++) {
            LoreLine ll = loreLines.get(i);
            int vi = i - loreScroll;
            if (vi < 0 || vi >= LORE_VISIBLE) continue;

            int y = loreAreaY + 2 + vi * LORE_ROW_H;

            ll.field = new EditBox(font, fx + 1, y, fieldW, 18, Component.empty());
            ll.field.setMaxLength(255);
            ll.field.setBordered(false);
            if (!ll.savedText.isEmpty()) ll.field.setValue(ll.savedText);
            addWidget(ll.field);

            int bx = fx + fieldW + 4;
            ll.removeBtn = Button.builder(Component.literal("\u00A7cX"), btn -> removeLoreLine(ll.index)).bounds(bx, y, rmW, 18).build();
            addRenderableWidget(ll.removeBtn);
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
        int fx = panelX + PAD;
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
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int fx = panelX + PAD;

        int palW = COLS * (COL_SW + COL_GAP);
        if (mouseX >= fx && mouseX < fx + palW && mouseY >= paletteY && mouseY < paletteY + 2 * (COL_SW + COL_GAP)) {
            pickColor(mouseX, mouseY, fx, paletteY);
            return true;
        }

        for (int i = 0; i < FMT_CODES.length; i++) {
            int bx = fmtBtnX + i * 20;
            if (mouseX >= bx && mouseX < bx + 18 && mouseY >= fmtBtnY && mouseY < fmtBtnY + 18) {
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

    private char getColorCode(ChatFormatting f) {
        return switch (f) {
            case BLACK -> '0'; case DARK_BLUE -> '1'; case DARK_GREEN -> '2'; case DARK_AQUA -> '3';
            case DARK_RED -> '4'; case DARK_PURPLE -> '5'; case GOLD -> '6'; case GRAY -> '7';
            case DARK_GRAY -> '8'; case BLUE -> '9'; case GREEN -> 'a'; case AQUA -> 'b';
            case RED -> 'c'; case LIGHT_PURPLE -> 'd'; case YELLOW -> 'e'; case WHITE -> 'f';
            default -> '\0';
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int fx = panelX + PAD;

        ctx.fill(panelX, 0, panelX + PANEL_W, height, 0xF010101C);
        ctx.fill(panelX, 0, panelX + 1, height, 0xFF4444CC);
        ctx.fill(panelX + PANEL_W - 1, 0, panelX + PANEL_W, height, 0xFF4444CC);

        ctx.centeredText(font, "\u00A7l\u00A7fItem Editor", panelX + PANEL_W / 2, startY - 12, 0xFFFFFFFF);

        renderPreview(ctx);

        ctx.text(font, "\u00A7bName", fx, nameFieldY - 12, 0xFF88BBFF);
        drawFieldBg(ctx, nameField);

        ctx.text(font, "\u00A7bLore \u00A78(" + loreLines.size() + " lines)", fx, loreLabelY, 0xFF88BBFF);
        ctx.fill(fx, loreAreaY, fx + innerW, loreAreaEndY, 0xFF141420);
        ctx.fill(fx, loreAreaY, fx + innerW, loreAreaY + 1, 0xFF333355);
        ctx.fill(fx, loreAreaEndY - 1, fx + innerW, loreAreaEndY, 0xFF333355);

        if (loreLines.isEmpty()) {
            ctx.centeredText(font, "\u00A78No lore lines yet", panelX + PANEL_W / 2, loreAreaY + loreAreaH / 2 - 4, 0xFF555555);
        }

        for (LoreLine ll : loreLines) {
            if (ll.field != null) drawFieldBg(ctx, ll.field);
        }

        if (loreLines.size() > LORE_VISIBLE) {
            int sbX = fx + innerW;
            ctx.fill(sbX, loreAreaY + 1, sbX + 6, loreAreaEndY - 1, 0xFF1A1A28);
            float ratio = (float) loreScroll / (loreLines.size() - LORE_VISIBLE);
            int thumbH = Math.max(16, (int) (loreAreaH * ((float) LORE_VISIBLE / loreLines.size())));
            int thumbY = loreAreaY + (int) ((loreAreaH - thumbH) * ratio);
            ctx.fill(sbX + 1, thumbY, sbX + 5, thumbY + thumbH, 0xFF7777BB);
        }

        ctx.text(font, "\u00A77Insert Color Code:", fx, paletteLabelY, 0xFF888888);
        drawPalette(ctx, fx, paletteY, mouseX, mouseY);

        ctx.text(font, "\u00A77Style:", fmtBtnX, paletteLabelY, 0xFF888888);
        for (int i = 0; i < FMT_CODES.length; i++) {
            int bx = fmtBtnX + i * 20;
            boolean hovered = mouseX >= bx && mouseX < bx + 18 && mouseY >= fmtBtnY && mouseY < fmtBtnY + 18;
            ctx.fill(bx, fmtBtnY, bx + 18, fmtBtnY + 18, hovered ? 0xFF555577 : 0xFF2A2A3A);
            ctx.fill(bx, fmtBtnY, bx + 18, fmtBtnY + 1, 0xFF555555);
            ctx.fill(bx, fmtBtnY + 17, bx + 18, fmtBtnY + 18, 0xFF555555);
            ctx.fill(bx, fmtBtnY, bx + 1, fmtBtnY + 18, 0xFF555555);
            ctx.fill(bx + 17, fmtBtnY, bx + 18, fmtBtnY + 18, 0xFF555555);
            ctx.centeredText(font, FMT_LABELS[i], bx + 9, fmtBtnY + 5, 0xFFFFFFFF);
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);

        renderCustomTextFields(ctx);

        if (colorHover >= 0) {
            String cn = COLORS[colorHover].getName() + " (&" + getColorCode(COLORS[colorHover]) + ")";
            ctx.fill(mouseX + 10, mouseY - 14, mouseX + 16 + font.width(cn), mouseY - 2, 0xEE000000);
            ctx.text(font, cn, mouseX + 13, mouseY - 12, 0xFFFFFFFF);
        }
    }

    private void renderPreview(GuiGraphicsExtractor ctx) {
        int prX = panelX + PAD;
        int prY = previewY;
        int prW = innerW;

        ctx.text(font, "\u00A7bLive Preview", prX, prY - 16, 0xFF88BBFF);

        String dn = nameField.getValue().isEmpty() ? strip(itemStack.getHoverName().getString()) : nameField.getValue();
        dn = dn.replace("&", "\u00A7");

        List<String> renderedLore = new ArrayList<>();
        for (LoreLine ll : loreLines) {
            String lt = (ll.field != null && !ll.field.getValue().isEmpty()) ? ll.field.getValue() : ll.savedText;
            if (!lt.isEmpty()) {
                renderedLore.add(lt.replace("&", "\u00A7"));
            }
        }

        int tooltipH = 28 + renderedLore.size() * 12;
        int tooltipW = prW;

        ctx.fill(prX, prY, prX + tooltipW, prY + tooltipH, 0xDD100010);
        ctx.fill(prX - 1, prY - 1, prX + tooltipW + 1, prY, 0xFF3300AA);
        ctx.fill(prX - 1, prY + tooltipH, prX + tooltipW + 1, prY + tooltipH + 1, 0xFF3300AA);
        ctx.fill(prX - 1, prY, prX, prY + tooltipH, 0xFF3300AA);
        ctx.fill(prX + tooltipW, prY, prX + tooltipW + 1, prY + tooltipH, 0xFF3300AA);

        ctx.item(itemStack, prX + 4, prY + 4);

        ctx.text(font, CustomNameManager.createStyledText(dn), prX + 24, prY + 8, 0xFFFFFFFF, false);

        int lpY = prY + 24;
        for (int i = 0; i < renderedLore.size(); i++) {
            ctx.text(font, CustomNameManager.createStyledText(renderedLore.get(i)), prX + 4, lpY + i * 12, 0xFFFFFFFF, false);
        }
    }

    private void renderCustomTextFields(GuiGraphicsExtractor ctx) {
        if (nameField != null) {
            int textY = nameField.getY() + 5;
            String text = nameField.getValue();
            if (!text.isEmpty() || nameField.isFocused()) {
                int color = nameField.isFocused() ? 0xFFE0E0E0 : 0xFFAAAAAA;
                ctx.text(font, text, nameField.getX() + 4, textY, color, false);
            }
            if (nameField.isFocused() && (focusedTicks / 6) % 2 == 0) {
                int cursorPos = nameField.getCursorPosition();
                String textBeforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cx = nameField.getX() + 4 + font.width(textBeforeCursor);
                ctx.fill(cx, nameField.getY() + 4, cx + 1, nameField.getY() + 14, 0xFFFFFFFF);
            }
            if (text.isEmpty() && !nameField.isFocused()) {
                ctx.text(font, "\u00A77Type item name...", nameField.getX() + 4, textY, 0xFF555555);
            }
        }

        for (LoreLine ll : loreLines) {
            if (ll.field == null) continue;
            int ly = ll.field.getY() + 5;
            String text = ll.field.getValue();
            if (!text.isEmpty() || ll.field.isFocused()) {
                int color = ll.field.isFocused() ? 0xFFE0E0E0 : 0xFFAAAAAA;
                ctx.text(font, text, ll.field.getX() + 4, ly, color, false);
            }
            if (ll.field.isFocused() && (focusedTicks / 6) % 2 == 0) {
                int cursorPos = ll.field.getCursorPosition();
                String textBeforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cx = ll.field.getX() + 4 + font.width(textBeforeCursor);
                ctx.fill(cx, ll.field.getY() + 4, cx + 1, ll.field.getY() + 14, 0xFFFFFFFF);
            }
            if (text.isEmpty() && !ll.field.isFocused()) {
                ctx.text(font, "\u00A77Line " + (ll.index + 1) + "...", ll.field.getX() + 4, ly, 0xFF555555);
            }
        }
    }

    private void drawFieldBg(GuiGraphicsExtractor ctx, EditBox field) {
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

    private void drawPalette(GuiGraphicsExtractor ctx, int sx, int sy, int mx, int my) {
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

    private void loadFromStorage() {
        loreLines.clear();
        savedNameText = "";

        StoredItem stored = StorageManager.getItem(fingerprint);
        if (stored != null) {
            if (stored.getName() != null && !stored.getName().isEmpty()) {
                savedNameText = stored.getName().replace("\u00A7", "&");
            }
            if (stored.getLore() != null) {
                for (int i = 0; i < stored.getLore().size(); i++) {
                    LoreLine ll = new LoreLine(i);
                    ll.savedText = stored.getLore().get(i).replace("\u00A7", "&");
                    loreLines.add(ll);
                }
            }
            return;
        }

        var cn = itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (cn != null) {
            String raw = cn.getString();
            savedNameText = strip(raw);
        }
        var lc = itemStack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lc != null) {
            for (int i = 0; i < lc.lines().size(); i++) {
                LoreLine ll = new LoreLine(i);
                ll.savedText = strip(lc.lines().get(i).getString());
                loreLines.add(ll);
            }
        }
    }

    private void apply() {
        Minecraft cl = Minecraft.getInstance();
        if (cl.player == null) { onClose(); return; }
        ItemStack held = cl.player.getMainHandItem();
        if (held.isEmpty()) { onClose(); return; }

        String fp = ItemFingerprint.getFingerprint(held);
        String nt = nameField.getValue().trim();

        if (!nt.isEmpty()) {
            String fmt = nt.replace("&", "\u00A7");
            CustomNameManager.applyName(fmt);
        }

        List<String> fl = new ArrayList<>();
        for (LoreLine ll : loreLines) {
            String t = ll.field != null ? ll.field.getValue().trim() : ll.savedText.trim();
            if (!t.isEmpty()) fl.add(t.replace("&", "\u00A7"));
        }
        if (!fl.isEmpty()) CustomNameManager.applyLore(fl);

        String store = nt.isEmpty() ? null : nt.replace("&", "\u00A7");
        StorageManager.storeItem(fp, store, fl.isEmpty() ? null : fl);
        onClose();
    }

    private void reset() {
        Minecraft cl = Minecraft.getInstance();
        if (cl.player == null) { onClose(); return; }
        ItemStack held = cl.player.getMainHandItem();
        if (held.isEmpty()) { onClose(); return; }
        CustomNameManager.resetName();
        CustomNameManager.resetLore();
        StorageManager.removeItem(ItemFingerprint.getFingerprint(held));
        onClose();
    }

    private static String strip(String t) { return t == null ? "" : t.replaceAll("\u00A7.", ""); }

    private static int colVal(ChatFormatting f) {
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
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    private static class LoreLine {
        int index;
        String savedText = "";
        EditBox field;
        Button removeBtn;

        LoreLine(int index) { this.index = index; }
    }
}
