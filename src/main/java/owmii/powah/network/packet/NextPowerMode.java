package owmii.powah.network.packet;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import owmii.lib.network.IPacket;
import owmii.lib.util.Server;
import owmii.powah.block.PowahTile;

import java.util.Optional;
import java.util.function.Supplier;

public class NextPowerMode implements IPacket<NextPowerMode> {
    private int i;
    private int dim;
    private BlockPos pos;

    public NextPowerMode(int i, int dim, BlockPos pos) {
        this.i = i;
        this.dim = dim;
        this.pos = pos;
    }

    public NextPowerMode() {
        this(0, 0, BlockPos.ZERO);
    }

    @Override
    public void encode(NextPowerMode msg, PacketBuffer buffer) {
        buffer.writeInt(msg.i);
        buffer.writeInt(msg.dim);
        buffer.writeBlockPos(msg.pos);
    }

    @Override
    public NextPowerMode decode(PacketBuffer buffer) {
        return new NextPowerMode(buffer.readInt(), buffer.readInt(), buffer.readBlockPos());
    }

    @Override
    public void handle(NextPowerMode msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Optional<ServerWorld> world = Server.getWorld(msg.dim);
            world.ifPresent(serverWorld -> {
                TileEntity tileEntity = serverWorld.getTileEntity(msg.pos);
                if (tileEntity instanceof PowahTile) {
                    if (msg.i == 6) {
                        ((PowahTile) tileEntity).getSideConfig().nextPowerModeAllSides();
                    } else {
                        ((PowahTile) tileEntity).getSideConfig().nextPowerMode(Direction.byIndex(msg.i));
                    }
                    ((PowahTile) tileEntity).markDirtyAndSync();
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
