package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ランチパッドのGUI画面。
 * v1.2.4でアイテム/燃料/カーゴ流体はロケット自身のGUIに移管されたため、
 * ここではエネルギー残量のみを表示する。
 * 背景・プレイヤーインベントリの枠は手描きの塗りつぶしではなく、vanillaのチェスト等と
 * 共通の本物のコンテナテクスチャ(generic_54.png)をそのままblitして使う（RocketScreenと同様）。
 */
public class LaunchPadScreen extends AbstractContainerScreen<LaunchPadMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    // ヘッダーのみ(チェストスロット0行分)の本物のテクスチャ領域
    private static final int HEADER_H = 17;

    static final int PLAYER_INV_Y = 84;
    static final int HOTBAR_Y     = PLAYER_INV_Y + 58;
    static final int IMAGE_H      = PLAYER_INV_Y + 96;

    private static final int INV_LBL_Y = PLAYER_INV_Y - 10;

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

        // ヘッダー部分: vanillaチェストと同じ本物のテクスチャをそのまま使う
        g.blit(TEXTURE, ox, oy, 0, 0, imageWidth, HEADER_H);
        // エネルギー情報エリア(このMOD独自の表示なので手描き)
        drawEnergyArea(g, ox, oy);
        // プレイヤーインベントリ+ホットバー: vanillaチェストGUIと共通の固定96px領域を
        // そのままblitする
        g.blit(TEXTURE, ox, oy + PLAYER_INV_Y, 0, 126, imageWidth, 96);
    }

    private void drawEnergyArea(GuiGraphics g, int ox, int oy) {
        int barH    = PLAYER_INV_Y - HEADER_H - 8;
        int barTopY = oy + HEADER_H + 4;
        int SEGMENTS = 10;

        // エネルギーバー（右端）10分割
        renderSegmentBar(g, ox + imageWidth - 22, barTopY, 12, barH,
                menu.getEnergy(), menu.getMaxEnergy(), 0xFFDD2200, SEGMENTS);

        int tx = ox + 8;
        int ty = oy + HEADER_H + 4;
        g.drawString(font, "Energy:", tx, ty, 0x404040, false);
        g.drawString(font, formatVal(menu.getEnergy()) + " / " + formatVal(menu.getMaxEnergy()) + " FE",
                tx, ty + 10, 0x404040, false);
        g.drawString(font, "Fuel and cargo are now stored", tx, ty + 24, 0x707070, false);
        g.drawString(font, "in the rocket itself.", tx, ty + 34, 0x707070, false);
    }

    /** 10分割セグメントバー（下から上に充填、セグメント間に区切り線） */
    private void renderSegmentBar(GuiGraphics g, int barX, int barY, int barW, int barH,
                                   int value, int max, int color, int segments) {
        g.fill(barX-1, barY-1, barX+barW+1, barY+barH+1, 0xFF555555);
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
        int barH   = PLAYER_INV_Y - HEADER_H - 8;
        int barTopY = oy + HEADER_H + 4;

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
