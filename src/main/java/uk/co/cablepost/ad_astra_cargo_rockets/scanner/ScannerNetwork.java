package uk.co.cablepost.ad_astra_cargo_rockets.scanner;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class ScannerNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AdAstraCargoRockets.MOD_ID, "scanner"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++,
                RequestRocketListPacket.class,
                RequestRocketListPacket::encode,
                RequestRocketListPacket::decode,
                RequestRocketListPacket::handle);

        CHANNEL.registerMessage(id++,
                RocketListResponsePacket.class,
                RocketListResponsePacket::encode,
                RocketListResponsePacket::decode,
                RocketListResponsePacket::handle);

        CHANNEL.registerMessage(id++,
                RenameRocketPacket.class,
                RenameRocketPacket::encode,
                RenameRocketPacket::decode,
                RenameRocketPacket::handle);
    }
}
