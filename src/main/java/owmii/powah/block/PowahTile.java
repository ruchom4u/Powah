package owmii.powah.block;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import owmii.lib.block.TileBase;
import owmii.lib.util.Energy;
import owmii.powah.energy.PowahStorage;
import owmii.powah.energy.RedstoneMode;
import owmii.powah.energy.SideConfig;

import javax.annotation.Nullable;

public abstract class PowahTile extends TileBase.Tickable {
    protected RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    public final PowahStorage internal;
    protected SideConfig sideConfig;
    protected boolean isCreative;

    public PowahTile(TileEntityType<?> type, int capacity, int maxReceive, int maxExtract, boolean isCreative) {
        super(type);
        this.internal = new PowahStorage(capacity, maxReceive, maxExtract);
        this.sideConfig = new SideConfig(this);
        this.isCreative = isCreative;
        if (isCreative) {
            this.internal.setEnergy(capacity);
            this.internal.setMaxReceive(0);
        }

        this.inv.add(getChargingSlots() + getUpgradeSlots());
    }

    @Override
    public void readStorable(CompoundNBT compound) {
        super.readStorable(compound);
        this.internal.read(compound);
        this.isCreative = compound.getBoolean("IsCreative");

    }

    @Override
    public CompoundNBT writeStorable(CompoundNBT compound) {
        this.internal.write(compound);
        compound.putBoolean("IsCreative", this.isCreative);
        return super.writeStorable(compound);
    }

    @Override
    public void readSync(CompoundNBT compound) {
        super.readSync(compound);
        this.sideConfig.read(compound);
        this.redstoneMode = RedstoneMode.values()[compound.getInt("RedstoneMode")];
    }

    @Override
    public CompoundNBT writeSync(CompoundNBT compound) {
        this.sideConfig.write(compound);
        compound.putInt("RedstoneMode", this.redstoneMode.ordinal());
        return super.writeSync(compound);
    }

    @Override
    protected void firstTick() {
        if (this.world == null) return;
        if (!this.world.isRemote) {
            if (getBlock() instanceof PowahBlock) {
                PowahBlock powahBlock = (PowahBlock) getBlock();
                this.internal.setCapacity(powahBlock.capacity);
                this.internal.setMaxExtract(powahBlock.maxExtract);
                this.internal.setMaxReceive(powahBlock.maxReceive);
                if (this.isCreative) {
                    this.internal.setEnergy(powahBlock.capacity);
                    this.internal.setMaxExtract(powahBlock.capacity);
                    this.internal.setMaxReceive(0);
                }
                markDirtyAndSync();
            }
        }
    }

    @Override
    protected boolean postTicks() {
        if (this.world == null) return false;
        if (this.world.isRemote) return false;

        int extracted = 0;

        if (canExtractFromSides()) {
            for (Direction direction : Direction.values()) {
                if (canExtract(direction)) {
                    int amount = Math.min(getMaxExtract(), getEnergyStored());
                    int received = Energy.receive(this.world.getTileEntity(this.pos.offset(direction)), direction, amount, false);
                    extracted += extractEnergy(received, false, direction);
                }
            }
        }

        if (canChargeItems() && canExtract(null)) {
            for (int i = 0; i < getChargingSlots(); i++) {
                extracted += chargeItem(this.inv.getStackInSlot(i));
            }
        }
        return extracted > 0;
    }

    protected int chargeItem(ItemStack stack) {
        return chargeItem(stack, getMaxExtract());
    }

    protected int chargeItem(ItemStack stack, int transfer) {
        if (!stack.isEmpty()) {
            int amount = Math.min(transfer, getEnergyStored());
            int received = Energy.receive(stack, amount, false);
            return extractEnergy(received, false, null);
        }
        return 0;
    }

    @Override
    public int getSyncTicks() {
        return isContainerOpen() ? 5 : 20;
    }

    @Override
    public int getSlotLimit(int index) {
        if (index < builtInSlots()) {
            return 1;
        }
        return 64;
    }

    public int[] nonBuiltInSlots() {
        int[] slots = new int[this.inv.getSlots() - builtInSlots()];
        for (int i = builtInSlots(); i < this.inv.getSlots(); i++) {
            slots[i] = i - builtInSlots();
        }
        return slots;
    }

    public int builtInSlots() {
        return getChargingSlots() + getUpgradeSlots();
    }

    private int getUpgradeSlots() {
        return 0;
    }

