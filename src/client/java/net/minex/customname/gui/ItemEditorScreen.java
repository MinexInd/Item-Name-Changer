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

    private static final int PANEL_W = 400;
    private static final int PAD = 12;
    private static final int COL_SW = 16;
    private static final int COL_GAP = 2;
    private static final int COLS = 8;
    private static final int MAX_LORE = 20;
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
    private int panelX, panelY, panelH;
    private boolean dataLoaded = false;

    // Name
    private TextFieldWidget nameField;
    private Formatting nameColor = Formatting.WHITE;
    private boolean nameBold, nameItalic, nameUnderline, nameStrike;
    private String savedNameText = "";
    private ButtonWidget boldBtn, italicBtn, underlineBtn, strikeBtn;
    private int focusedTicks = 0;

    // Lore
    private final List<LoreLine> loreLines = new ArrayList<>();
    private int loreScroll = 0;

    // Color
    private String colorTarget = "name";
    private int colorHover = -1;

    // Layout
    private int nameFieldY;
    private int namePaletteY;
    private int loreLabelY;
    private int loreAreaY;
    private int loreAreaH;
    private int loreAreaEndY;
    private int lorePaletteY;
    private int previewLabelY;
    private int previewBgY;
    private int previewBgH;

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

        int fw = PANEL_W - PAD * 2;

        // Pass 1: Calculate relative positions (panelY = 0)
        int rNameField = 32;
        int rNamePalette = rNameField + 38;
        int rLoreLabel = rNamePalette + 42;
        int rLoreArea = rLoreLabel + 14;
        int visibleLore = Math.min(Math.max(loreLines.size(), 1), LORE_VISIBLE);
        int rLoreAreaH = visibleLore * LORE_ROW_H + 4;
        int rLoreAreaEnd = rLoreArea + rLoreAreaH;
        int rLorePalette = rLoreAreaEnd + 22;
        int rAddBtn = rLorePalette + 42;
        int rPreviewLabel = rAddBtn + 36;
        int rPreviewBgH = 16 + Math.min(loreLines.size(), 3) * 12 + (loreLines.size() > 3 ? 12 : 0);
        int rPreviewBg = rPreviewLabel + 18;
        int rBottomBtn = rPreviewBg + rPreviewBgH + 12;

        // Pass 2: Center panel and apply offsets
        panelH = rBottomBtn + 36;
        panelY = (height - panelH) / 2;
        panelX = (width - PANEL_W) / 2;

        nameFieldY = panelY + rNameField;
        int nameFmtY = nameFieldY;
        namePaletteY = panelY + rNamePalette;
        loreLabelY = panelY + rLoreLabel;
        loreAreaY = panelY + rLoreArea;
        loreAreaH = rLoreAreaH;
        loreAreaEndY = panelY + rLoreAreaEnd;
        lorePaletteY = panelY + rLorePalette;
        int addBtnY = panelY + rAddBtn;
        previewLabelY = panelY + rPreviewLabel;
        previewBgH = rPreviewBgH;
        previewBgY = panelY + rPreviewBg;
        int bottomBtnY = panelY + rBottomBtn;

        int fx = panelX + PAD;
        int nameFieldW = fw - 76; // 300px field + 4px gap + 72px buttons = 376

        // Name field
        nameField = new TextFieldWidget(textRenderer, fx + 1, nameFieldY + 1, nameFieldW, 18, Text.empty());
        nameField.setMaxLength(100);
        nameField.setDrawsBackground(false);
        if (!savedNameText.isEmpty()) nameField.setText(savedNameText);
        addSelectableChild(nameField);

        // Name format buttons
        int nbx = fx + nameFieldW + 4;
        boldBtn = mkBtn(nbx, nameFmtY, "§lB");
        italicBtn = mkBtn(nbx + 20, nameFmtY, "§oI");
        underlineBtn = mkBtn(nbx + 40, nameFmtY, "§nU");
        strikeBtn = mkBtn(nbx + 60, nameFmtY, "§mS");

        // Lore widgets
        buildLoreWidgets();

        // Add button
        ButtonWidget addBtn = ButtonWidget.builder(Text.literal("+ Add Lore Line"), b -> addLoreLine()).dimensions(fx, addBtnY, 120, 20).build();
        addDrawableChild(addBtn);

        // Bottom buttons
        int bw = 100;
        int totalBw = bw * 3 + 12;
        int bx = panelX + (PANEL_W - totalBw) / 2;
        // Buttons
        ButtonWidget applyBtn = ButtonWidget.builder(Text.literal("Apply").formatted(Formatting.GREEN), b -> apply()).dimensions(bx, bottomBtnY, bw, 22).build();
        ButtonWidget cancelBtn = ButtonWidget.builder(Text.literal("Cancel"), b -> close()).dimensions(bx + bw + 6, bottomBtnY, bw, 22).build();
        ButtonWidget resetBtn = ButtonWidget.builder(Text.literal("Reset").formatted(Formatting.RED), b -> reset()).dimensions(bx + (bw + 6) * 2, bottomBtnY, bw, 22).build();
        addDrawableChild(applyBtn);
        addDrawableChild(cancelBtn);
        addDrawableChild(resetBtn);
    }

    @Override
    public void tick() {
        focusedTicks++;
    }

    private ButtonWidget mkBtn(int x, int y, String label) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), btn -> {}).dimensions(x, y, 18, 18).build();
        addDrawableChild(b);
        return b;
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
            if (ll.boldBtn != null) { super.remove(ll.boldBtn); ll.boldBtn = null; }
            if (ll.italicBtn != null) { super.remove(ll.italicBtn); ll.italicBtn = null; }
            if (ll.underlineBtn != null) { super.remove(ll.underlineBtn); ll.underlineBtn = null; }
            if (ll.removeBtn != null) { super.remove(ll.removeBtn); ll.removeBtn = null; }
        }

        int fx = panelX + PAD;
        int fw = PANEL_W - PAD * 2;
        int fmtW = 82;
        int rmW = 18;
        int fieldW = fw - fmtW - rmW;

        for (int i = 0; i < loreLines.size(); i++) {
            LoreLine ll = loreLines.get(i);
            int vi = i - loreScroll;
            if (vi < 0 || vi >= LORE_VISIBLE) continue;

            int y = loreAreaY + 2 + vi * LORE_ROW_H;

            ll.field = new TextFieldWidget(textRenderer, fx + 1, y, fieldW, 18, Text.empty());
            ll.field.setMaxLength(100);
            ll.field.setDrawsBackground(false);
            if (!ll.savedText.isEmpty()) ll.field.setText(ll.savedText);
            addSelectableChild(ll.field);

            int bx = fx + fieldW + 4;
            ll.boldBtn = ButtonWidget.builder(Text.literal("§lB"), btn -> ll.bold = !ll.bold).dimensions(bx, y, 18, 18).build();
            ll.italicBtn = ButtonWidget.builder(Text.literal("§oI"), btn -> ll.italic = !ll.italic).dimensions(bx + 20, y, 18, 18).build();
            ll.underlineBtn = ButtonWidget.builder(Text.literal("§nU"), btn -> ll.underline = !ll.underline).dimensions(bx + 40, y, 18, 18).build();
            ll.removeBtn = ButtonWidget.builder(Text.literal("§cX"), btn -> removeLoreLine(ll.index)).dimensions(bx + 62, y, rmW, 18).build();

            addDrawableChild(ll.boldBtn);
            addDrawableChild(ll.italicBtn);
            addDrawableChild(ll.underlineBtn);
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
        if (colorTarget.equals("lore_" + idx)) colorTarget = "name";
        init();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int fw = PANEL_W - PAD * 2;
        int fx = panelX + PAD;
        if (mx >= fx && mx <= fx + fw && my >= loreAreaY && my <= loreAreaEndY) {
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
        int fx = panelX + PAD;

        // Toggle buttons
        if (hit(mx, my, boldBtn)) { nameBold = !nameBold; return true; }
        if (hit(mx, my, italicBtn)) { nameItalic = !nameItalic; return true; }
        if (hit(mx, my, underlineBtn)) { nameUnderline = !nameUnderline; return true; }
        if (hit(mx, my, strikeBtn)) { nameStrike = !nameStrike; return true; }

        // Lore toggle buttons
        for (LoreLine ll : loreLines) {
            if (ll.field == null) continue;
            if (hit(mx, my, ll.boldBtn)) { ll.bold = !ll.bold; return true; }
            if (hit(mx, my, ll.italicBtn)) { ll.italic = !ll.italic; return true; }
            if (hit(mx, my, ll.underlineBtn)) { ll.underline = !ll.underline; return true; }
        }

        // Name palette
        int palW = COLS * (COL_SW + COL_GAP);
        if (mx >= fx && mx < fx + palW && my >= namePaletteY && my < namePaletteY + 2 * (COL_SW + COL_GAP)) {
            colorTarget = "name";
            pickColor(mx, my, fx, namePaletteY);
            return true;
        }

        // Lore palette
        if (!loreLines.isEmpty() && mx >= fx && mx < fx + palW && my >= lorePaletteY && my < lorePaletteY + 2 * (COL_SW + COL_GAP)) {
            pickColor(mx, my, fx, lorePaletteY);
            return true;
        }

        // Click lore field sets target
        for (LoreLine ll : loreLines) {
            if (ll.field != null && mx >= ll.field.getX() - 2 && mx <= ll.field.getX() + ll.field.getWidth() + 2
                && my >= ll.field.getY() - 2 && my <= ll.field.getY() + ll.field.getHeight() + 2) {
                colorTarget = "lore_" + ll.index;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean hit(double mx, double my, ButtonWidget btn) {
        return btn != null && mx >= btn.getX() && mx <= btn.getX() + btn.getWidth()
            && my >= btn.getY() && my <= btn.getY() + btn.getHeight();
    }

    private void pickColor(double mx, double my, int sx, int sy) {
        for (int i = 0; i < COLORS.length; i++) {
            int c = i % COLS, r = i / COLS;
            int x = sx + c * (COL_SW + COL_GAP), y = sy + r * (COL_SW + COL_GAP);
            if (mx >= x && mx < x + COL_SW && my >= y && my < y + COL_SW) {
                if (colorTarget.equals("name")) nameColor = COLORS[i];
                else if (colorTarget.startsWith("lore_")) {
                    int li = Integer.parseInt(colorTarget.substring(5));
                    if (li >= 0 && li < loreLines.size()) loreLines.get(li).color = COLORS[i];
                }
                break;
            }
        }
    }

    // ========== RENDER ==========

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int fw = PANEL_W - PAD * 2;
        int fx = panelX + PAD;

        // Panel
        ctx.fill(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + panelH + 1, 0xFF555555);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, 0xF010101C);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, 0xFF4444CC);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "§l§fItem Editor", panelX + PANEL_W / 2, panelY + 8, 0xFFFFFFFF);

        // ===== NAME SECTION =====
        ctx.drawTextWithShadow(textRenderer, "§bName", fx, nameFieldY - 12, 0xFF88BBFF);

        drawFieldBg(ctx, nameField);

        drawBtnHighlight(ctx, boldBtn, nameBold);
        drawBtnHighlight(ctx, italicBtn, nameItalic);
        drawBtnHighlight(ctx, underlineBtn, nameUnderline);
        drawBtnHighlight(ctx, strikeBtn, nameStrike);

        // Name palette
        ctx.drawTextWithShadow(textRenderer, "§7Name Color:", fx, namePaletteY - 13, 0xFF888888);
        drawPalette(ctx, fx, namePaletteY, colorTarget.equals("name") ? nameColor : null, mouseX, mouseY);

        // Target indicator
        String tLabel = colorTarget.equals("name") ? "§aName" : colorTarget.startsWith("lore_")
            ? "§aLore #" + (Integer.parseInt(colorTarget.substring(5)) + 1) : "§7-";
        ctx.drawTextWithShadow(textRenderer, "§7Editing: " + tLabel, fx + 150, namePaletteY + 4, 0xFFAAAAAA);

        // ===== LORE SECTION =====
        ctx.drawTextWithShadow(textRenderer, "§bLore §8(" + loreLines.size() + " lines)", fx, loreLabelY, 0xFF88BBFF);

        // Lore area bg
        ctx.fill(fx, loreAreaY, fx + fw, loreAreaEndY, 0xFF141420);
        ctx.fill(fx, loreAreaY, fx + fw, loreAreaY + 1, 0xFF333355);
        ctx.fill(fx, loreAreaEndY - 1, fx + fw, loreAreaEndY, 0xFF333355);

        if (loreLines.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§8No lore lines yet", panelX + PANEL_W / 2, loreAreaY + loreAreaH / 2 - 4, 0xFF555555);
        }

        // Draw lore area bg behind fields
        for (LoreLine ll : loreLines) {
            if (ll.field == null) continue;
            drawFieldBg(ctx, ll.field);
            drawBtnHighlight(ctx, ll.boldBtn, ll.bold);
            drawBtnHighlight(ctx, ll.italicBtn, ll.italic);
            drawBtnHighlight(ctx, ll.underlineBtn, ll.underline);
        }

        // Scroll bar
        if (loreLines.size() > LORE_VISIBLE) {
            int sbX = fx + fw - 7;
            ctx.fill(sbX, loreAreaY + 1, sbX + 6, loreAreaEndY - 1, 0xFF1A1A28);
            float ratio = (float) loreScroll / (loreLines.size() - LORE_VISIBLE);
            int thumbH = Math.max(16, (int) (loreAreaH * ((float) LORE_VISIBLE / loreLines.size())));
            int thumbY = loreAreaY + (int) ((loreAreaH - thumbH) * ratio);
            ctx.fill(sbX + 1, thumbY, sbX + 5, thumbY + thumbH, 0xFF7777BB);
        }

        // Lore palette
        ctx.drawTextWithShadow(textRenderer, "§7Lore Color:", fx, lorePaletteY - 13, 0xFF888888);
        Formatting ltc = null;
        if (colorTarget.startsWith("lore_")) {
            int li = Integer.parseInt(colorTarget.substring(5));
            if (li >= 0 && li < loreLines.size()) ltc = loreLines.get(li).color;
        }
        drawPalette(ctx, fx, lorePaletteY, ltc, mouseX, mouseY);

        // ===== PREVIEW =====
        ctx.fill(fx, previewLabelY - 2, fx + fw, previewLabelY + 2, 0xFF333355);
        ctx.drawTextWithShadow(textRenderer, "§bPreview", fx, previewLabelY + 3, 0xFF88BBFF);
        ctx.fill(fx, previewBgY, fx + fw, previewBgY + previewBgH, 0xFF1A1A28);
        ctx.drawItem(itemStack, fx + fw - 20, previewBgY);

        String dn = nameField.getText().isEmpty() ? strip(itemStack.getName().getString()) : nameField.getText();
        ctx.drawText(textRenderer, Text.literal(buildFmt(dn, nameColor, nameBold, nameItalic, nameUnderline, nameStrike)), fx + 4, previewBgY + 4, 0xFFFFFFFF, false);

        int lpY = previewBgY + 16;
        for (int i = 0; i < Math.min(3, loreLines.size()); i++) {
            LoreLine ll = loreLines.get(i);
            String lt = (ll.field != null && !ll.field.getText().isEmpty()) ? ll.field.getText() : ll.savedText;
            if (!lt.isEmpty()) {
                ctx.drawText(textRenderer, Text.literal(buildFmt(lt, ll.color, ll.bold, ll.italic, ll.underline, false)), fx + 4, lpY + i * 12, 0xFFFFFFFF, false);
            }
        }
        if (loreLines.size() > 3) {
            ctx.drawTextWithShadow(textRenderer, "§8+" + (loreLines.size() - 3) + " more", fx + 4, lpY + 36, 0xFF555555);
        }

        // Tooltip
        if (colorHover >= 0) {
            String cn = COLORS[colorHover].getName();
            ctx.fill(mouseX + 10, mouseY - 14, mouseX + 16 + textRenderer.getWidth(cn), mouseY - 2, 0xEE000000);
            ctx.drawTextWithShadow(textRenderer, cn, mouseX + 13, mouseY - 12, 0xFFFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);

        // Manually draw text field contents + blinking cursor
        // (setDrawsBackground(false) skips vanilla text/cursor rendering)

        // Name field
        if (nameField != null) {
            int textY = nameField.getY() + 5;
            String text = nameField.getText();

            // Draw text content
            if (!text.isEmpty() || nameField.isFocused()) {
                int color = nameField.isFocused() ? 0xFFE0E0E0 : 0xFFAAAAAA;
                ctx.drawText(textRenderer, text, nameField.getX() + 4, textY, color, false);
            }

            // Blinking cursor - uses getCursor() for actual insertion point
            if (nameField.isFocused() && (focusedTicks / 6) % 2 == 0) {
                int cursorPos = nameField.getCursor();
                String textBeforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cx = nameField.getX() + 4 + textRenderer.getWidth(textBeforeCursor);
                ctx.fill(cx, nameField.getY() + 4, cx + 1, nameField.getY() + 14, 0xFFFFFFFF);
            }

            // Placeholder
            if (text.isEmpty() && !nameField.isFocused()) {
                ctx.drawTextWithShadow(textRenderer, "§7Type item name...", nameField.getX() + 4, textY, 0xFF555555);
            }
        }

        // Lore fields
        for (LoreLine ll : loreLines) {
            if (ll.field == null) continue;
            int ly = ll.field.getY() + 5;
            String text = ll.field.getText();

            // Draw text content
            if (!text.isEmpty() || ll.field.isFocused()) {
                int color = ll.field.isFocused() ? 0xFFE0E0E0 : 0xFFAAAAAA;
                ctx.drawText(textRenderer, text, ll.field.getX() + 4, ly, color, false);
            }

            // Blinking cursor - uses getCursor() for actual insertion point
            if (ll.field.isFocused() && (focusedTicks / 6) % 2 == 0) {
                int cursorPos = ll.field.getCursor();
                String textBeforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cx = ll.field.getX() + 4 + textRenderer.getWidth(textBeforeCursor);
                ctx.fill(cx, ll.field.getY() + 4, cx + 1, ll.field.getY() + 14, 0xFFFFFFFF);
            }

            // Placeholder
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

    private void drawBtnHighlight(DrawContext ctx, ButtonWidget btn, boolean on) {
        if (btn == null || !on) return;
        int x = btn.getX(), y = btn.getY(), w = btn.getWidth(), h = btn.getHeight();
        ctx.fill(x, y, x + w, y + h, 0x4444FF44);
        ctx.fill(x, y, x + w, y + 1, 0xFF44FF44);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFF44FF44);
        ctx.fill(x, y, x + 1, y + h, 0xFF44FF44);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFF44FF44);
    }

    private void drawPalette(DrawContext ctx, int sx, int sy, Formatting sel, int mx, int my) {
        colorHover = -1;
        for (int i = 0; i < COLORS.length; i++) {
            int c = i % COLS, r = i / COLS;
            int x = sx + c * (COL_SW + COL_GAP), y = sy + r * (COL_SW + COL_GAP);
            boolean s = COLORS[i] == sel;
            boolean h = mx >= x && mx < x + COL_SW && my >= y && my < y + COL_SW;
            if (h) colorHover = i;
            ctx.fill(x, y, x + COL_SW, y + COL_SW, s ? 0xFFFFFFFF : (h ? 0xFF777777 : 0xFF2A2A2A));
            int p = s ? 2 : 3;
            ctx.fill(x + p, y + p, x + COL_SW - p, y + COL_SW - p, colVal(COLORS[i]) | 0xFF000000);
        }
    }

    // ========== DATA ==========

    private void loadFromStorage() {
        loreLines.clear();
        nameColor = Formatting.WHITE;
        nameBold = nameItalic = nameUnderline = nameStrike = false;
        savedNameText = "";

        StoredItem stored = StorageManager.getItem(fingerprint);
        if (stored != null) {
            if (stored.getName() != null && !stored.getName().isEmpty()) {
                parseFmt(stored.getName());
                savedNameText = strip(stored.getName());
            }
            if (stored.getLore() != null) {
                for (int i = 0; i < stored.getLore().size(); i++) {
                    LoreLine ll = new LoreLine(i);
                    ll.parseFrom(stored.getLore().get(i));
                    loreLines.add(ll);
                }
            }
            return;
        }

        var cn = itemStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
        if (cn != null) {
            String raw = cn.getString();
            parseFmt(raw);
            savedNameText = strip(raw);
        }
        var lc = itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
        if (lc != null) {
            for (int i = 0; i < lc.lines().size(); i++) {
                LoreLine ll = new LoreLine(i);
                ll.parseFrom(lc.lines().get(i).getString());
                loreLines.add(ll);
            }
        }
    }

    private void parseFmt(String t) {
        if (t == null) return;
        for (Formatting f : COLORS) {
            if (t.contains(f.toString())) { nameColor = f; break; }
        }
        nameBold = t.contains(Formatting.BOLD.toString());
        nameItalic = t.contains(Formatting.ITALIC.toString());
        nameUnderline = t.contains(Formatting.UNDERLINE.toString());
        nameStrike = t.contains(Formatting.STRIKETHROUGH.toString());
    }

    private void apply() {
        MinecraftClient cl = MinecraftClient.getInstance();
        if (cl.player == null) { close(); return; }
        ItemStack held = cl.player.getMainHandStack();
        if (held.isEmpty()) { close(); return; }

        String fp = ItemFingerprint.getFingerprint(held);
        String nt = nameField.getText().trim();

        if (!nt.isEmpty()) {
            String fmt = buildFmt(nt, nameColor, nameBold, nameItalic, nameUnderline, nameStrike);
            CustomNameManager.applyName(fmt);
        }

        List<String> fl = new ArrayList<>();
        for (LoreLine ll : loreLines) {
            String t = ll.field != null ? ll.field.getText().trim() : ll.savedText.trim();
            if (!t.isEmpty()) fl.add(buildFmt(t, ll.color, ll.bold, ll.italic, ll.underline, false));
        }
        if (!fl.isEmpty()) CustomNameManager.applyLore(fl);

        String store = nt.isEmpty() ? null : buildFmt(nt, nameColor, nameBold, nameItalic, nameUnderline, nameStrike);
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

    private static String buildFmt(String text, Formatting color, boolean b, boolean i, boolean u, boolean s) {
        StringBuilder sb = new StringBuilder("§");
        sb.append(Integer.toHexString(color.getColorIndex()));
        if (b) sb.append("§l");
        if (i) sb.append("§o");
        if (u) sb.append("§n");
        if (s) sb.append("§m");
        sb.append(text);
        return sb.toString();
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
        Formatting color = Formatting.DARK_PURPLE;
        boolean bold, italic, underline;
        TextFieldWidget field;
        ButtonWidget boldBtn, italicBtn, underlineBtn, removeBtn;

        LoreLine(int index) { this.index = index; }

        void parseFrom(String raw) {
            if (raw == null) return;
            for (Formatting f : COLORS) { if (raw.contains(f.toString())) { color = f; break; } }
            bold = raw.contains(Formatting.BOLD.toString());
            italic = raw.contains(Formatting.ITALIC.toString());
            underline = raw.contains(Formatting.UNDERLINE.toString());
            savedText = strip(raw);
        }
    }
}
