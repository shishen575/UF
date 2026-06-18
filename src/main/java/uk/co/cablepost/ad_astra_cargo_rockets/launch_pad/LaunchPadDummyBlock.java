package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nullable;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class LaunchPadDummyBlock extends Block implements EntityBlock {

    public LaunchPadDummyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** 周囲1マスからランチパッド中心を探す */
    @Nullable
    public static BlockPos findCenterPos(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos candidate = pos.offset(dx, 0, dz);
                if (level.getBlockState(candidate).getBlock() instanceof LaunchPadBlock) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.block(); }
    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.block(); }
    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }
    @Override
    public int getLightBlock(BlockState s, BlockGetter l, BlockPos p) { return 0; }
    @Override
    public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1.0f; }
    @Override
    public boolean useShapeForLightOcclusion(BlockState s) { return false; }
    @Override
    public boolean canBeReplaced(BlockState s, net.minecraft.world.item.context.BlockPlaceContext c) { return false; }

    // 右クリックを中心ブロックに転送（GUI・ロケット設置など）
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos center = findCenterPos(level, pos);
        if (center != null) {
            BlockState centerState = level.getBlockState(center);
            return centerState.use(level, player, hand,
                    new BlockHitResult(hit.getLocation(), hit.getDirection(), center, hit.isInside()));
        }
        return InteractionResult.PASS;
    }

    // 破壊時に中心ブロックも除去
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockPos center = findCenterPos(level, pos);
            if (center != null && level.getBlockState(center).getBlock() instanceof LaunchPadBlock) {
                level.destroyBlock(center, true);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    // BlockEntityを持つことでCapabilityを中心BEに転送できる
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchPadDummyBlockEntity(pos, state);
    }
}
