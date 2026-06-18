package uk.co.cablepost.ad_astra_cargo_rockets.scanner;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketEntity;

import java.util.function.Supplier;

/** Scanner GUIのテキスト入力欄からロケットの名前を変更するリクエスト。 */
public class RenameRocketPacket {

    public final int entityId;
    public final String newName;

    public RenameRocketPacket(int entityId, String newName) {
        this.entityId = entityId;
        this.newName = newName;
    }

    public static void encode(RenameRocketPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entityId);
        buf.writeUtf(pkt.newName);
    }

    public static RenameRocketPacket decode(FriendlyByteBuf buf) {
        return new RenameRocketPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(RenameRocketPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null || player.getServer() == null) return;
            // 名前は最大32文字に制限（GUI崩れ防止）。サロゲートペアの途中で切らないよう
            // offsetByCodePoints を使う（絵文字等の非BMP文字が含まれる場合の文字化け防止）。
            String safeName = pkt.newName == null ? "" : pkt.newName;
            if (safeName.length() > 32) {
                int cut = Math.min(32, safeName.codePointCount(0, safeName.length()));
                int end = safeName.offsetByCodePoints(0, cut);
                safeName = safeName.substring(0, end);
            }

            for (ServerLevel level : player.getServer().getAllLevels()) {
                var entity = level.getEntity(pkt.entityId);
                if (entity instanceof CargoRocketEntity rocket) {
                    rocket.setRocketName(safeName);
                    break;
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
