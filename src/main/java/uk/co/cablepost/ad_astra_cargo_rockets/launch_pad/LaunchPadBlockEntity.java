package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.AbstractMachineBlockEntity;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;
import uk.co.cablepost.ad_astra_cargo_rockets.ModConfig;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketEntity;

import java.util.*;

/**
 * ランチパッドのブロックエンティティ。
 *
 * 燃料・カーゴ流体・アイテムの実体は常にロケット自身（CargoRocketEntityのfuelTank /
 * cargoFluidTank / getInventory()）が保持する唯一のデータであり、ランチパッドはそれ自体に
 * 中身を持たない。ランチパッドのパイプ接続面・ホッパースロットは、地上に着地している
 * （= getRocket()で見つかる）ロケットのタンク/インベントリへの「窓口」として機能し、
 * getCapability()経由でロケット側のハンドラへそのまま委譲する。ロケットがいない、または
 * 飛行中はその窓口は何も受け付けない（渡し先がないため）。
 */
public class LaunchPadBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {

    public static final TagKey<Item> DENIED_ITEMS = ItemTags.create(
            new ResourceLocation(AdAstraCargoRockets.MOD_ID, "denied_in_launch_pad"));

    private final Set<IComputerAccess> computers = new HashSet<>();

    // このランチパッドから最後に発射したロケットのID。飛行中はgetRocket()の半径2マス
    // 探索に引っかからなくなるため、運搬中のカーゴ流体などを引き続きLuaから問い合わせ
    // できるように記録しておく。次にこのランチパッドへ別のロケットが着陸/設置されたら、
    // getRocket()経由でそちらが優先される。
    private int lastLaunchedRocketId = -1;

    // 容量0の空ハンドラ。ロケットがいない/飛行中に、パイプやホッパーが何も受け付けない
    // 状態を表現するために使う（渡し先が無いので何も入らない・何も出てこない）。
    private static final IItemHandler EMPTY_ITEM_HANDLER = new net.minecraftforge.items.ItemStackHandler(0);
    private static final IFluidHandler EMPTY_FLUID_HANDLER = new net.minecraftforge.fluids.capability.templates.FluidTank(0);

    // パイプ/ホッパー側がLazyOptionalを保持し続けるケースに対応するため、委譲先の
    // ロケットが変わった（着地/離陸/別のロケットに切り替わった）タイミングだけ
    // invalidateしてgetCapability経由の再取得を促す。毎tick作り直すのではなく、
    // 変化があった時だけ無効化することで余計なLazyOptional生成を避ける。
    private int cachedDelegateRocketId = -1;
    private LazyOptional<IItemHandler> itemHandlerDelegate;
    private LazyOptional<IFluidHandler> fuelHandlerDelegate;
    private LazyOptional<IFluidHandler> cargoHandlerDelegate;

    public LaunchPadBlockEntity(BlockPos pos, BlockState state) {
        super(
            AdAstraCargoRockets.LAUNCH_PAD.getBlockEntity().get(),
            pos, state,
            new int[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8 },
            new int[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8 },
            1000000, 100000, 0, false
        );
        itemHandlerDelegate = LazyOptional.of(this::getDelegatedItemHandler);
        fuelHandlerDelegate = LazyOptional.of(() -> getDelegatedFluidHandler(Direction.DOWN));
        cargoHandlerDelegate = LazyOptional.of(() -> getDelegatedFluidHandler(Direction.UP));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchPadBlockEntity be) {
        // リアルタイムGUI同期のためにsetChangedを毎tick呼ぶ
        if (!level.isClientSide) {
            be.setChanged();
            be.refreshDelegateIfRocketChanged();
        }
    }

    /**
     * 委譲先のロケットが変わった（いなくなった/新しく着地した/別のロケットに替わった）
     * 場合だけCapabilityを無効化する。パイプ側が古いLazyOptionalを掴んだままでも、
     * 無効化をきっかけに再度getCapability()を呼び直してくれる。
     */
    private void refreshDelegateIfRocketChanged() {
        CargoRocketEntity rocket = getRocket();
        int currentId = rocket != null ? rocket.getId() : -1;
        if (currentId != cachedDelegateRocketId) {
            cachedDelegateRocketId = currentId;
            itemHandlerDelegate.invalidate();
            fuelHandlerDelegate.invalidate();
            cargoHandlerDelegate.invalidate();
            itemHandlerDelegate = LazyOptional.of(this::getDelegatedItemHandler);
            fuelHandlerDelegate = LazyOptional.of(() -> getDelegatedFluidHandler(Direction.DOWN));
            cargoHandlerDelegate = LazyOptional.of(() -> getDelegatedFluidHandler(Direction.UP));
            // 委譲先のロケットが変わった = パイプ/ホッパー/コンパレータから見える内容量が
            // 変化したタイミングなので、周囲ブロックに更新を通知する。
            if (level != null) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
        }
    }

