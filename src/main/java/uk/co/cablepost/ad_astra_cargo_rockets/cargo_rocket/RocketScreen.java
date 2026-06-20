package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ロケットのインベントリ・燃料/カーゴ流体タンクを表示するGUI画面。
 * 背景・スロットの枠は手描きの塗りつぶしではなく、vanillaのチェスト等と共通の本物の
 * コンテナテクスチャ(generic_54.png)をそのままblitして使う。これによりvanillaの
 * インベントリ画面と完全に同じ質感・配色になる。
 * vanillaのChestScreenと同じ構造: 「ヘッダー+チェスト行数分のスロット」と
 * 「プレイヤーインベントリ+ホットバー(常に固定の96px領域)」の2回のblitで構成される。
 * ロケット自身の9スロットは「1行のチェスト」として扱い、その下に独自のFuel/Cargo
 * ゲージ表示エリアを挟んでからプレイヤーインベントリ部分を続ける。
 */
public class RocketScreen extends AbstractContainerScreen<RocketMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    public static final int SLOT_Y = 18;
    // ヘッダー17px + チェスト1行18px分の本物のテクスチャ領域
    private static final int ROCKET_ROW_H = 1 * 18 + 17;

    public static final int FUEL_GAUGE_X = 50;
    public static final int CARGO_GAUGE_X = 90;
    public static final int GAUGE_Y = ROCKET_ROW_H + 12;
    public static final int GAUGE_W = 30;
    public static final int GAUGE_H = 16;
    public static final int PLAYER_INV_Y = ROCKET_ROW_H + 40;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;
    public static final int IMAGE_H = PLAYER_INV_Y + 96;

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = IMAGE_H;
        this.inventoryLabelY = PLAYER_INV_Y - 10;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // ロケットの9スロット行: vanillaチェストのヘッダー+1行分の本物のテクスチャをそのまま使う
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, ROCKET_ROW_H);
        // Fuel/Cargoゲージ(このMOD独自の表示なので手描き)
        drawFluidGauges(g, x, y);
        // プレイヤーインベントリ+ホットバー: vanillaチェストGUIと共通の固定96px領域を
        // そのままblitする(行数に関わらず常にこの領域の見た目は同じ)
        g.blit(TEXTURE, x, y + PLAYER_INV_Y, 0, 126, imageWidth, 96);
    }

    private void drawFluidGauges(GuiGraphics g, int x, int y) {
        int fuel = menu.getFuel(), maxFuel = Math.max(1, menu.getMaxFuel());
        int cargo = menu.getCargoFluid(), maxCargo = Math.max(1, menu.getMaxCargoFluid());

        g.fill(x + FUEL_GAUGE_X, y + GAUGE_Y, x + FUEL_GAUGE_X + GAUGE_W, y + GAUGE_Y + GAUGE_H, 0xFF1A1A1A);
        int fuelFillW = (int) ((long) GAUGE_W * fuel / maxFuel);
        g.fill(x + FUEL_GAUGE_X, y + GAUGE_Y, x + FUEL_GAUGE_X + fuelFillW, y + GAUGE_Y + GAUGE_H, 0xFFE05A2B);

        g.fill(x + CARGO_GAUGE_X, y + GAUGE_Y, x + CARGO_GAUGE_X + GAUGE_W, y + GAUGE_Y + GAUGE_H, 0xFF1A1A1A);
        int cargoFillW = (int) ((long) GAUGE_W * cargo / maxCargo);
        g.fill(x + CARGO_GAUGE_X, y + GAUGE_Y, x + CARGO_GAUGE_X + cargoFillW, y + GAUGE_Y + GAUGE_H, 0xFF4AA8E0);

        g.drawString(font, "Fuel", x + FUEL_GAUGE_X, y + GAUGE_Y - 10, 0xFF404040, false);
        g.drawString(font, "Cargo", x + CARGO_GAUGE_X, y + GAUGE_Y - 10, 0xFF404040, false);
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
