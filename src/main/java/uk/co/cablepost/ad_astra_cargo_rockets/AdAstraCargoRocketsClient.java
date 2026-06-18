package uk.co.cablepost.ad_astra_cargo_rockets;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketEntityRenderer;
import uk.co.cablepost.ad_astra_cargo_rockets.launch_pad.LaunchPadBlockEntityRenderer;
import uk.co.cablepost.ad_astra_cargo_rockets.launch_pad.LaunchPadInit;

public class AdAstraCargoRocketsClient {

    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(AdAstraCargoRockets.CARGO_ROCKET_ENTITY.get(), CargoRocketEntityRenderer::new);
            AdAstraCargoRockets.LAUNCH_PAD.clientSetup();
            AdAstraCargoRockets.CARGO_ROCKET_MENU.clientSetup();
        });
    }
}
