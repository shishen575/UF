package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ロケットのインベントリ・燃料/カーゴ流体タンクを表示するGUI画面。
 *
 * 設計方針:
 * vanillaの3行チェスト(generic_54.png)のテクスチャを「ヘッダー+スロット3行分」まで
 * 一度にblitして丸ごと使う。これにより枠線・背景・スロットの穴がすべてテクスチャ由来に
 * なり、自前で枠線を描く必要がなくなる（=ツギハギ感ゼロ）。
 *
 * ロケットの9スロットは上1行だけを使い、残りの下2行分のスロットの穴の上には
 * Fuel/Cargoの流体メーターを重ねて描画して隠す。
 * その下にvanilla共通のプレイヤーインベントリ+ホットバー(96px)をblitする。
 */
public class RocketScreen extends AbstractContainerScreen<RocketMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    // テクスチャ内のレイアウト定数
    private static final int HEADER_H = 17;          // ヘッダー高さ
    private static final int SLOT_ROW_H = 18;         // スロット1行の高さ

    // ヘッダー + スロット3行分をまとめてblitする領域(=3行チェスト相当)
    private static final int CHEST_AREA_H = HEADER_H + SLOT_ROW_H * 3; // 17 + 54 = 71

    // ロケットスロット(上1行)の実描画位置
    public static final int SLOT_Y = HEADER_H + 1;   // 18

    // 下2行分のスロット穴をメーターエリアとして使う。その先頭Y(テクスチャ先頭からの相対)
    private static final int METER_AREA_Y = HEADER_H + SLOT_ROW_H; // 35

    // Fuel/Cargoメーターの配置(メーターエリア内)
    public static final int FUEL_GAUGE_X = 50;
    public static final int CARGO_GAUGE_X = 100;
    public static final int GAUGE_Y = METER_AREA_Y + 10;
    public static final int GAUGE_W = 26;
    public static final int GAUGE_H = 20;

    // プレイヤーインベントリのblit基準位置
    public static final int PLAYER_INV_Y = CHEST_AREA_H;       // 71
    // 実際のプレイヤーインベントリスロットのY(vanilla準拠で+14)
    public static final int PLAYER_INV_ROW_Y = PLAYER_INV_Y + 14;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 72;
    public static final int IMAGE_H = PLAYER_INV_Y + 96;       // 167

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = IMAGE_H;
        this.inventoryLabelY = PLAYER_INV_Y - 10;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // ヘッダー+スロット3行分(=3行チェスト相当)を丸ごとblit。
        // 枠線・背景・スロットの穴がすべてテクスチャから来るので、自前の枠線描画は不要。
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, CHEST_AREA_H);

        // 下2行分のスロット穴の上にメーターを重ねて隠し、Fuel/Cargoを表示する。
        drawFluidGauges(g, x, y);

        // プレイヤーインベントリ+ホットバー: vanilla共通の固定96px領域をそのままblit。
        g.blit(TEXTURE, x, y + PLAYER_INV_Y, 0, 126, imageWidth, 96);
    }

    private void drawFluidGauges(GuiGraphics g, int x, int y) {
        int fuel = menu.getFuel(), maxFuel = Math.max(1, menu.getMaxFuel());
        int cargo = menu.getCargoFluid(), maxCargo = Math.max(1, menu.getMaxCargoFluid());

        drawGauge(g, x + FUEL_GAUGE_X, y + GAUGE_Y, fuel, maxFuel, 0xFFE05A2B);
        drawGauge(g, x + CARGO_GAUGE_X, y + GAUGE_Y, cargo, maxCargo, 0xFF4AA8E0);

        g.drawString(font, "Fuel", x + FUEL_GAUGE_X, y + GAUGE_Y - 10, 0xFF404040, false);
        g.drawString(font, "Cargo", x + CARGO_GAUGE_X, y + GAUGE_Y - 10, 0xFF404040, false);
    }

    /** 縦型ゲージ(下から上に充填) + 暗い枠で囲む */
    private void drawGauge(GuiGraphics g, int gx, int gy, int value, int max, int fillColor) {
        // 枠(暗い縁)
        g.fill(gx - 1, gy - 1, gx + GAUGE_W + 1, gy + GAUGE_H + 1, 0xFF373737);
        // 背景(空のタンク)
        g.fill(gx, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFF1A1A1A);
        // 充填(下から上へ)
        int fillH = (int) ((long) GAUGE_H * value / max);
        if (fillH > 0) {
            g.fill(gx, gy + GAUGE_H - fillH, gx + GAUGE_W, gy + GAUGE_H, fillColor);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = leftPos, y = topPos;
        if (isMouseOverBar(mouseX, mouseY, x + FUEL_GAUGE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H)) {
            g.renderTooltip(font, java.util.List.of(
                    menu.getFuelTypeName().getVisualOrderText(),
                    Component.literal(menu.getFuel() + " / " + menu.getMaxFuel() + " mB").getVisualOrderText()
            ), mouseX, mouseY);
        } else if (isMouseOverBar(mouseX, mouseY, x + CARGO_GAUGE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H)) {
            g.renderTooltip(font, java.util.List.of(
                    menu.getCargoFluidTypeName().getVisualOrderText(),
                    Component.literal(menu.getCargoFluid() + " / " + menu.getMaxCargoFluid() + " mB").getVisualOrderText()
            ), mouseX, mouseY);
        }
    }

    private boolean isMouseOverBar(int mouseX, int mouseY, int barX, int barY, int barW, int barH) {
        return mouseX >= barX && mouseX <= barX + barW
            && mouseY >= barY && mouseY <= barY + barH;
    }
}
