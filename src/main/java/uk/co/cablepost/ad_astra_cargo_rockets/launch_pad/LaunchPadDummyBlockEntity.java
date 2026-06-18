package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class LaunchPadDummyBlockEntity extends BlockEntity {

    public LaunchPadDummyBlockEntity(BlockPos pos, BlockState state) {
        super(AdAstraCargoRockets.LAUNCH_PAD.getDummyBlockEntity().get(), pos, state);
    }

    @Override
    public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        // 中心のBEにCapabilityを転送
        if (level != null) {
            BlockPos center = LaunchPadDummyBlock.findCenterPos(level, worldPosition);
            if (center != null) {
                BlockEntity centerBE = level.getBlockEntity(center);
                if (centerBE instanceof LaunchPadBlockEntity launchPad) {
                    return launchPad.getCapability(cap, side);
                }
            }
        }
        return super.getCapability(cap, side);
    }
}