    @Override public int getMaxProcessProgress() { return 0; }
    @Override public int processEnergyConsumption() { return 0; }

    @Override @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * レンダリング範囲をブロック自体より広げる。モデルの実寸が3x3マルチブロックの
     * 中心ブロックの当たり判定より大きいため、デフォルトのバウンディングボックスでは
     * 中心ブロックを直接見ていない角度・距離からだとフラスタムカリングで描画が消えてしまう。
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.0, 2.0, 2.0);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ad_astra_cargo_rockets.launch_pad");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new LaunchPadMenu(syncId, playerInventory, this);
    }

    public void addComputer(IComputerAccess computer) { computers.add(computer); }
    public void removeComputer(IComputerAccess computer) { computers.remove(computer); }

    public @Nullable CargoRocketEntity getRocket() {
        if (level == null) return null;
        List<CargoRocketEntity> nearby = level.getEntitiesOfClass(
                CargoRocketEntity.class, new AABB(worldPosition).inflate(2), CargoRocketEntity::isAlive);
        nearby = nearby.stream()
                .filter(x -> x.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0, 0.5, 0)) < 2f)
                .toList();
        return nearby.size() == 1 ? nearby.get(0) : null;
    }

    /**
     * getRocket()と同様にこのランチパッドのロケットを返すが、見つからない場合は
     * 直前にこのランチパッドから発射されたロケットを（飛行中でディメンションが
     * 違っても）level.getEntity()で探してフォールバックする。
     * 飛行中でも setRocketStatus / getCarriedCargoFluid 等が機能するようにするため。
     */
    public @Nullable CargoRocketEntity getRocketIncludingInFlight() {
        @Nullable CargoRocketEntity rocket = getRocket();
        if (rocket != null) return rocket;
        if (lastLaunchedRocketId == -1 || level == null || level.getServer() == null) return null;
        for (var serverLevel : level.getServer().getAllLevels()) {
            var entity = serverLevel.getEntity(lastLaunchedRocketId);
            if (entity instanceof CargoRocketEntity r && r.isAlive()) return r;
        }
        // 既に存在しない（破壊された等）場合は追跡をやめる
        lastLaunchedRocketId = -1;
        return null;
    }

    /**
     * パイプ/ホッパーが繋がる先のIItemHandler。地上に着地しているロケットがいれば
     * その9スロットインベントリへ直接委譲する（ランチパッド自体は中身を持たない）。
     * ロケットがいない場合は何も受け付けない空ハンドラを返す。
     */
    public IItemHandler getDelegatedItemHandler() {
        CargoRocketEntity rocket = getRocket();
        if (rocket == null) return EMPTY_ITEM_HANDLER;
        return new InvWrapper(rocket.getInventory());
    }

    /**
     * 指定した面に繋がる先のIFluidHandler。下面は燃料タンク、それ以外はカーゴ流体タンクへ
     * 委譲する（AbstractFluidMachineBlockEntityだった頃と同じ面割り当て）。ロケットがいない
     * 場合は何も受け付けない空ハンドラを返す。
     */
    public IFluidHandler getDelegatedFluidHandler(@Nullable Direction side) {
        CargoRocketEntity rocket = getRocket();
        if (rocket == null) return EMPTY_FLUID_HANDLER;
        return side == Direction.DOWN ? rocket.fuelTank : rocket.cargoFluidTank;
    }

    @Override
    public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerDelegate.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return (side == Direction.DOWN ? fuelHandlerDelegate : cargoHandlerDelegate).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerDelegate.invalidate();
        fuelHandlerDelegate.invalidate();
        cargoHandlerDelegate.invalidate();
    }

    public int calculateDifficulty(String planet) {
        Map<String, Integer> valid = getValidDestinations();
        String cur = level.dimension().location().toString();
        return Math.max(1, Math.abs(valid.getOrDefault(planet, 1) - valid.getOrDefault(cur, 1)));
    }

    public @Nullable LaunchFailReason launch(String planet) {
        @Nullable CargoRocketEntity rocket = getRocket();
        if (rocket == null) return LaunchFailReason.NO_ROCKET;
        Map<String, Integer> valid = getValidDestinations();
        if (!valid.containsKey(planet)) return LaunchFailReason.INVALID_PLANET;
        int difficulty = calculateDifficulty(planet);
        if (difficulty > rocket.getTier()) return LaunchFailReason.ROCKET_TIER_TOO_LOW;
        if ((long) getEnergyRequiredForLaunch() * difficulty > _energyStorage.getEnergyStored())
            return LaunchFailReason.NOT_ENOUGH_ENERGY;
        // 燃料はロケット自身のタンクから消費する。ランチパッドのBOTTOM面パイプから
        // 注がれた燃料も、着地中はrocket.fuelTankへ直接委譲されているのでここに反映済み。
        FluidStack fuelFluid = rocket.fuelTank.getFluid();
        if (fuelFluid.isEmpty()) return LaunchFailReason.NOT_ENOUGH_FUEL;
        String fluidId = ForgeRegistries.FLUIDS.getKey(fuelFluid.getFluid()).toString();
        double perf = ModConfig.INSTANCE.fuels.getOrDefault(fluidId, 1.0);
        if (perf <= 0) perf = 1.0;
        int actualFuel = (int) ((getFuelRequiredForLaunch() * difficulty) / perf);
        if (fuelFluid.getAmount() < actualFuel) return LaunchFailReason.NOT_ENOUGH_FUEL;
        FluidStack drained = rocket.fuelTank.drain(actualFuel,
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() == actualFuel) {
            _energyStorage.extractEnergy(getEnergyRequiredForLaunch() * difficulty, false);
            // カーゴ流体・アイテムは既にロケット自身のタンク/インベントリにあるので、
            // 積み替え処理は不要。パイプ/ホッパーで注がれた分がそのまま運ばれる。
            rocket.targetPlanet = planet;
            lastLaunchedRocketId = rocket.getId();
            setChanged();
            return null;
        }
        return LaunchFailReason.NOT_ENOUGH_FUEL;
    }

    public int getEnergyRequiredForLaunch() { return 5000; }
    public int getFuelRequiredForLaunch() { return 3000; }
    public int getEnergy() { return _energyStorage.getEnergyStored(); }
    public int getMaxEnergy() { return _energyStorage.getMaxEnergyStored(); }

    public Map<String, Integer> getValidDestinations() {
        if (level == null || level.getServer() == null) return new HashMap<>();
        Map<String, Integer> result = new HashMap<>();
        for (var world : level.getServer().getAllLevels()) {
            String id = world.dimension().location().toString();
            if (ModConfig.INSTANCE.validDestinations.containsKey(id))
                result.put(id, ModConfig.INSTANCE.validDestinations.get(id));
        }
        return result;
    }

    public void destroyRocket() {
        CargoRocketEntity r = getRocket();
        if (r != null) r.killRocket();
    }

    /**
     * ロケットが地上で待機している理由を、ランチパッドの現在値から自動推測する。
     * Lua側がCargoRocketEntity.statusOverrideを明示的にセットしている場合はそちらが優先される
     * （Scanner GUI側で判定する）。
     */
    public String inferWaitReason() {
        CargoRocketEntity rocket = getRocket();
        if (rocket == null) return "no_rocket";
        if (!"grounded".equals(rocket.getFlightState())) return "in_flight";

        // 想定される最低難易度（Tier1相当）でエネルギー・燃料の不足を粗く判定する。
        // 実際の目的地はLuaスクリプト側しか知らないため、これは目安の判定。
        int approxDifficulty = 1;
        if (_energyStorage.getEnergyStored() < (long) getEnergyRequiredForLaunch() * approxDifficulty) {
            return "not_enough_energy";
        }
        FluidStack fluid = rocket.fuelTank.getFluid();
        double perf = 1.0;
        if (!fluid.isEmpty()) {
            String fluidId = ForgeRegistries.FLUIDS.getKey(fluid.getFluid()).toString();
            perf = ModConfig.INSTANCE.fuels.getOrDefault(fluidId, 1.0);
            if (perf <= 0) perf = 1.0;
        }
        int approxFuelNeeded = (int) ((getFuelRequiredForLaunch() * approxDifficulty) / perf);
        if (rocket.fuelTank.getFluidAmount() < approxFuelNeeded) {
            return "not_enough_fuel";
        }
        return "idle";
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !stack.is(DENIED_ITEMS);
    }

    // --- Container委譲 ---
    // ホッパー等はIItemHandlerのCapability経由だけでなく、BlockEntityをそのままContainerと
    // して扱う経路でもアクセスしてくる可能性があるため、こちらも着地中のロケットの
    // インベントリへ委譲する。ランチパッド自身は常に「空」（中身を持たない）。

    @Override
    public int getContainerSize() {
        CargoRocketEntity rocket = getRocket();
        return rocket != null ? rocket.getInventory().getContainerSize() : 0;
    }

    @Override
    public boolean isEmpty() {
        CargoRocketEntity rocket = getRocket();
        return rocket == null || rocket.getInventory().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        CargoRocketEntity rocket = getRocket();
        return rocket != null ? rocket.getInventory().getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        CargoRocketEntity rocket = getRocket();
        return rocket != null ? rocket.getInventory().removeItem(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        CargoRocketEntity rocket = getRocket();
        return rocket != null ? rocket.getInventory().removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        CargoRocketEntity rocket = getRocket();
        if (rocket != null) rocket.getInventory().setItem(slot, stack);
    }

    @Override
    public void clearContent() {
        CargoRocketEntity rocket = getRocket();
        if (rocket != null) rocket.getInventory().clearContent();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
