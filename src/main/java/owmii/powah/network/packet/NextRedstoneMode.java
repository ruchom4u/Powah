package owmii.powah.network.packet;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import owmii.lib.network.IPacket;
import owmii.lib.util.Server;
import owmii.powah.block.PowahTile;

import java.util.Optional;
import java.util.function.Supplier;

public class NextRedstoneMode implements IPacket<NextRedstoneMode> {
    private int dim;
    private BlockPos pos;

    public NextRedstoneMode(int dim, BlockPos pos) {
        this.pos = pos;
        this.dim = dim;
    }

    public NextRedstoneMode() {
        this(0, BlockPos.ZERO);
    }

    @Override
    public void encode(NextRedstoneMode msg, PacketBuffer buffer) {
        buffer.writeInt(msg.dim);
        buffer.writeBlockPos(msg.pos);
    }

    @Override
    public NextRedstoneMode decode(PacketBuffer buffer) {
        return new NextRedstoneMode(buffer.readInt(), buffer.readBlockPos());
    }

    @Override
    public void handle(NextRedstoneMode msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Optional<ServerWorld> world = Server.getWorld(msg.dim);
            world.ifPresent(serverWorld -> {
                TileEntity tileEntity = serverWorld.getTileEntity(msg.pos);
                if (tileEntity instanceof PowahTile) {
                    ((PowahTile) tileEntity).nextRedstoneMode();
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
