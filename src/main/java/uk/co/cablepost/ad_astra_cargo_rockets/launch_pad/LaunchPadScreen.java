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
 *
 * 設計方針(RocketScreenと同様):
 * vanillaのチェスト(generic_54.png)のテクスチャを「ヘッダー+スロット4行分」まで
 * 一度にblitして丸ごと使う。これにより枠線・背景・スロットの穴がすべてテクスチャ由来に
 * なり、自前で枠線を描く必要がなくなる（=ツギハギ感ゼロ）。
 * スロットの穴の上にエネルギー情報・メーターを重ねて描画して隠す。
 */
public class LaunchPadScreen extends AbstractContainerScreen<LaunchPadMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    // テクスチャ内のレイアウト定数
    private static final int HEADER_H = 17;       // ヘッダー高さ
    private static final int SLOT_ROW_H = 18;     // スロット1行の高さ

    // ヘッダー + スロット4行分をまとめてblitする領域。
    // エネルギー情報エリア(ラベル+数値+説明2行+メーター)を収めるため4行分(72px)を確保。
    private static final int INFO_ROWS = 4;
    private static final int CHEST_AREA_H = HEADER_H + SLOT_ROW_H * INFO_ROWS; // 17 + 72 = 89

    static final int PLAYER_INV_Y = CHEST_AREA_H;            // 89
    // 実際のプレイヤーインベントリスロットのY(vanilla準拠で+14)
    static final int PLAYER_INV_ROW_Y = PLAYER_INV_Y + 14;
    static final int HOTBAR_Y     = PLAYER_INV_Y + 72;
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

        // ヘッダー+スロット4行分を丸ごとblit。
        // 枠線・背景・スロットの穴がすべてテクスチャから来るので、自前の枠線描画は不要。
        g.blit(TEXTURE, ox, oy, 0, 0, imageWidth, CHEST_AREA_H);

        // スロットの穴の上にエネルギー情報・メーターを重ねて隠す。
        drawEnergyArea(g, ox, oy);

        // プレイヤーインベントリ+ホットバー: vanilla共通の固定96px領域をそのままblit。
        g.blit(TEXTURE, ox, oy + PLAYER_INV_Y, 0, 126, imageWidth, 96);
    }

    private void drawEnergyArea(GuiGraphics g, int ox, int oy) {
        int barH    = PLAYER_INV_Y - HEADER_H - 12;
        int barTopY = oy + HEADER_H + 6;
        int SEGMENTS = 10;

        // エネルギーバー（右端）10分割
        renderSegmentBar(g, ox + imageWidth - 24, barTopY, 14, barH,
                menu.getEnergy(), menu.getMaxEnergy(), 0xFFDD2200, SEGMENTS);

        int tx = ox + 8;
        int ty = oy + HEADER_H + 6;
        g.drawString(font, "Energy:", tx, ty, 0x404040, false);
        g.drawString(font, formatVal(menu.getEnergy()) + " / " + formatVal(menu.getMaxEnergy()) + " FE",
                tx, ty + 10, 0x404040, false);
        g.drawString(font, "Fuel and cargo are now stored", tx, ty + 26, 0x707070, false);
        g.drawString(font, "in the rocket itself.", tx, ty + 36, 0x707070, false);
    }

    /** 10分割セグメントバー（下から上に充填、セグメント間に区切り線） */
    private void renderSegmentBar(GuiGraphics g, int barX, int barY, int barW, int barH,
                                   int value, int max, int color, int segments) {
        g.fill(barX-1, barY-1, barX+barW+1, barY+barH+1, 0xFF373737);
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
        int barH   = PLAYER_INV_Y - HEADER_H - 12;
        int barTopY = oy + HEADER_H + 6;

        int eBarX = ox + imageWidth - 24;
        if (isMouseOverBar(mouseX, mouseY, eBarX, barTopY, 14, barH)) {
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
