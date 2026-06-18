package uk.co.cablepost.ad_astra_cargo_rockets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge replacement for f_tech_core's AbstractMachineBlockEntity.
 * Provides 18 slots (9 input + 9 output), energy storage, and Forge capability support.
 */
public abstract class AbstractMachineBlockEntity extends BlockEntity implements Container {

    public final int[] _inputSlots;
    public final int[] _outputSlots;

    protected final List<ItemStack> _inventory;
    protected final EnergyStorage _energyStorage;

    private final LazyOptional<IItemHandler> itemHandler;
    private final LazyOptional<net.minecraftforge.energy.IEnergyStorage> energyHandler;

    public AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       int[] inputSlots, int[] outputSlots,
                                       int energyCapacity, int energyMaxInsert,
                                       int energyMaxExtract, boolean doProcessing) {
        super(type, pos, state);
        this._inputSlots = inputSlots;
        this._outputSlots = outputSlots;
        int totalSlots = inputSlots.length + outputSlots.length;
        this._inventory = new ArrayList<>(totalSlots);
        for (int i = 0; i < totalSlots; i++) _inventory.add(ItemStack.EMPTY);

        this._energyStorage = new EnergyStorage(energyCapacity, energyMaxInsert, energyMaxExtract) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (!simulate) setChanged();
                return received;
            }
        };

        // _inventory をラップする ItemStackHandler （別リストを持たず GUI と同じデータを参照）
        IItemHandler handler = new IItemHandler() {
            @Override public int getSlots() { return totalSlots; }

            @Override public @Nonnull ItemStack getStackInSlot(int slot) {
                return slot >= 0 && slot < _inventory.size() ? _inventory.get(slot) : ItemStack.EMPTY;
            }

            @Override public @Nonnull ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) return ItemStack.EMPTY;
                if (!isItemValid(slot, stack)) return stack;

                ItemStack existing = _inventory.get(slot);
                int limit = getSlotLimit(slot);

                if (!existing.isEmpty()) {
                    if (!ItemStack.isSameItemSameTags(stack, existing)) return stack;
                    limit -= existing.getCount();
                }
                if (limit <= 0) return stack;

                boolean reachedLimit = stack.getCount() > limit;

                if (!simulate) {
                    if (existing.isEmpty()) {
                        _inventory.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack.copy());
                    } else {
                        existing.grow(reachedLimit ? limit : stack.getCount());
                    }
                    setChanged();
                }

                return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
            }

            @Override public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (amount == 0) return ItemStack.EMPTY;
                ItemStack existing = _inventory.get(slot);
                if (existing.isEmpty()) return ItemStack.EMPTY;

                int toExtract = Math.min(amount, existing.getCount());

                if (!simulate) {
                    ItemStack split = existing.copy();
                    split.setCount(toExtract);
                    existing.shrink(toExtract);
                    if (existing.isEmpty()) {
                        _inventory.set(slot, ItemStack.EMPTY);
                    }
                    setChanged();
                    return split;
                } else {
                    ItemStack split = existing.copy();
                    split.setCount(toExtract);
                    return split;
                }
            }

            @Override public int getSlotLimit(int slot) { return 64; }

            @Override public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return AbstractMachineBlockEntity.this.canPlaceItem(slot, stack);
            }
        };
        this.itemHandler = LazyOptional.of(() -> handler);
        this.energyHandler = LazyOptional.of(() -> _energyStorage);
    }

    // --- Container impl ---

    @Override
    public int getContainerSize() { return _inventory.size(); }

    @Override
    public boolean isEmpty() {
        return _inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < _inventory.size() ? _inventory.get(slot) : ItemStack.EMPTY;
    }

    /** Alias matching the Fabric naming used in the rest of the code */
    public ItemStack getStack(int slot) { return getItem(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= _inventory.size()) return ItemStack.EMPTY;
        ItemStack stack = _inventory.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) _inventory.set(slot, ItemStack.EMPTY);
        setChanged();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= _inventory.size()) return ItemStack.EMPTY;
        ItemStack old = _inventory.get(slot);
        _inventory.set(slot, ItemStack.EMPTY);
        return old;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < _inventory.size()) {
            _inventory.set(slot, stack);
            setChanged();
        }
    }

    /** Alias matching Fabric naming */
    public void setStack(int slot, ItemStack stack) { setItem(slot, stack); }

    /** size() alias */
    public int size() { return getContainerSize(); }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return true; }

    @Override
    public void clearContent() {
        for (int i = 0; i < _inventory.size(); i++) _inventory.set(i, ItemStack.EMPTY);
        setChanged();
    }

    // --- NBT ---

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // Inventory
        ListTag items = new ListTag();
        for (int i = 0; i < _inventory.size(); i++) {
            ItemStack stack = _inventory.get(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte) i);
                stack.save(slotTag);
                items.add(slotTag);
            }
        }
        tag.put("Items", items);
        // Energy
        tag.putInt("Energy", _energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Inventory
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag slotTag = items.getCompound(i);
            int slot = slotTag.getByte("Slot") & 0xFF;
            if (slot < _inventory.size()) {
                _inventory.set(slot, ItemStack.of(slotTag));
            }
        }
        // Energy: EnergyStorageのmaxInsert制限を迂回してリフレクションで直接セット
        int savedEnergy = tag.getInt("Energy");
        try {
            java.lang.reflect.Field f = net.minecraftforge.energy.EnergyStorage.class.getDeclaredField("energy");
            f.setAccessible(true);
            f.set(_energyStorage, Math.min(savedEnergy, _energyStorage.getMaxEnergyStored()));
        } catch (Exception e) {
            _energyStorage.receiveEnergy(savedEnergy, false);
        }
    }

    // --- Capabilities ---

    @Override
    public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandler.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        energyHandler.invalidate();
    }

    public abstract int getMaxProcessProgress();
    public abstract int processEnergyConsumption();
}
