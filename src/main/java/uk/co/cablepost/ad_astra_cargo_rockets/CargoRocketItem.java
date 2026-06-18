package uk.co.cablepost.ad_astra_cargo_rockets;

import net.minecraft.world.item.Item;

public class CargoRocketItem extends Item {
    public final int tier;

    public CargoRocketItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }
}
