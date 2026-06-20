package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

/**
 * ロケット自身のインベントリと燃料/カーゴ流体タンクを表示・操作するためのメニュー。
 * ランチパッド↔ロケット間で「移動」させるAPI(loadAllItems等)は廃止されたが、
 * ランチパッドのパイプ・ホッパー接続経由でも同じインベントリ/タンクへ自動的に
 * 出し入れできる（着地中のみ）。このGUIはプレイヤーが手動で直接操作する手段。
 * 燃料/カーゴ流体タンクの表示は読み取り専用（バケツでの直接給排は廃止）。
 */
public class RocketMenu extends net.minecraft.world.inventory.AbstractContainerMenu {

    @Nullable private final CargoRocketEntity rocket;
    private final ContainerData data;

    // [0]=fuel, [1]=maxFuel, [2]=cargo, [3]=maxCargo, [4]=fuelFluidId, [5]=cargoFluidId
    private static final int DATA_COUNT = 6;

    // ネットワーク経由でクライアントに同期された値のキャッシュ。
    // FluidTankはSynchedEntityDataの対象外でエンティティとして自動同期されないため、
    // クライアント側のrocket.fuelTank等を直接読んでも更新されない。ContainerDataの
    // set()で受け取った同期値をここに保持し、GUI表示はこちらを参照する。
    private final int[] syncedValues = new int[DATA_COUNT];

    public RocketMenu(int syncId, Inventory playerInventory, @Nullable CargoRocketEntity rocket) {
        super(AdAstraCargoRockets.CARGO_ROCKET_MENU.getMenuType().get(), syncId);
        this.rocket = rocket;

        this.data = rocket != null ? new ContainerData() {
            @Override public int get(int i) {
                // サーバー側ではbroadcastChanges()が変更検知のために毎tick呼ぶので、
                // ここは常にロケットの実値を返す（クライアント側では直接呼ばれない）。
                return switch (i) {
                    case 0 -> rocket.fuelTank.getFluidAmount();
                    case 1 -> rocket.fuelTank.getCapacity();
                    case 2 -> rocket.cargoFluidTank.getFluidAmount();
                    case 3 -> rocket.cargoFluidTank.getCapacity();
                    // 流体の種類は文字列をContainerDataで同期できないため、レジストリIDを
                    // 数値として送る（フルードレジストリIDはゲーム実行中は安定している）。
                    case 4 -> ForgeRegistries.FLUIDS.getId(rocket.fuelTank.getFluid().getFluid());
                    case 5 -> ForgeRegistries.FLUIDS.getId(rocket.cargoFluidTank.getFluid().getFluid());
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {
                if (i >= 0 && i < DATA_COUNT) syncedValues[i] = v;
            }
            @Override public int getCount() { return DATA_COUNT; }
        } : new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
        broadcastChanges();

        if (rocket != null) {
            for (int i = 0; i < 9; i++)
                addSlot(new Slot(rocket.getInventory(), i, 8 + i * 18, RocketScreen.SLOT_Y));
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, RocketScreen.PLAYER_INV_Y + row * 18));

        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, RocketScreen.HOTBAR_Y));
    }

    public int getFuel()    { return syncedValues[0]; }
    public int getMaxFuel() { return syncedValues[1]; }
    public int getCargoFluid()    { return syncedValues[2]; }
    public int getMaxCargoFluid() { return syncedValues[3]; }
    public Component getFuelTypeName()       { return fluidName(syncedValues[4]); }
    public Component getCargoFluidTypeName() { return fluidName(syncedValues[5]); }

    private static Component fluidName(int fluidId) {
        var fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) return Component.literal("Empty");
        return new FluidStack(fluid, 1).getDisplayName();
    }

    @Override public boolean stillValid(Player p) {
        return rocket != null && rocket.isAlive() && "grounded".equals(rocket.getFlightState());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        int cs = rocket != null ? 9 : 0; // 9 inventory slots
        if (index < cs) {
            if (!moveItemStackTo(stack, cs, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, cs, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }
}
