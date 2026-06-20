package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * ロケットのインベントリ・燃料/カーゴ流体タンクを表示するGUI画面。
 * vanillaの本物のテクスチャ(generic_54.png)を部分blitする方式は、自前で描く
 * Fuel/Cargo表示エリアとの継ぎ目に縁の不連続が出てしまうため不採用とし、
 * パネル全体を一枚の継続した塗りつぶしで描く（外枠+内側パネル+スロット枠）。
 */
public class RocketScreen extends AbstractContainerScreen<RocketMenu> {

    public static final int SLOT_Y = 18;
    // 上段インベントリの枠はy=17〜35を占めるため、"Fuel"/"Cargo"ラベル(GAUGE_Y-10)が
    // それと重ならないよう、燃料/カーゴ行全体を十分下げて配置する。
    public static final int FUEL_GAUGE_X = 50;
    public static final int CARGO_GAUGE_X = 90;
    public static final int GAUGE_Y = 48;
    public static final int GAUGE_W = 30;
    public static final int GAUGE_H = 16;
    public static final int PLAYER_INV_Y = 86;
    public static final int HOTBAR_Y = 144;
    public static final int IMAGE_H = 166;

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = IMAGE_H;
        this.inventoryLabelY = PLAYER_INV_Y - 10;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // パネル全体を一枚の継続した塗りつぶしにすることで、縁の不連続を無くす。
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);

        drawSlotGrid(g, x, y);
        drawFluidGauges(g, x, y);
    }

    private void drawSlotGrid(GuiGraphics g, int x, int y) {
        for (int i = 0; i < 9; i++) drawSlotFrame(g, x + 8 + i * 18, y + SLOT_Y);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotFrame(g, x + 8 + col * 18, y + PLAYER_INV_Y + row * 18);
        for (int col = 0; col < 9; col++)
            drawSlotFrame(g, x + 8 + col * 18, y + HOTBAR_Y);
    }

    /** バニラのチェストGUIのスロットと同じ配色: 単色の暗い枠線 + 中間グレーの中身。 */
    private void drawSlotFrame(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
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
