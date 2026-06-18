package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

/** ロケット自身のインベントリ/燃料/カーゴ流体GUI用のメニュータイプを登録する。 */
public class CargoRocketInit {

    private RegistryObject<MenuType<RocketMenu>> menuType;

    public void register(IEventBus bus) {
        DeferredRegister<MenuType<?>> menus = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AdAstraCargoRockets.MOD_ID);
        menuType = menus.register("cargo_rocket", () -> IForgeMenuType.create((syncId, inv, buf) -> {
            int entityId = buf.readVarInt();
            CargoRocketEntity rocket = null;
            if (Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getEntity(entityId) instanceof CargoRocketEntity r) {
                rocket = r;
            }
            return new RocketMenu(syncId, inv, rocket);
        }));
        menus.register(bus);
    }

    public RegistryObject<MenuType<RocketMenu>> getMenuType() { return menuType; }

    @OnlyIn(Dist.CLIENT)
    public void clientSetup() {
        MenuScreens.register(menuType.get(), RocketScreen::new);
    }
}
