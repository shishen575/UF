package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketEntity;

import java.util.Map;

/**
 * ランチパッドのCC:Tweaked API。
 *
 * v1.2.4でアイテム/燃料/カーゴ流体の実体はロケット自身が持つようになったため、
 * ランチパッド↔ロケット間で「移動」させるAPI(loadAllItems/unloadAllItems/
 * moveItemsFrom...等)は廃止した。ランチパッドのパイプ・ホッパー接続自体は
 * そのまま残っており、着地中のロケットのタンク/インベントリへ直結する
 * （LaunchPadBlockEntity.getCapability参照）。getFuel()/getCargoFluid()等は
 * 常にロケット自身のタンクの値を返す。
 */
public class LaunchPadBlockPeripheral implements IPeripheral {

    private final LaunchPadBlockEntity blockEntity;

    public LaunchPadBlockPeripheral(BlockEntity blockEntity, @Nullable Direction direction) {
        this.blockEntity = (LaunchPadBlockEntity) blockEntity;
    }

    @Override public void attach(IComputerAccess computer) { blockEntity.addComputer(computer); }
    @Override public void detach(IComputerAccess computer) { blockEntity.removeComputer(computer); }
    @Override public String getType() { return "cargo_rocket_launch_pad"; }
    @Override public boolean equals(IPeripheral other) {
        return other instanceof LaunchPadBlockPeripheral lbp && lbp.blockEntity == blockEntity;
    }

    @LuaFunction(mainThread = true)
    public final void launch(String planet) throws LuaException {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocket();
        @Nullable LaunchFailReason reason = blockEntity.launch(planet);
        if (reason == null) {
            if (rocket != null) {
                var sound = ForgeRegistries.SOUND_EVENTS.getValue(
                        new net.minecraft.resources.ResourceLocation("ad_astra", "launch"));
                if (sound != null)
                    rocket.level().playSound(null, rocket.getX(), rocket.getY(), rocket.getZ(),
                            sound, SoundSource.NEUTRAL, 1f, 1f);
                else
                    rocket.level().playSound(null, rocket.getX(), rocket.getY(), rocket.getZ(),
                            SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1f, 1f);
            }
            return;
        }
        switch (reason) {
            case NO_ROCKET -> throw new LuaException("No rocket found");
            case INVALID_PLANET -> throw new LuaException(planet + " is not a valid planet");
            case NOT_ENOUGH_ENERGY -> throw new LuaException("Not enough energy to launch");
            case NOT_ENOUGH_FUEL -> throw new LuaException("Not enough fuel to launch.");
            case ROCKET_TIER_TOO_LOW -> throw new LuaException(
                    planet + " requires a Tier " + blockEntity.calculateDifficulty(planet) + " rocket");
        }
    }

    @LuaFunction(mainThread = true)
    public final int getEnergyRequiredForLaunch() { return blockEntity.getEnergyRequiredForLaunch(); }

    @LuaFunction(mainThread = true)
    public final int getFuelRequiredForLaunch() { return blockEntity.getFuelRequiredForLaunch() / 1000; }

    @LuaFunction(mainThread = true)
    public final long getEnergy() { return blockEntity.getEnergy(); }

    @LuaFunction(mainThread = true)
    public final long getMaxEnergy() { return blockEntity.getMaxEnergy(); }

    @LuaFunction(mainThread = true)
    public final Map<String, Integer> getValidDestinations() { return blockEntity.getValidDestinations(); }

    @LuaFunction(mainThread = true)
    public final int getSecondsSinceLanded() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocket();
        return rocket == null ? 0 : rocket.getSecondsSinceGrounded();
    }

    @LuaFunction(mainThread = true)
    public final String getTargetPlanet() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        if (rocket == null || rocket.targetPlanet.isEmpty()) return "none";
        return rocket.targetPlanet;
    }

    @LuaFunction(mainThread = true)
    public boolean isRocketPresent() { return blockEntity.getRocket() != null; }

    @LuaFunction(mainThread = true)
    public final boolean isRocketInventoryEmpty() throws LuaException {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocket();
        if (rocket == null) throw new LuaException("No rocket found");
        for (int i = 0; i < rocket.getInventory().getContainerSize(); i++) {
            if (!rocket.getInventory().getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @LuaFunction(mainThread = true)
    public void destroyRocket() { blockEntity.destroyRocket(); }

    @LuaFunction(mainThread = true)
    public final void setRocketStatus(String status) throws LuaException {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        if (rocket == null) throw new LuaException("No rocket found");
        rocket.statusOverride = status == null ? "" : status;
    }

    @LuaFunction(mainThread = true)
    public final void setRocketName(String name) throws LuaException {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        if (rocket == null) throw new LuaException("No rocket found");
        rocket.setRocketName(name);
    }

    // 燃料・カーゴ流体はv1.2.4でロケット自身のタンクに移管された。
    // ロケットがランチパッドに無い(飛行中)場合でも、直前に発射したロケットを
    // 追跡して値を返す(getRocketIncludingInFlight)。

    @LuaFunction(mainThread = true)
    public final int getFuel() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        return rocket == null ? 0 : rocket.fuelTank.getFluidAmount();
    }

    @LuaFunction(mainThread = true)
    public final int getMaxFuel() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        return rocket == null ? CargoRocketEntity.FUEL_CAPACITY : rocket.fuelTank.getCapacity();
    }

    @LuaFunction(mainThread = true)
    public final String getFuelType() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        if (rocket == null) return "empty";
        var fluid = rocket.fuelTank.getFluid();
        if (fluid.isEmpty()) return "empty";
        var key = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return key != null ? key.toString() : "empty";
    }

    @LuaFunction(mainThread = true)
    public final int getCargoFluid() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        return rocket == null ? 0 : rocket.cargoFluidTank.getFluidAmount();
    }

    @LuaFunction(mainThread = true)
    public final int getMaxCargoFluid() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        return rocket == null ? CargoRocketEntity.CARGO_FLUID_CAPACITY : rocket.cargoFluidTank.getCapacity();
    }

    @LuaFunction(mainThread = true)
    public final String getCargoFluidType() {
        @Nullable CargoRocketEntity rocket = blockEntity.getRocketIncludingInFlight();
        if (rocket == null) return "empty";
        var fluid = rocket.cargoFluidTank.getFluid();
        if (fluid.isEmpty()) return "empty";
        var key = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return key != null ? key.toString() : "empty";
    }
}
