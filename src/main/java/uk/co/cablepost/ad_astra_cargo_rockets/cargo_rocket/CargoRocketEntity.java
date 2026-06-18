package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

import java.util.List;
import java.util.Objects;

public class CargoRocketEntity extends Entity {
    public String targetPlanet = "";
    // Lua側から明示的に送られた状態（優先表示）。空ならMOD側で自動推測する。
    public String statusOverride = "";

    /** 32 buckets = 32000 mB （ランチパッド側と同じ容量） */
    public static final int FUEL_CAPACITY = 32000;
    public static final int CARGO_FLUID_CAPACITY = 32000;

    // ロケット自身が燃料タンクとカーゴ流体タンクを持つ。以前はランチパッド側にあり、
    // 発射後も出発地に残ったままになる（着陸先で値が消えたように見える）バグの
    // 原因だったため、積荷として完全にロケット側へ移した。
    public final net.minecraftforge.fluids.capability.templates.FluidTank fuelTank =
            new net.minecraftforge.fluids.capability.templates.FluidTank(FUEL_CAPACITY) {
                @Override public boolean isFluidValid(net.minecraftforge.fluids.FluidStack stack) { return true; }
            };
    public final net.minecraftforge.fluids.capability.templates.FluidTank cargoFluidTank =
            new net.minecraftforge.fluids.capability.templates.FluidTank(CARGO_FLUID_CAPACITY) {
                @Override public boolean isFluidValid(net.minecraftforge.fluids.FluidStack stack) { return true; }
            };

    private static final EntityDataAccessor<String> TRACKED_NAME =
            SynchedEntityData.defineId(CargoRocketEntity.class, EntityDataSerializers.STRING);

