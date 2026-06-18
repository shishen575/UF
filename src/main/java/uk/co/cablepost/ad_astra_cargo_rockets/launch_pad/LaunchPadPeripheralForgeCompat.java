package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

public class LaunchPadPeripheralForgeCompat {
    public static void regPer() {
        ForgeComputerCraftAPI.registerPeripheralProvider((level, pos, side) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LaunchPadBlockEntity launchPad) {
                return LazyOptional.of(() -> (IPeripheral) new LaunchPadBlockPeripheral(launchPad, side));
            }
            return LazyOptional.empty();
        });
    }
}