    public int getChargingSlots() {
        return 0;
    }

    public int receiveEnergy(int maxReceive, boolean simulate, @Nullable Direction side) {
        if (!canReceive(side)) return 0;
        int energyReceived = Math.min(getCapacity() - getEnergyStored(), Math.min(getMaxReceive(), maxReceive));
        if (!simulate) {
            setEnergy(getEnergyStored() + energyReceived, side);
            if (energyReceived > 0) {
                sync(getSyncTicks());
            }
        }
        return energyReceived;
    }

    public int extractEnergy(int maxExtract, boolean simulate, @Nullable Direction side) {
        if (!canExtract(side)) return 0;
        int energyExtracted = Math.min(getEnergyStored(), Math.min(getMaxExtract(), maxExtract));
        if (!simulate && !this.isCreative) {
            setEnergy(getEnergyStored() - energyExtracted, side);
            if (energyExtracted > 0) {
                sync(getSyncTicks());
            }
        }
        return energyExtracted;
    }

    public int getMaxReceive() {
        return this.internal.getMaxReceive();
    }

    public int getMaxExtract() {
        return this.internal.getMaxExtract();
    }

    public void setEnergy(int amount, @Nullable Direction side) {
        this.internal.setEnergy(amount);
    }

    public int getEnergyStored() {
        return this.internal.getEnergyStored();
    }

    public int getCapacity() {
        return this.internal.getMaxEnergyStored();
    }

    public PowahStorage getInternal() {
        return this.internal;
    }

    public SideConfig getSideConfig() {
        return this.sideConfig;
    }

    public boolean isCreative() {
        return this.isCreative;
    }

    public boolean canExtract(@Nullable Direction side) {
        return checkRedstone() && (isOut(side) || side == null) && this.internal.canExtract();
    }

    public boolean isOut(@Nullable Direction side) {
        return side != null && this.sideConfig.getPowerMode(side).isOut();
    }

    public boolean canReceive(@Nullable Direction side) {
        return checkRedstone() && (isIn(side) || side == null) && this.internal.canReceive();
    }

    public boolean isIn(@Nullable Direction side) {
        return side != null && this.sideConfig.getPowerMode(side).isIn();
    }

    public RedstoneMode getRedstoneMode() {
        return this.redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
    }

    public void nextRedstoneMode() {
        int i = this.redstoneMode.ordinal() + 1;
        RedstoneMode redstoneMode = RedstoneMode.values()[i > 2 ? 0 : i];
        this.redstoneMode = redstoneMode;
        markDirtyAndSync();
    }

    @Override
    protected boolean doTick() {
        return checkRedstone();
    }

    public boolean checkRedstone() {
        boolean power = this.world != null && this.world.getRedstonePowerFromNeighbors(this.pos) > 0;
        return RedstoneMode.IGNORE.equals(getRedstoneMode())
                || power && RedstoneMode.ON.equals(getRedstoneMode())
                || !power && RedstoneMode.OFF.equals(getRedstoneMode());
    }

    @Override
    public boolean keepInventory() {
        return true;
    }

    @Override
    public boolean canInsert(int index, ItemStack stack) {
        if (index < getChargingSlots()) {
            return Energy.isPresent(stack);
        }
        return true;
    }

    protected ExtractionType getExtractionType() {
        return ExtractionType.ALL;
    }

    public boolean isExtractor() {
        return canExtractFromSides();
    }

    public boolean canExtractFromSides() {
        return getExtractionType().equals(ExtractionType.ALL) || getExtractionType().equals(ExtractionType.TILE);
    }

    public boolean canChargeItems() {
        return getExtractionType().equals(ExtractionType.ALL) || getExtractionType().equals(ExtractionType.ITEM);
    }

    public enum ExtractionType {
        ALL, ITEM, TILE, OFF
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityEnergy.ENERGY && hasWorld() && isEnergyPresent(side) ? LazyOptional.of(() -> new PowahStorage(getInternal()) {
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return PowahTile.this.extractEnergy(maxExtract, simulate, side);
            }

            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return PowahTile.this.receiveEnergy(maxReceive, simulate, side);
            }

            @Override
            public boolean canReceive() {
                return PowahTile.this.canReceive(side);
            }

            @Override
            public boolean canExtract() {
                return PowahTile.this.canExtract(side);
            }
        }).cast() : super.getCapability(cap, side);
    }

    public boolean isEnergyPresent(@Nullable Direction side) {
        return true;
    }
}
