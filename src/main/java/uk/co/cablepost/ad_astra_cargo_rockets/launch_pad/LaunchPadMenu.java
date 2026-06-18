package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

/**
 * ランチパッドのメニュー。
 * v1.2.4でアイテム/燃料/カーゴ流体はロケット自身のGUIに移管されたため、
 * ここではエネルギー残量の表示のみを行う（アイテムスロットは無し）。
 */
public class LaunchPadMenu extends AbstractContainerMenu {

    @Nullable private final LaunchPadBlockEntity blockEntity;
    private final ContainerData data;

    // 各値を上位・下位に分割してshortの制限を回避
    // [0]=energy上位, [1]=energy下位, [2]=maxEnergy上位, [3]=maxEnergy下位
    private static final int DATA_COUNT = 4;

    public LaunchPadMenu(int syncId, Inventory playerInventory, @Nullable LaunchPadBlockEntity blockEntity) {
        super(AdAstraCargoRockets.LAUNCH_PAD.getMenuType().get(), syncId);
        this.blockEntity = blockEntity;

        this.data = blockEntity != null ? new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> blockEntity.getEnergy() >> 16;
                    case 1 -> blockEntity.getEnergy() & 0xFFFF;
                    case 2 -> blockEntity.getMaxEnergy() >> 16;
                    case 3 -> blockEntity.getMaxEnergy() & 0xFFFF;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return DATA_COUNT; }
        } : new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
        // 初回同期を強制
        broadcastChanges();

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row*9 + 9,
                        8 + col*18, LaunchPadScreen.PLAYER_INV_Y + row*18));

        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col*18, LaunchPadScreen.HOTBAR_Y));
    }

    private int reconstruct(int high, int low) {
        // ContainerDataはshortで同期されるため符号拡張に注意
        int h = data.get(high) & 0xFFFF;
        int l = data.get(low)  & 0xFFFF;
        return (h << 16) | l;
    }

    public int getEnergy()    { return reconstruct(0, 1); }
    public int getMaxEnergy() { return reconstruct(2, 3); }

    @Override public boolean stillValid(Player p) {
        return blockEntity == null || blockEntity.stillValid(p);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        // プレイヤーインベントリ同士でのみ移動可能（ランチパッド側にスロットが無いため）
        if (!moveItemStackTo(stack, 0, slots.size(), false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }
}
