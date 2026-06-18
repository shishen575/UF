package uk.co.cablepost.ad_astra_cargo_rockets.scanner;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** ロケット1台分の表示用情報。サーバー→クライアント転送用のDTO。 */
public class RocketInfo {
    public int entityId;
    public String name;
    public String dimension;
    public int x, y, z;
    public int tier;
    public String flightState;     // "grounded" / "ascending" / "descending" / "in_flight"
    public String statusOverride;  // Luaから送られた状態文字列（空なら自動推測を使う）
    public String autoWaitReason;  // MOD側の自動推測（"not_enough_energy" 等）
    public String targetPlanet;    // 飛行中の目的地ディメンションID。地上では空文字。
    public List<SlotInfo> inventory = new ArrayList<>();

    // 燃料・カーゴ流体はv1.2.4でロケット自身のタンクに移管されたため、飛行中でも
    // 常に取得できる（hasLaunchPadは「待機理由」の判定に使う隣接ランチパッドの有無のみを表す）。
    public boolean hasLaunchPad;
    public int fuel, maxFuel;
    public String fuelType = "empty";
    public int cargoFluid, maxCargoFluid;
    public String cargoFluidType = "empty";

    public static class SlotInfo {
        public int slot;
        public String itemId;
        public String displayName;
        public int count;

        public void write(FriendlyByteBuf buf) {
            buf.writeInt(slot);
            buf.writeUtf(itemId);
            buf.writeUtf(displayName);
            buf.writeInt(count);
        }

        public static SlotInfo read(FriendlyByteBuf buf) {
            SlotInfo s = new SlotInfo();
            s.slot = buf.readInt();
            s.itemId = buf.readUtf();
            s.displayName = buf.readUtf();
            s.count = buf.readInt();
            return s;
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(name);
        buf.writeUtf(dimension);
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z);
        buf.writeInt(tier);
        buf.writeUtf(flightState);
        buf.writeUtf(statusOverride);
        buf.writeUtf(autoWaitReason);
        buf.writeUtf(targetPlanet == null ? "" : targetPlanet);
        buf.writeInt(inventory.size());
        for (SlotInfo s : inventory) s.write(buf);
        buf.writeBoolean(hasLaunchPad);
        buf.writeInt(fuel); buf.writeInt(maxFuel); buf.writeUtf(fuelType);
        buf.writeInt(cargoFluid); buf.writeInt(maxCargoFluid); buf.writeUtf(cargoFluidType);
    }

    public static RocketInfo read(FriendlyByteBuf buf) {
        RocketInfo r = new RocketInfo();
        r.entityId = buf.readInt();
        r.name = buf.readUtf();
        r.dimension = buf.readUtf();
        r.x = buf.readInt(); r.y = buf.readInt(); r.z = buf.readInt();
        r.tier = buf.readInt();
        r.flightState = buf.readUtf();
        r.statusOverride = buf.readUtf();
        r.autoWaitReason = buf.readUtf();
        r.targetPlanet = buf.readUtf();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) r.inventory.add(SlotInfo.read(buf));
        r.hasLaunchPad = buf.readBoolean();
        r.fuel = buf.readInt(); r.maxFuel = buf.readInt(); r.fuelType = buf.readUtf();
        r.cargoFluid = buf.readInt(); r.maxCargoFluid = buf.readInt(); r.cargoFluidType = buf.readUtf();
        return r;
    }
}
