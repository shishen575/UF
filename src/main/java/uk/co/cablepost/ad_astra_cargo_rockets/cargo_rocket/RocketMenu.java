package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

/**
 * ロケット自身のインベントリと燃料/カーゴ流体タンクを表示・操作するためのメニュー。
 * ランチパッド↔ロケット間で「移動」させるAPI(loadAllItems等)は廃止されたが、
 * ランチパッドのパイプ・ホッパー接続経由でも同じインベントリ/タンクへ自動的に
 * 出し入れできる（着地中のみ）。このGUIはプレイヤーが手動で直接操作する手段。
 */
public class RocketMenu extends net.minecraft.world.inventory.AbstractContainerMenu {

    @Nullable private final CargoRocketEntity rocket;
    private final ContainerData data;

    // [0]=fuel, [1]=maxFuel, [2]=cargo, [3]=maxCargo
    private static final int DATA_COUNT = 4;

    public RocketMenu(int syncId, Inventory playerInventory, @Nullable CargoRocketEntity rocket) {
        super(AdAstraCargoRockets.CARGO_ROCKET_MENU.getMenuType().get(), syncId);
        this.rocket = rocket;

        this.data = rocket != null ? new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> rocket.fuelTank.getFluidAmount();
                    case 1 -> rocket.fuelTank.getCapacity();
                    case 2 -> rocket.cargoFluidTank.getFluidAmount();
                    case 3 -> rocket.cargoFluidTank.getCapacity();
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return DATA_COUNT; }
        } : new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
        broadcastChanges();

        if (rocket != null) {
            for (int i = 0; i < 9; i++)
                addSlot(new Slot(rocket.getInventory(), i, 8 + i * 18, RocketScreen.SLOT_Y));

            // バケツ専用スロット: 満タンのバケツを入れるとタンクに注がれ空バケツに変わる。
            // 空バケツを入れるとタンクから汲み出して満タンのバケツに変わる。
            addSlot(new FluidBucketSlot(rocket.getBucketSlots(), 0, RocketScreen.FUEL_SLOT_X, RocketScreen.FLUID_SLOT_Y, rocket.fuelTank));
            addSlot(new FluidBucketSlot(rocket.getBucketSlots(), 1, RocketScreen.CARGO_SLOT_X, RocketScreen.FLUID_SLOT_Y, rocket.cargoFluidTank));
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, RocketScreen.PLAYER_INV_Y + row * 18));

        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, RocketScreen.HOTBAR_Y));
    }

    public int getFuel()    { return data.get(0); }
    public int getMaxFuel() { return data.get(1); }
    public int getCargoFluid()    { return data.get(2); }
    public int getMaxCargoFluid() { return data.get(3); }

    @Override public boolean stillValid(Player p) {
        return rocket != null && rocket.isAlive() && "grounded".equals(rocket.getFlightState());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        int cs = rocket != null ? 11 : 0; // 9 inventory slots + 2 bucket slots
        if (index < cs) {
            if (!moveItemStackTo(stack, cs, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // バケツアイテムはまずバケツスロット(9,10)へ、それ以外は一般スロット(0-8)へ
            if (rocket != null && (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                    || stack.getItem() == net.minecraft.world.item.Items.BUCKET)) {
                if (!moveItemStackTo(stack, 9, 11, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    /**
     * バケツアイテムとFluidTankの相互変換を行う専用スロット。
     * 満タンのバケツ(例: lava_bucket, water_bucket, ad_astraの燃料バケツ等)を置くと、
     * その流体をタンクに注ぎ、スロットの中身は空バケツに変わる。
     * 逆に空バケツを置くと、タンクから汲み出して満タンのバケツに変える。
     * タンクが対応する量に満たない/空でない等で操作できない場合は何もしない。
     */
    private static class FluidBucketSlot extends Slot {
        private final net.minecraftforge.fluids.capability.templates.FluidTank tank;

        FluidBucketSlot(net.minecraft.world.Container container, int slot, int x, int y,
                        net.minecraftforge.fluids.capability.templates.FluidTank tank) {
            super(container, slot, x, y);
            this.tank = tank;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isBucketItem(stack);
        }

        private static boolean isBucketItem(ItemStack stack) {
            return stack.getItem() instanceof net.minecraft.world.item.BucketItem
                    || stack.getItem() == net.minecraft.world.item.Items.BUCKET;
        }

        private boolean converting = false;

        @Override
        public void set(ItemStack stack) {
            super.set(stack);
            tryConvert();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            tryConvert();
        }

        /**
         * 置かれたバケツをタンクとの間で変換できるなら変換する。
         * converting フラグで再入を防止する（変換時の set()/setChanged() 呼び出しが
         * 再度このメソッドを呼び、満タン→空→満タン...と無限再帰するのを防ぐため）。
         */
        private void tryConvert() {
            if (converting) return;
            ItemStack current = getItem();
            if (current.isEmpty()) return;

            converting = true;
            try {
                if (current.getItem() == net.minecraft.world.item.Items.BUCKET) {
                    // 空バケツ -> タンクから1バケツ(1000mB)汲み出して満タンバケツに変える
                    if (tank.getFluidAmount() >= 1000) {
                        var drained = tank.drain(1000, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        if (drained.getAmount() == 1000) {
                            var bucketItem = drained.getFluid().getBucket();
                            if (bucketItem != null) {
                                set(new ItemStack(bucketItem));
                            } else {
                                // バケツアイテムが定義されていない流体は汲み出せない(注ぎ戻す)
                                tank.fill(drained, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                            }
                        }
                    }
                } else if (current.getItem() instanceof net.minecraft.world.item.BucketItem bucketItem) {
                    // 満タンのバケツ -> タンクに注いで空バケツに変える
                    net.minecraftforge.fluids.FluidStack toFill = bucketItemToFluidStack(bucketItem);
                    if (!toFill.isEmpty()) {
                        int accepted = tank.fill(toFill.copy(), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                        if (accepted == 1000) {
                            tank.fill(toFill, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                            set(new ItemStack(net.minecraft.world.item.Items.BUCKET));
                        }
                    }
                }
            } finally {
                converting = false;
            }
        }

        private static net.minecraftforge.fluids.FluidStack bucketItemToFluidStack(net.minecraft.world.item.BucketItem bucketItem) {
            var fluid = bucketItem.getFluid();
            if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            return new net.minecraftforge.fluids.FluidStack(fluid, 1000);
        }
    }
}