    private final net.minecraft.core.NonNullList<ItemStack> inventory =
            net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);

    private static final EntityDataAccessor<Integer> TRACKED_TIER =
            SynchedEntityData.defineId(CargoRocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRACKED_LAUNCH_TICKS =
            SynchedEntityData.defineId(CargoRocketEntity.class, EntityDataSerializers.INT);

    private boolean hasPlayedLandingSound = false;
    // grounded状態になった瞬間のゲームタイム(tick)。-1なら未設定(まだ飛行中、または記録前)。
    // 「発射からの固定秒数」ではなく「実際に止まってからの経過時間」で待機判定するためのもの。
    private long groundedSinceTick = -1;
    private String lastFlightState = "";

    public CargoRocketEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        // モデルの実寸はバウンディングボックスより大きいため、カリング用に拡大
        return super.getBoundingBoxForCulling().inflate(2.0, 2.0, 2.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TRACKED_TIER, 0);
        this.entityData.define(TRACKED_LAUNCH_TICKS, 0);
        this.entityData.define(TRACKED_NAME, "");
    }

    public void setRocketName(String name) { entityData.set(TRACKED_NAME, name == null ? "" : name); }
    public String getRocketName() { return entityData.get(TRACKED_NAME); }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        entityData.set(TRACKED_TIER, nbt.getInt("Tier"));
        entityData.set(TRACKED_LAUNCH_TICKS, nbt.getInt("LaunchTicks"));
        targetPlanet = nbt.getString("TargetPlanet");
        entityData.set(TRACKED_NAME, nbt.getString("RocketName"));
        statusOverride = nbt.getString("StatusOverride");
        if (nbt.contains("FuelTank")) fuelTank.readFromNBT(nbt.getCompound("FuelTank"));
        if (nbt.contains("CargoFluidTank")) cargoFluidTank.readFromNBT(nbt.getCompound("CargoFluidTank"));
        ContainerHelper.loadAllItems(nbt, inventory);
        if (nbt.contains("BucketSlotsData")) {
            net.minecraft.core.NonNullList<ItemStack> tmp = net.minecraft.core.NonNullList.withSize(2, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("BucketSlotsData"), tmp);
            for (int i = 0; i < 2; i++) bucketSlots.set(i, tmp.get(i));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("Tier", entityData.get(TRACKED_TIER));
        nbt.putInt("LaunchTicks", entityData.get(TRACKED_LAUNCH_TICKS));
        nbt.putString("TargetPlanet", targetPlanet);
        nbt.putString("RocketName", entityData.get(TRACKED_NAME));
        nbt.putString("StatusOverride", statusOverride);
        CompoundTag fuelTag = new CompoundTag();
        fuelTank.writeToNBT(fuelTag);
        nbt.put("FuelTank", fuelTag);
        CompoundTag cargoTag = new CompoundTag();
        cargoFluidTank.writeToNBT(cargoTag);
        nbt.put("CargoFluidTank", cargoTag);
        ContainerHelper.saveAllItems(nbt, inventory);
        CompoundTag bucketSaveTag = new CompoundTag();
        ContainerHelper.saveAllItems(bucketSaveTag, bucketSlots);
        nbt.put("BucketSlotsData", bucketSaveTag);
    }

    public void setTier(int tier) { entityData.set(TRACKED_TIER, tier); }
    public int getTier() { return entityData.get(TRACKED_TIER); }
    public void setLaunchTicks(int ticks) { entityData.set(TRACKED_LAUNCH_TICKS, ticks); }
    public int getLaunchTicks() { return entityData.get(TRACKED_LAUNCH_TICKS); }

    /** ロケットの物理的な飛行状態（自動判定）。Scanner GUIの表示に使う。 */
    public String getFlightState() {
        if (!targetPlanet.isEmpty()) return "ascending";
        if (getDeltaMovement().y < -0.05) return "descending";
        return "grounded";
    }

    // 固定インスタンス - 毎回生成すると変更が失われる
    private final net.minecraft.world.SimpleContainer inventoryContainer = new net.minecraft.world.SimpleContainer(9) {
        @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
        @Override public void setItem(int slot, ItemStack stack) { inventory.set(slot, stack); setChanged(); }
        @Override public int getContainerSize() { return 9; }
        @Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack split = stack.split(amount);
            if (stack.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            setChanged();
            return split;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack old = inventory.get(slot);
            inventory.set(slot, ItemStack.EMPTY);
            return old;
        }
        @Override public void clearContent() {
            for (int i = 0; i < 9; i++) inventory.set(i, ItemStack.EMPTY);
        }
    };

    public net.minecraft.world.SimpleContainer getInventory() {
        return inventoryContainer;
    }

    // 燃料バケツ・カーゴ流体バケツを置くための2スロット専用コンテナ([0]=燃料, [1]=カーゴ)。
    // ここに満タンのバケツを置くとfuelTank/cargoFluidTankに注がれ空バケツに変わり、
    // 空バケツを置くとタンクから汲み出して満タンバケツに変わる(RocketMenu側で処理)。
    private final net.minecraft.core.NonNullList<ItemStack> bucketSlots =
            net.minecraft.core.NonNullList.withSize(2, ItemStack.EMPTY);

    private final net.minecraft.world.SimpleContainer bucketContainer = new net.minecraft.world.SimpleContainer(2) {
        @Override public ItemStack getItem(int slot) { return bucketSlots.get(slot); }
        @Override public void setItem(int slot, ItemStack stack) { bucketSlots.set(slot, stack); setChanged(); }
        @Override public int getContainerSize() { return 2; }
        @Override public boolean isEmpty() { return bucketSlots.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = bucketSlots.get(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack split = stack.split(amount);
            if (stack.isEmpty()) bucketSlots.set(slot, ItemStack.EMPTY);
            setChanged();
            return split;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack old = bucketSlots.get(slot);
            bucketSlots.set(slot, ItemStack.EMPTY);
            return old;
        }
        @Override public void clearContent() {
            for (int i = 0; i < 2; i++) bucketSlots.set(i, ItemStack.EMPTY);
        }
    };

    public net.minecraft.world.SimpleContainer getBucketSlots() {
        return bucketContainer;
    }

    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean isPushable() { return false; }
    // プレイヤーが殴って攻撃できるようにする
    @Override public boolean isPickable() { return true; }
    @Override public boolean skipAttackInteraction(net.minecraft.world.entity.Entity attacker) { return false; }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!"grounded".equals(getFlightState())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "The rocket must be grounded to open its inventory."), true);
            return InteractionResult.FAIL;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.MenuProvider() {
                        @Override
                        public net.minecraft.network.chat.Component getDisplayName() {
                            String name = getRocketName();
                            return net.minecraft.network.chat.Component.literal(
                                    name != null && !name.isEmpty() ? name : "Cargo Rocket");
                        }
                        @Override
                        public @javax.annotation.Nullable net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                                int syncId, net.minecraft.world.entity.player.Inventory playerInventory, Player p) {
                            return new RocketMenu(syncId, playerInventory, CargoRocketEntity.this);
                        }
                    },
                    buf -> buf.writeVarInt(getId()));
        }
        return InteractionResult.CONSUME;
    }

    private void dropInventory() {
        if (level().isClientSide) return;
        for (int i = 0; i < inventory.size(); i++) {
            spawnAtLocation(inventory.get(i));
            inventory.set(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < bucketSlots.size(); i++) {
            spawnAtLocation(bucketSlots.get(i));
            bucketSlots.set(i, ItemStack.EMPTY);
        }
    }

    private void dropSelf() {
        if (level().isClientSide) return;
        int tier = getTier();
        if (tier == 1) { spawnAtLocation(new ItemStack(AdAstraCargoRockets.CARGO_ROCKET_TIER_1_ITEM.get())); return; }
        if (tier == 2) { spawnAtLocation(new ItemStack(AdAstraCargoRockets.CARGO_ROCKET_TIER_2_ITEM.get())); return; }
        if (tier == 3) { spawnAtLocation(new ItemStack(AdAstraCargoRockets.CARGO_ROCKET_TIER_3_ITEM.get())); return; }
        if (tier == 4) { spawnAtLocation(new ItemStack(AdAstraCargoRockets.CARGO_ROCKET_TIER_4_ITEM.get())); return; }
        spawnAtLocation(new ItemStack(Items.DIRT));
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (isInvulnerableTo(source)) return false;
        dropInventory();
        dropSelf();
        discard();
        return true;
    }

    public void killRocket() {
        if (!level().isClientSide) {
            dropInventory();
            dropSelf();
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        setPos(getX(), getY() + getDeltaMovement().y, getZ());
        if (level().isClientSide) { clientTick(); return; }
        serverTick();
    }

    private void clientTick() {
        double velY = getDeltaMovement().y;
        if (velY < -0.1) {
            boolean groundNearby = false;
            for (int i = 1; i < 30; i++) {
                BlockPos check = blockPosition().below(i);
                if (!level().getBlockState(check).getCollisionShape(level(), check).isEmpty()) {
                    groundNearby = true; break;
                }
            }
            if (groundNearby) {
                spawnFlameParticles();
                if (!hasPlayedLandingSound) { hasPlayedLandingSound = true; playLaunchSound(); }
            } else {
                hasPlayedLandingSound = false;
            }
        } else if (getLaunchTicks() > 0) {
            spawnFlameParticles();
            int ticks = getLaunchTicks();
            if (ticks == 1 || ticks == 40) playLaunchSound();
            hasPlayedLandingSound = false;
        } else {
            hasPlayedLandingSound = false;
        }
    }

    private void spawnFlameParticles() {
        for (int i = 0; i < 3; i++) {
            level().addParticle(ParticleTypes.FLAME, true,
                    getX() + (random.nextDouble() - 0.5), getY(), getZ() + (random.nextDouble() - 0.5), 0, -0.2, 0);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    getX() + (random.nextDouble() - 0.5), getY(), getZ() + (random.nextDouble() - 0.5), 0, -0.2, 0);
        }
    }

    private void playLaunchSound() {
        // playSound(Player, x, y, z, ...) の正しいシグネチャ: null = 誰も除外しない
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("ad_astra", "launch"));
        if (sound != null) {
            level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.AMBIENT, 2f, 0.5f);
        } else {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 2f, 0.5f);
        }
    }

    private void serverTick() {
        // 状態がgroundedに変わった瞬間(=実際に止まった瞬間)を記録する。
        // 「発射からの固定秒数」ではなく、ここからの経過時間で待機判定できるようにする。
        String currentState = getFlightState();
        if ("grounded".equals(currentState) && !"grounded".equals(lastFlightState)) {
            groundedSinceTick = level().getGameTime();
        } else if (!"grounded".equals(currentState)) {
            groundedSinceTick = -1;
        }
        lastFlightState = currentState;

        // 衝突判定はascentTick/descentTick内でより精密に行う。
        // ここで広範囲の判定をすると隣接ランチパッドの駐機ロケットを誤爆する原因になるため削除。
        if (targetPlanet.isEmpty()) { descentTick(); } else { ascentTick(); }
    }

    /** 止まってからの経過秒数。まだ止まっていない(飛行中)場合は0。 */
    public int getSecondsSinceGrounded() {
        if (groundedSinceTick < 0 || level() == null) return 0;
        long elapsedTicks = level().getGameTime() - groundedSinceTick;
        return (int) Math.max(0, elapsedTicks / 20);
    }

    private void descentTick() {
        setLaunchTicks(0);
        // 自分の直下、同じXZ位置の半径1マス以内に「地上で停止している」別のロケットが
        // いる場合のみ衝突とみなす。以前はbelow(3)±2マスという広い範囲で判定していたため、
        // 月など複数のロケットが行き来する場所では、駐機中のロケットの上空を通過するだけで
        // 誤って爆発・破壊されてしまっていた（「月に着くとロケットが破壊される」バグの本体）。
        List<CargoRocketEntity> below = level().getEntitiesOfClass(
                CargoRocketEntity.class, new AABB(blockPosition().below(2)).inflate(1, 1, 1),
                e -> e.isAlive() && e.getId() != getId() && "grounded".equals(e.getFlightState()));
        if (!below.isEmpty()) {
            level().explode(this, getX(), getY() - 0.5, getZ(), 5, Level.ExplosionInteraction.MOB);
            dropInventory(); dropSelf(); kill(); return;
        }

        Integer highestBlockY = null;
        int cy = blockPosition().getY();
        outer:
        for (int y = cy; y > cy - 30; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos check = new BlockPos(blockPosition().getX() + x, y, blockPosition().getZ() + z);
                    if (!level().getBlockState(check).getCollisionShape(level(), check).isEmpty()) {
                        highestBlockY = y; break outer;
                    }
                }
            }
        }

        if (highestBlockY != null) {
            double target = highestBlockY + 1.0;
            double dist = getY() - target;
            if (dist <= 0.1) { setDeltaMovement(0, 0, 0); setPos(getX(), target, getZ()); }
            else { setDeltaMovement(0, -Math.min(1.0, Math.max(0.1, dist * 0.1)), 0); }
        } else {
            setDeltaMovement(0, -1.0, 0);
        }
    }

    private void ascentTick() {
        int ticks = getLaunchTicks();
        setLaunchTicks(ticks + 1);

        List<CargoRocketEntity> above = level().getEntitiesOfClass(
                CargoRocketEntity.class, new AABB(blockPosition().above(4)).inflate(2),
                e -> e.isAlive() && e.getId() != getId());
        if (!above.isEmpty()) {
            level().explode(this, getX(), getY() - 4, getZ(), 5, Level.ExplosionInteraction.MOB);
            dropInventory(); dropSelf(); kill(); return;
        }

        boolean clear = true;
        for (int x = -1; x <= 1 && clear; x++)
            for (int z = -1; z <= 1 && clear; z++)
                if (!level().getBlockState(blockPosition().offset(x, 4, z)).isAir()) clear = false;

        if (clear) {
            if (ticks < 40) setDeltaMovement((random.nextDouble() - 0.5) * 0.05, 0, (random.nextDouble() - 0.5) * 0.05);
            else setDeltaMovement(0, Math.min(1.0, (ticks - 40) * 0.01), 0);
        } else {
            setDeltaMovement(0, 0, 0);
        }

        if (getY() > level().getMaxBuildHeight() + 400) dimensionTransfer();
    }

    private void dimensionTransfer() {
        ServerLevel targetWorld = null;
        for (var world : Objects.requireNonNull(level().getServer()).getAllLevels()) {
            if (world.dimension().location().toString().equals(targetPlanet)) { targetWorld = world; break; }
        }
        targetPlanet = "";
        statusOverride = ""; // 到着後は古い状態表示を持ち越さない
        if (targetWorld == null || targetWorld.equals(level())) return;
        Entity spawned = getType().create(targetWorld);
        if (spawned != null) {
            spawned.restoreFrom(this);
            spawned.moveTo(getX(), targetWorld.getMaxBuildHeight() + 200, getZ(), 0, 0);
            spawned.setDeltaMovement(Vec3.ZERO);
            targetWorld.addFreshEntity(spawned);
        }
        discard();
    }
}
