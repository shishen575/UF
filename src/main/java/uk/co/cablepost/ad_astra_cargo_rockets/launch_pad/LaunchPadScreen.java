package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * ランチパッドのGUI画面。
 * v1.2.4でアイテム/燃料/カーゴ流体はロケット自身のGUIに移管されたため、
 * ここではエネルギー残量のみを表示する。
 */
public class LaunchPadScreen extends AbstractContainerScreen<LaunchPadMenu> {

    static final int PLAYER_INV_Y = 84;
    static final int HOTBAR_Y     = 142;
    static final int IMAGE_H      = 164;

    private static final int BLANK_TOP = 24;
    private static final int INV_LBL_Y = 74;

    private static final int COL_BG    = 0xFFC6C6C6;
    private static final int COL_DARK  = 0xFF555555;

    public LaunchPadScreen(LaunchPadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth      = 176;
        this.imageHeight     = IMAGE_H;
        this.titleLabelX     = 8;
        this.titleLabelY     = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = INV_LBL_Y;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int ox = (width  - imageWidth)  / 2;
        int oy = (height - imageHeight) / 2;
        int r  = ox + imageWidth;

        g.fill(ox, oy, r, oy + IMAGE_H, COL_DARK);
        g.fill(ox+1, oy+1, r-1, oy+IMAGE_H-1, COL_BG);
        g.fill(ox+1, oy+16, r-1, oy+17, COL_DARK);

        int barH    = INV_LBL_Y - BLANK_TOP - 8;
        int barTopY = oy + BLANK_TOP + 4;
        int SEGMENTS = 10;

        // エネルギーバー（右端）10分割
        renderSegmentBar(g, ox + imageWidth - 22, barTopY, 12, barH,
                menu.getEnergy(), menu.getMaxEnergy(), 0xFFDD2200, SEGMENTS);

        int tx = ox + 8;
        int ty = oy + BLANK_TOP + 4;
        g.drawString(font, "Energy:", tx, ty, 0x404040, false);
        g.drawString(font, formatVal(menu.getEnergy()) + " / " + formatVal(menu.getMaxEnergy()) + " FE",
                tx, ty + 10, 0x404040, false);
        g.drawString(font, "Fuel and cargo are now stored", tx, ty + 24, 0x707070, false);
        g.drawString(font, "in the rocket itself.", tx, ty + 34, 0x707070, false);

        drawSlotGrid(g, ox+8, oy+PLAYER_INV_Y, 9, 3);
        drawSlotGrid(g, ox+8, oy+HOTBAR_Y,     9, 1);
    }

    /** 10分割セグメントバー（下から上に充填、セグメント間に区切り線） */
    private void renderSegmentBar(GuiGraphics g, int barX, int barY, int barW, int barH,
                                   int value, int max, int color, int segments) {
        g.fill(barX-1, barY-1, barX+barW+1, barY+barH+1, COL_DARK);
        g.fill(barX, barY, barX+barW, barY+barH, 0xFF1A1A1A);

        if (max <= 0) return;

        int filled = (int)((long)value * barH / max);
        if (filled > 0) {
            g.fill(barX, barY + barH - filled, barX + barW, barY + barH, color);
        }
        for (int s = 1; s < segments; s++) {
            int lineY = barY + barH - (barH * s / segments);
            g.fill(barX, lineY, barX + barW, lineY + 1, 0xFF000000);
        }
    }

    private static String formatVal(int v) {
        if (v >= 1_000_000) return String.format("%.2fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    private void drawSlotGrid(GuiGraphics g, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = x + col * 18, sy = y + row * 18;
                g.fill(sx-1, sy-1, sx+17, sy+17, 0xFF373737);
                g.fill(sx, sy, sx+16, sy+16, 0xFF8B8B8B);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title,                titleLabelX,     titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, INV_LBL_Y,   0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        renderBarTooltip(g, mouseX, mouseY);
    }

    private void renderBarTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int ox = (width  - imageWidth)  / 2;
        int oy = (height - imageHeight) / 2;
        int barH   = INV_LBL_Y - BLANK_TOP - 8;
        int barTopY = oy + BLANK_TOP + 4;

        int eBarX = ox + imageWidth - 22;
        if (isMouseOverBar(mouseX, mouseY, eBarX, barTopY, 12, barH)) {
            g.renderTooltip(font, java.util.List.of(
                Component.literal("Energy").getVisualOrderText(),
                Component.literal(
                    formatVal(menu.getEnergy()) + " / " + formatVal(menu.getMaxEnergy()) + " FE").getVisualOrderText()
            ), mouseX, mouseY);
        }
    }

    private boolean isMouseOverBar(int mouseX, int mouseY, int barX, int barY, int barW, int barH) {
        return mouseX >= barX && mouseX <= barX + barW
            && mouseY >= barY && mouseY <= barY + barH;
    }
}
