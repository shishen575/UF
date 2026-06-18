package uk.co.cablepost.ad_astra_cargo_rockets.scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RocketListResponsePacket {

    public final List<RocketInfo> rockets;

    public RocketListResponsePacket(List<RocketInfo> rockets) {
        this.rockets = rockets;
    }

    public static void encode(RocketListResponsePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.rockets.size());
        for (RocketInfo r : pkt.rockets) r.write(buf);
    }

    public static RocketListResponsePacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<RocketInfo> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(RocketInfo.read(buf));
        return new RocketListResponsePacket(list);
    }

    public static void handle(RocketListResponsePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(pkt)));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(RocketListResponsePacket pkt) {
        if (Minecraft.getInstance().screen instanceof ScannerScreen screen) {
            screen.updateRocketList(pkt.rockets);
        }
    }
}
