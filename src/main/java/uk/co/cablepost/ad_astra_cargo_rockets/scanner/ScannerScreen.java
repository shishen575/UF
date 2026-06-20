package uk.co.cablepost.ad_astra_cargo_rockets.scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ScannerScreen extends Screen {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 250;
    private static final int LIST_WIDTH = 130;
    private static final int ROW_HEIGHT = 16;
    // 名前ラベル+入力欄+ボタンの行全体の高さ（ラベルを上、入力欄を下に積む構成のため）
    private static final int NAME_ROW_HEIGHT = 30;

    private List<RocketInfo> rockets = new ArrayList<>();
    private RocketInfo selected = null;
    private int scrollOffset = 0;
    private EditBox nameBox;
    private Button renameButton;
    private long lastRequestTime = 0;
    // 名前変更直後、サーバーの応答が追いつく前に古い名前で上書きされるのを防ぐための一時保持
    private Integer pendingRenameEntityId = null;
    private String pendingRenameName = null;
    // 名前欄に最後に値をセットした際の選択ロケットID。同じロケットを選択し続けている間は
    // 2秒ごとの自動更新でnameBoxの内容を上書きしないようにするためのガード
    // （これが無いと入力中でも自動更新でテキストが消えてしまう）。
    private Integer nameBoxBoundEntityId = null;

    public ScannerScreen() {
        super(Component.literal("Rocket Scanner"));
    }

    /** アイテム右クリック時に呼ばれる: 画面を開く（初回のリクエストはrender()の自動更新ロジックに任せる） */
    public static void openAndRequest() {
        ScannerScreen screen = new ScannerScreen();
        Minecraft.getInstance().setScreen(screen);
    }

    /** WIDTH/HEIGHTが画面サイズを超える場合に縮小したパネルサイズ。画面外へのはみ出しを防ぐ。 */
    private int panelWidth() { return Math.min(WIDTH, width - 16); }
    private int panelHeight() { return Math.min(HEIGHT, height - 16); }
    private int originX() { return (width - panelWidth()) / 2; }
    private int originY() { return (height - panelHeight()) / 2; }

    @Override
    protected void init() {
        super.init();
        int ox = originX();
        int oy = originY();
        int ph = panelHeight();
        int dx = ox + LIST_WIDTH + 12;
        // "Name:" ラベルの下に入力欄とボタンを配置する（横並びだとラベルが隠れていたため）
        int rowY = oy + ph - NAME_ROW_HEIGHT + 10;

        nameBox = new EditBox(font, dx, rowY, 110, 16, Component.literal("name"));
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        renameButton = Button.builder(Component.literal("Rename"), b -> doRename())
                .bounds(dx + 114, rowY - 1, 50, 18)
                .build();
        addRenderableWidget(renameButton);

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> refresh())
                .bounds(ox + 8, oy + ph - 24, LIST_WIDTH - 8, 16)
                .build());

        updateSelectionFields();
    }

    private void refresh() {
        ScannerNetwork.CHANNEL.sendToServer(new RequestRocketListPacket());
        lastRequestTime = System.currentTimeMillis();
    }

    private void doRename() {
        if (selected == null) return;
        String newName = nameBox.getValue();
        ScannerNetwork.CHANNEL.sendToServer(new RenameRocketPacket(selected.entityId, newName));
        selected.name = newName;
        // サーバーの応答(RocketListResponsePacket)がこの名前を反映するまで、
        // 自動更新で古い名前に巻き戻されないようにペンディング状態として保持する。
        pendingRenameEntityId = selected.entityId;
        pendingRenameName = newName;
    }

    /** ネットワークパケット受信時にサーバーから呼ばれる (RocketListResponsePacket経由) */
    public void updateRocketList(List<RocketInfo> newList) {
        // 選択中のロケットがまだリストにあれば選択を維持する
        Integer keepId = selected != null ? selected.entityId : null;

        // 名前変更がまだサーバーに反映されていない場合、リストの値で上書きしない。
        // サーバー側の名前がこちらが送った名前と一致した時点でペンディングを解除する。
        if (pendingRenameEntityId != null) {
            for (RocketInfo r : newList) {
                if (r.entityId == pendingRenameEntityId.intValue()) {
                    if (pendingRenameName.equals(r.name)) {
                        pendingRenameEntityId = null;
                        pendingRenameName = null;
                    } else {
                        r.name = pendingRenameName;
                    }
                    break;
                }
            }
        }

        this.rockets = newList;
        this.selected = null;
        if (keepId != null) {
            for (RocketInfo r : rockets) {
                if (r.entityId == keepId) { selected = r; break; }
            }
        }
        updateSelectionFields();
    }

    private void updateSelectionFields() {
        Integer curId = selected != null ? selected.entityId : null;
        // 選択ロケットが変わった時だけテキストを書き換える。同じロケットを選択し続けて
        // いる間は、2秒ごとの自動更新が来ても入力中の文字を消さないようにする。
        if (nameBox != null && !java.util.Objects.equals(curId, nameBoxBoundEntityId)) {
            nameBox.setValue(selected != null ? selected.name : "");
            nameBoxBoundEntityId = curId;
        }
        if (renameButton != null) {
            renameButton.active = selected != null;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 2秒ごとに自動更新（手動Refreshボタンも併用可）
        long now = System.currentTimeMillis();
        if (now - lastRequestTime > 2000) {
            refresh();
        }

        renderBackground(g);
        int ox = originX();
        int oy = originY();
        int pw = panelWidth();
        int ph = panelHeight();

        // パネル背景
        g.fill(ox, oy, ox + pw, oy + ph, 0xFF1A1A1A);
        g.fill(ox, oy, ox + pw, oy + 20, 0xFF2A2A2A);
        g.drawString(font, "Rocket Scanner", ox + 8, oy + 6, 0xFFFFFF, false);

        // 左: ロケット一覧
        drawRocketList(g, ox, oy, ph, mouseX, mouseY);

        // 右: 詳細パネル
        drawDetailPanel(g, ox, oy, pw, ph);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawRocketList(GuiGraphics g, int ox, int oy, int ph, int mouseX, int mouseY) {
        int listX = ox + 4;
        int listY = oy + 24;
        int listH = ph - 24 - 28;

        g.fill(listX, listY, listX + LIST_WIDTH, listY + listH, 0xFF0F0F0F);

        if (rockets.isEmpty()) {
            g.drawString(font, "No rockets found", listX + 4, listY + 6, 0xAAAAAA, false);
            return;
        }

        int maxVisible = listH / ROW_HEIGHT;
        int maxScroll = Math.max(0, rockets.size() - maxVisible);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int rowY = listY;
        for (int i = scrollOffset; i < rockets.size(); i++) {
            RocketInfo r = rockets.get(i);
            if (rowY + ROW_HEIGHT > listY + listH) break;
            boolean isSelected = selected != null && selected.entityId == r.entityId;
            boolean isHover = mouseX >= listX && mouseX <= listX + LIST_WIDTH
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (isSelected) g.fill(listX, rowY, listX + LIST_WIDTH, rowY + ROW_HEIGHT, 0xFF3A3A5A);
            else if (isHover) g.fill(listX, rowY, listX + LIST_WIDTH, rowY + ROW_HEIGHT, 0xFF2A2A2A);

            String label = r.name != null && !r.name.isEmpty() ? r.name : ("Rocket #" + r.entityId);
            g.drawString(font, trimToWidth(label, LIST_WIDTH - 8), listX + 4, rowY + 4, 0xFFFFFF, false);
            rowY += ROW_HEIGHT;
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        int[] codePoints = text.codePoints().toArray();
        int count = codePoints.length;
        while (count > 1) {
            String trimmed = new String(codePoints, 0, count) + "...";
            if (font.width(trimmed) <= maxWidth) return trimmed;
            count--;
        }
        return new String(codePoints, 0, count) + "...";
    }

    private void drawDetailPanel(GuiGraphics g, int ox, int oy, int pw, int ph) {
        int dx = ox + LIST_WIDTH + 12;
        int dy = oy + 24;
        int textMaxWidth = pw - LIST_WIDTH - 24;

        if (selected == null) {
            g.drawString(font, "Select a rocket from the list", dx, dy, 0xAAAAAA, false);
            return;
        }

        g.drawString(font, "Tier " + selected.tier + "  |  " + selected.dimension, dx, dy, 0xCCCCCC, false);
        g.drawString(font, "Pos: " + selected.x + ", " + selected.y + ", " + selected.z, dx, dy + 11, 0xCCCCCC, false);

        int y = dy + 26;
        String stateLabel = describeFlightState(selected.flightState);
        if (selected.targetPlanet != null && !selected.targetPlanet.isEmpty()) {
            stateLabel += " -> " + shortId(selected.targetPlanet);
        }
        g.drawString(font, "State: " + trimToWidth(stateLabel, textMaxWidth), dx, y, 0xFFFFAA, false);
        y += 11;

        String reason = !selected.statusOverride.isEmpty()
                ? selected.statusOverride
                : describeWaitReason(selected.autoWaitReason);
        g.drawString(font, "Waiting on: " + trimToWidth(reason, textMaxWidth), dx, y, 0xFFAA88, false);
        y += 15;

        // 燃料・カーゴ流体はロケット自身のタンクの値なので、飛行中でも常に表示できる。
        g.drawString(font, "Fuel: " + selected.fuel + "/" + selected.maxFuel + " mB ("
                + shortId(selected.fuelType) + ")", dx, y, 0x88AAFF, false);
        y += 10;
        g.drawString(font, "Cargo: " + selected.cargoFluid + "/" + selected.maxCargoFluid
                + " mB (" + shortId(selected.cargoFluidType) + ")", dx, y, 0x88FFAA, false);
        y += 14;

        g.drawString(font, "Inventory:", dx, y, 0xCCCCCC, false);
        y += 11;
        int invBottom = oy + ph - NAME_ROW_HEIGHT - 4;
        if (selected.inventory.isEmpty()) {
            g.drawString(font, "(empty)", dx + 4, y, 0x888888, false);
        } else {
            for (RocketInfo.SlotInfo slot : selected.inventory) {
                if (y > invBottom) {
                    g.drawString(font, "...", dx + 4, y, 0x888888, false);
                    break;
                }
                String line = "[" + slot.slot + "] " + slot.displayName + " x" + slot.count;
                g.drawString(font, trimToWidth(line, pw - LIST_WIDTH - 28), dx + 4, y, 0xDDDDDD, false);
                y += 10;
            }
        }

        // "Name:" ラベルは入力欄の左ではなく上に置く。横並びだと入力欄の幅を削るか
        // ラベルと重なるため、縦に並べて確実に両方とも視認できるようにする。
        g.drawString(font, "Name:", dx, oy + ph - NAME_ROW_HEIGHT - 2, 0xCCCCCC, false);
    }

    /** 名前空間付きID(流体や惑星)の名前空間部分を省略して短く表示する (例: "ad_astra:fuel" -> "fuel") */
    private static String shortId(String id) {
        if (id == null || id.isEmpty()) return "empty";
        int idx = id.indexOf(':');
        return idx >= 0 ? id.substring(idx + 1) : id;
    }

    private static String describeFlightState(String state) {
        return switch (state) {
            case "ascending" -> "Ascending";
            case "descending" -> "Descending";
            case "grounded" -> "Grounded";
            default -> "Unknown";
        };
    }

    private static String describeWaitReason(String reason) {
        return switch (reason) {
            case "not_enough_energy" -> "Not enough energy";
            case "not_enough_fuel" -> "Not enough fuel";
            case "no_rocket" -> "N/A";
            case "no_launchpad" -> "No launchpad nearby";
            case "in_flight" -> "Currently in flight";
            case "idle" -> "Ready (idle)";
            default -> "Unknown";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int ox = originX();
        int oy = originY();
        int ph = panelHeight();
        int listX = ox + 4;
        int listY = oy + 24;
        int listH = ph - 24 - 28;

        if (mouseX >= listX && mouseX <= listX + LIST_WIDTH && mouseY >= listY && mouseY < listY + listH) {
            int rowIndex = (int) ((mouseY - listY) / ROW_HEIGHT);
            if (rowIndex >= 0 && rowIndex < rockets.size()) {
                selected = rockets.get(rowIndex);
                updateSelectionFields();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int ox = originX(), oy = originY(), ph = panelHeight();
        int listX = ox + 4, listY = oy + 24, listH = ph - 24 - 28;
        if (mouseX >= listX && mouseX <= listX + LIST_WIDTH && mouseY >= listY && mouseY < listY + listH) {
            int maxVisible = listH / ROW_HEIGHT;
            int maxScroll = Math.max(0, rockets.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
