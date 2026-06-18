package uk.co.cablepost.ad_astra_cargo_rockets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketEntity;
import uk.co.cablepost.ad_astra_cargo_rockets.launch_pad.LaunchPadInit;

@Mod(AdAstraCargoRockets.MOD_ID)
public class AdAstraCargoRockets {
    public static final String MOD_ID = "ad_astra_cargo_rockets";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<CargoRocketItem> CARGO_ROCKET_TIER_1_ITEM =
            ITEMS.register("cargo_rocket_tier_1", () -> new CargoRocketItem(new Item.Properties().stacksTo(1), 1));
    public static final RegistryObject<CargoRocketItem> CARGO_ROCKET_TIER_2_ITEM =
            ITEMS.register("cargo_rocket_tier_2", () -> new CargoRocketItem(new Item.Properties().stacksTo(1), 2));
    public static final RegistryObject<CargoRocketItem> CARGO_ROCKET_TIER_3_ITEM =
            ITEMS.register("cargo_rocket_tier_3", () -> new CargoRocketItem(new Item.Properties().stacksTo(1), 3));
    public static final RegistryObject<CargoRocketItem> CARGO_ROCKET_TIER_4_ITEM =
            ITEMS.register("cargo_rocket_tier_4", () -> new CargoRocketItem(new Item.Properties().stacksTo(1), 4));

    public static final RegistryObject<uk.co.cablepost.ad_astra_cargo_rockets.scanner.RocketScannerItem> ROCKET_SCANNER_ITEM =
            ITEMS.register("rocket_scanner", () ->
                new uk.co.cablepost.ad_astra_cargo_rockets.scanner.RocketScannerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<EntityType<CargoRocketEntity>> CARGO_ROCKET_ENTITY =
            ENTITY_TYPES.register("cargo_rocket", () ->
                EntityType.Builder.<CargoRocketEntity>of(CargoRocketEntity::new, MobCategory.MISC)
                    .sized(1.5f, 5.5f)
                    .build(MOD_ID + ":cargo_rocket"));

    public static final LaunchPadInit LAUNCH_PAD = new LaunchPadInit();
    public static final uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketInit CARGO_ROCKET_MENU =
            new uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketInit();

    public static final RegistryObject<CreativeModeTab> ITEM_GROUP =
            CREATIVE_TABS.register("main_creative_inventory_tab", () ->
                CreativeModeTab.builder()
                    .icon(() -> new ItemStack(LAUNCH_PAD.getBlock().get()))
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.ad_astra_cargo_rockets.items"))
                    .build());

    public AdAstraCargoRockets() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(bus);
        ENTITY_TYPES.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        CREATIVE_TABS.register(bus);
        LAUNCH_PAD.register(bus);
        CARGO_ROCKET_MENU.register(bus);

        bus.addListener(this::setup);
        bus.addListener(this::buildCreativeTab);
        bus.addListener(AdAstraCargoRocketsClient::clientSetup);

        ModConfig.load();
        uk.co.cablepost.ad_astra_cargo_rockets.scanner.ScannerNetwork.register();
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class.forName("dan200.computercraft.api.peripheral.IPeripheral");
                uk.co.cablepost.ad_astra_cargo_rockets.launch_pad.LaunchPadPeripheralForgeCompat.regPer();
            } catch (ClassNotFoundException ignored) {}
        });
    }

    private void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ITEM_GROUP.getKey()) {
            event.accept(LAUNCH_PAD.getBlock().get());
            event.accept(CARGO_ROCKET_TIER_1_ITEM.get());
            event.accept(CARGO_ROCKET_TIER_2_ITEM.get());
            event.accept(CARGO_ROCKET_TIER_3_ITEM.get());
            event.accept(CARGO_ROCKET_TIER_4_ITEM.get());
            event.accept(ROCKET_SCANNER_ITEM.get());
        }
    }
}
