package owmii.powah.block.generator.furnator;

import net.minecraft.block.material.Material;
import owmii.powah.block.IBlocks;

public enum Furnators {
    BASIC(Material.ROCK, 3.0F, 50000, 120, 90, 2),
    HARDENED(Material.ROCK, 30.0F, 100000, 400, 220, 4),
    BLAZING(Material.IRON, 30.0F, 500000, 900, 500, 10),
    NIOTIC(Material.IRON, 100.0F, 1000000, 3000, 2400, 18),
    SPIRITED(Material.IRON, 100.0F, 5000000, 20000, 10000, 30);

    public final Material material;
    public final float resistance;
    public final int capacity;
    public final int transfer;
    public final int perTick;

    Furnators(Material material, float resistance, int capacity, int transfer, int perTick, int buckets) {
        this.material = material;
        this.resistance = resistance;
        this.capacity = capacity;
        this.transfer = transfer;
        this.perTick = perTick;
    }

    public FurnatorBlock get() {
        return IBlocks.FURNATORS[ordinal()];
    }
}
