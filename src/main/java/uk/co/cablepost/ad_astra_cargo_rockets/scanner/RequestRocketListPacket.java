package uk.co.cablepost.ad_astra_cargo_rockets.scanner;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket.CargoRocketEntity;
import uk.co.cablepost.ad_astra_cargo_rockets.launch_pad.LaunchPadBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** クライアントがScanner GUIを開いたときに送るリクエスト。サーバー上の全ロケットを集めて返す。 */
public class RequestRocketListPacket {

    public RequestRocketListPacket() {}

    public static void encode(RequestRocketListPacket pkt, FriendlyByteBuf buf) {}

    public static RequestRocketListPacket decode(FriendlyByteBuf buf) {
        return new RequestRocketListPacket();
    }

    public static void handle(RequestRocketListPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null || player.getServer() == null) return;

            List<RocketInfo> result = new ArrayList<>();
            for (ServerLevel level : player.getServer().getAllLevels()) {
                List<CargoRocketEntity> rockets = new ArrayList<>();
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof CargoRocketEntity rocket && rocket.isAlive()) {
                        rockets.add(rocket);
                    }
                }
                for (CargoRocketEntity rocket : rockets) {
                    RocketInfo info = new RocketInfo();
                    info.entityId = rocket.getId();
                    info.name = rocket.getRocketName();
                    info.dimension = level.dimension().location().toString();
                    info.x = (int) Math.floor(rocket.getX());
                    info.y = (int) Math.floor(rocket.getY());
                    info.z = (int) Math.floor(rocket.getZ());
                    info.tier = rocket.getTier();
                    info.flightState = rocket.getFlightState();
                    info.statusOverride = rocket.statusOverride;
                    info.targetPlanet = rocket.targetPlanet;

                    // 隣接ランチパッドの検索は飛行中なら無意味なのでスキップする
                    // （上空にいるロケットの近くにランチパッドは無いのが正常なので、
                    // 探索コストを省きつつ "unknown" へのフォールバックも防ぐ）。
                    LaunchPadBlockEntity adjacentPad = "grounded".equals(info.flightState)
                            ? findAdjacentLaunchPad(level, rocket) : null;

                    String autoReason;
                    if (!"grounded".equals(info.flightState)) {
                        autoReason = "in_flight";
                    } else {
                        autoReason = adjacentPad != null ? adjacentPad.inferWaitReason() : "no_launchpad";
                    }
                    info.autoWaitReason = autoReason;
                    info.hasLaunchPad = adjacentPad != null;

                    // 燃料・カーゴ流体はv1.2.4でロケット自身のタンクに移管されたため、
                    // ランチパッドの有無に関わらず常に取得できる（飛行中も表示可能）。
                    info.fuel = rocket.fuelTank.getFluidAmount();
                    info.maxFuel = rocket.fuelTank.getCapacity();
                    info.fuelType = fluidTypeId(rocket.fuelTank.getFluid());
                    info.cargoFluid = rocket.cargoFluidTank.getFluidAmount();
                    info.maxCargoFluid = rocket.cargoFluidTank.getCapacity();
                    info.cargoFluidType = fluidTypeId(rocket.cargoFluidTank.getFluid());

                    for (int i = 0; i < rocket.getInventory().getContainerSize(); i++) {
                        var stack = rocket.getInventory().getItem(i);
                        if (stack.isEmpty()) continue;
                        RocketInfo.SlotInfo slot = new RocketInfo.SlotInfo();
                        slot.slot = i + 1;
                        slot.itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                        slot.displayName = stack.getHoverName().getString();
                        slot.count = stack.getCount();
                        info.inventory.add(slot);
                    }
                    result.add(info);
                }
            }
            ScannerNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new RocketListResponsePacket(result));
        });
        ctx.setPacketHandled(true);
    }

    /**
     * ロケットの位置から最も近いランチパッドを探す。
     * LaunchPadBlockEntity.getRocket() が中心ブロックから半径2マスでロケットを探すのと
     * 整合させるため、ここでも半径2マスの範囲を走査する（ロケットがランチパッドの
     * 3x3マルチブロックの隅にある場合、隣接1マスだけでは中心ブロックを取り逃すため）。
     */
    private static LaunchPadBlockEntity findAdjacentLaunchPad(ServerLevel level, CargoRocketEntity rocket) {
        var pos = rocket.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    var be = level.getBlockEntity(pos.offset(dx, dy, dz));
                    if (be instanceof LaunchPadBlockEntity lp && lp.getRocket() == rocket) return lp;
                }
            }
        }
        return null;
    }

    private static String fluidTypeId(net.minecraftforge.fluids.FluidStack fluid) {
        if (fluid.isEmpty()) return "empty";
        var key = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return key != null ? key.toString() : "empty";
    }
}
