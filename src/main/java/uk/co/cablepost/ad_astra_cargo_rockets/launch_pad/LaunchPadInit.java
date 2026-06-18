package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class LaunchPadInit {

    private RegistryObject<LaunchPadBlock> block;
    private RegistryObject<LaunchPadDummyBlock> dummyBlock;
    private RegistryObject<BlockEntityType<LaunchPadBlockEntity>> blockEntityType;
    private RegistryObject<BlockEntityType<LaunchPadDummyBlockEntity>> dummyBlockEntityType;
    private RegistryObject<MenuType<LaunchPadMenu>> menuType;

    public void register(IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, AdAstraCargoRockets.MOD_ID);

        block = blocks.register("launch_pad", () -> new LaunchPadBlock(
                BlockBehaviour.Properties.of()
                        .strength(2.0f, 6.0f)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()));

        dummyBlock = blocks.register("launch_pad_dummy", () -> new LaunchPadDummyBlock(
                BlockBehaviour.Properties.of().strength(3.5f).noOcclusion().noLootTable()));

        blocks.register(bus);

        AdAstraCargoRockets.ITEMS.register("launch_pad",
                () -> new BlockItem(block.get(), new Item.Properties()));

        blockEntityType = AdAstraCargoRockets.BLOCK_ENTITY_TYPES.register("launch_pad",
                () -> BlockEntityType.Builder.of(LaunchPadBlockEntity::new, block.get()).build(null));

        dummyBlockEntityType = AdAstraCargoRockets.BLOCK_ENTITY_TYPES.register("launch_pad_dummy",
                () -> BlockEntityType.Builder.of(LaunchPadDummyBlockEntity::new, dummyBlock.get()).build(null));

        DeferredRegister<MenuType<?>> menus = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AdAstraCargoRockets.MOD_ID);
        menuType = menus.register("launch_pad", () -> IForgeMenuType.create((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            LaunchPadBlockEntity be = null;
            if (Minecraft.getInstance().level != null) {
                BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(pos);
                if (blockEntity instanceof LaunchPadBlockEntity lbe) be = lbe;
            }
            return new LaunchPadMenu(syncId, inv, be);
        }));
        menus.register(bus);

        bus.addListener(this::onRegisterRenderers);
    }

    public RegistryObject<LaunchPadBlock> getBlock() { return block; }
    public RegistryObject<LaunchPadDummyBlock> getDummyBlock() { return dummyBlock; }
    public RegistryObject<BlockEntityType<LaunchPadBlockEntity>> getBlockEntity() { return blockEntityType; }
    public RegistryObject<BlockEntityType<LaunchPadDummyBlockEntity>> getDummyBlockEntity() { return dummyBlockEntityType; }
    public RegistryObject<MenuType<LaunchPadMenu>> getMenuType() { return menuType; }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(blockEntityType.get(), LaunchPadBlockEntityRenderer::new);
    }

    @OnlyIn(Dist.CLIENT)
    public void clientSetup() {
        MenuScreens.register(menuType.get(), LaunchPadScreen::new);
    }
}
