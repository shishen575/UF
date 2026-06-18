package uk.co.cablepost.ad_astra_cargo_rockets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractFluidMachineBlockEntity extends AbstractMachineBlockEntity {

    /** 32 buckets = 32000 mB */
    public static final int FLUID_CAPACITY = 32000; // 32B 燃料タンク
    public static final int CARGO_FLUID_CAPACITY = 32000; // 32B 貨物タンク

    // 燃料タンク（燃料専用）
    public final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return true;
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    // 貨物タンク（輸送用液体）
    public final FluidTank cargoFluidTank = new FluidTank(CARGO_FLUID_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return true;
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final LazyOptional<IFluidHandler> fluidHandler      = LazyOptional.of(() -> fluidTank);
    private final LazyOptional<IFluidHandler> cargoFluidHandler = LazyOptional.of(() -> cargoFluidTank);

    public AbstractFluidMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                            int[] inputSlots, int[] outputSlots,
                                            int energyCapacity, int energyMaxInsert,
                                            int energyMaxExtract, boolean doProcessing) {
        super(type, pos, state, inputSlots, outputSlots, energyCapacity, energyMaxInsert, energyMaxExtract, doProcessing);
    }

    @Override
    public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // 下面 = 燃料タンク、それ以外 = 貨物タンク
            if (side == Direction.DOWN) {
                return fluidHandler.cast();
            }
            return cargoFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
        cargoFluidHandler.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag fluidTag = new CompoundTag();
        fluidTank.writeToNBT(fluidTag);
        tag.put("FluidContent", fluidTag);
        CompoundTag cargoFluidTag = new CompoundTag();
        cargoFluidTank.writeToNBT(cargoFluidTag);
        tag.put("CargoFluidContent", cargoFluidTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("FluidContent")) {
            fluidTank.readFromNBT(tag.getCompound("FluidContent"));
        }
        if (tag.contains("CargoFluidContent")) {
            cargoFluidTank.readFromNBT(tag.getCompound("CargoFluidContent"));
        }
    }
}
