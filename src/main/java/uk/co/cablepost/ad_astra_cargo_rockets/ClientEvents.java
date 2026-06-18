package uk.co.cablepost.ad_astra_cargo_rockets;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.T1CargoRocketEntityModel;
import uk.co.cablepost.ad_astra_cargo_rockets.launch_pad.LaunchPadModel;

@Mod.EventBusSubscriber(modid = AdAstraCargoRockets.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(T1CargoRocketEntityModel.LAYER_LOCATION,
                T1CargoRocketEntityModel::createBodyLayer);
        event.registerLayerDefinition(LaunchPadModel.LAYER_LOCATION,
                LaunchPadModel::createBodyLayer);
    }
}
