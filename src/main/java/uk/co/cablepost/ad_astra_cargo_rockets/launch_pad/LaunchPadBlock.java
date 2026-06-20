package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;
import uk.co.cablepost.ad_astra_cargo_rockets.CargoRocketItem;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketEntity;

import java.util.List;

public class LaunchPadBlock extends BaseEntityBlock {

    public LaunchPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchPadBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, AdAstraCargoRockets.LAUNCH_PAD.getBlockEntity().get(),
                LaunchPadBlockEntity::tick);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        net.minecraft.world.level.Level level = ctx.getLevel();
        // 3×3スペースチェック
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos neighbor = pos.offset(dx, 0, dz);
                BlockState neighborState = level.getBlockState(neighbor);
                if (!neighborState.isAir() && !neighborState.canBeReplaced()
                        && !(neighborState.getBlock() instanceof LaunchPadDummyBlock)) {
                    // 設置不可 - プレイヤーに通知
                    if (!level.isClientSide) {
                        ctx.getPlayer().sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("[Launch Pad] Needs 3x3 clear space!"));
                    }
                    return null;
                }
            }
        }
        return super.getStateForPlacement(ctx);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        // 周囲8ブロックにソリッドブロックがないかチェック
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos neighbor = pos.offset(dx, 0, dz);
                BlockState neighborState = level.getBlockState(neighbor);
                if (!neighborState.isAir() && !neighborState.canBeReplaced()
                        && !(neighborState.getBlock() instanceof LaunchPadDummyBlock)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level.isClientSide) return;
        placeDummies(level, pos);
        // ダミーブロック設置で中心ブロックの周囲が変化するため、コンパレータ・パイプ等が
        // すぐに追従できるよう明示的に近傍ブロックへ更新を通知する。
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            removeDummies(level, pos);
            // 破壊によって周囲のダミーブロックが無くなるため、近傍ブロックへ更新を通知する。
            level.updateNeighborsAt(pos, state.getBlock());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    private void placeDummies(Level level, BlockPos center) {
        LaunchPadDummyBlock dummy = AdAstraCargoRockets.LAUNCH_PAD.getDummyBlock().get();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos dPos = center.offset(dx, 0, dz);
                BlockState existing = level.getBlockState(dPos);
                // 空気・草・花などの置き換え可能なブロックのみ上書き
                if (!existing.is(dummy) && (existing.isAir() || existing.canBeReplaced())) {
                    level.setBlockAndUpdate(dPos, dummy.defaultBlockState());
                }
            }
        }
    }

    private void removeDummies(Level level, BlockPos center) {
        LaunchPadDummyBlock dummy = AdAstraCargoRockets.LAUNCH_PAD.getDummyBlock().get();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos dPos = center.offset(dx, 0, dz);
                if (level.getBlockState(dPos).is(dummy)) {
                    level.removeBlock(dPos, false);
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);

        // バケツ右クリックで液体をタンクに入れる
        // 下面クリック = 燃料タンク、それ以外 = 貨物タンク
        if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BucketItem) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof LaunchPadBlockEntity launchPad) {
                    net.minecraft.core.Direction face = hit.getDirection();
                    launchPad.getCapability(
                        net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
                        face).ifPresent(tankHandler -> {
                        net.minecraftforge.fluids.FluidActionResult fluidResult =
                            net.minecraftforge.fluids.FluidUtil.tryEmptyContainer(stack, tankHandler, Integer.MAX_VALUE, player, true);
                        if (fluidResult.isSuccess()) {
                            player.setItemInHand(hand, fluidResult.getResult());
                        }
                    });
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!stack.isEmpty() && stack.getItem() instanceof CargoRocketItem cargoRocketItem) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            List<CargoRocketEntity> nearby = level.getEntitiesOfClass(
                    CargoRocketEntity.class, new AABB(pos).inflate(1), CargoRocketEntity::isAlive);
            if (nearby.isEmpty()) {
                CargoRocketEntity entity = AdAstraCargoRockets.CARGO_ROCKET_ENTITY.get().create(level);
                if (entity != null) {
                    entity.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0f, 0f);
                    entity.setTier(cargoRocketItem.tier);
                    level.addFreshEntity(entity);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
            }
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LaunchPadBlockEntity launchPad) {
                NetworkHooks.openScreen((ServerPlayer) player, launchPad, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
