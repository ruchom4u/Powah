package owmii.powah.block.cable;

import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import owmii.lib.block.TileBase;
import owmii.lib.client.handler.IHud;
import owmii.lib.inventory.ContainerBase;
import owmii.lib.util.Energy;
import owmii.lib.util.Server;
import owmii.lib.util.Side;
import owmii.powah.api.cable.ICable;
import owmii.powah.block.PowahBlock;
import owmii.powah.energy.PowerMode;
import owmii.powah.inventory.CableContainer;
import owmii.powah.inventory.IContainers;
import owmii.powah.item.WrenchItem;

import javax.annotation.Nullable;
import java.util.*;

public class CableBlock extends PowahBlock implements ICable, IHud, IWaterLoggable {
    public static final BooleanProperty NORTH = SixWayBlock.NORTH;
    public static final BooleanProperty EAST = SixWayBlock.EAST;
    public static final BooleanProperty SOUTH = SixWayBlock.SOUTH;
    public static final BooleanProperty WEST = SixWayBlock.WEST;
    public static final BooleanProperty UP = SixWayBlock.UP;
    public static final BooleanProperty DOWN = SixWayBlock.DOWN;
    public static final BooleanProperty TILE = BooleanProperty.create("tile");

    private static final VoxelShape CABLE = makeCuboidShape(6.5, 6.5, 6.5, 9.5, 9.5, 9.5);
    private static final VoxelShape[] MULTIPART = new VoxelShape[]{
            makeCuboidShape(7, 7, 0, 9, 9, 6.5),
            makeCuboidShape(9.5, 7, 7, 16, 9, 9),
            makeCuboidShape(7, 7, 9.5, 9, 9, 16),
            makeCuboidShape(0, 7, 7, 6.5, 9, 9),
            makeCuboidShape(7, 9.5, 7, 9, 16, 9),
            makeCuboidShape(7, 0, 7, 9, 6.5, 9)
    };

    public CableBlock(Properties properties, int maxExtract, int maxReceive) {
        super(properties.sound(SoundType.METAL), 0, maxExtract, maxReceive);
        setDefaultState(this.stateContainer.getBaseState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(UP, false).with(DOWN, false).with(TILE, false).with(WATERLOGGED, false));
    }

    @Override
    public int stackSize() {
        return 64;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        VoxelShape voxelShape = CABLE;
        if (state.get(NORTH) || canConnectEnergy(worldIn, pos, Direction.NORTH))
            voxelShape = VoxelShapes.or(voxelShape, MULTIPART[0]);
        if (state.get(EAST) || canConnectEnergy(worldIn, pos, Direction.EAST))
            voxelShape = VoxelShapes.or(voxelShape, MULTIPART[1]);
        if (state.get(SOUTH) || canConnectEnergy(worldIn, pos, Direction.SOUTH))
            voxelShape = VoxelShapes.or(voxelShape, MULTIPART[2]);
        if (state.get(WEST) || canConnectEnergy(worldIn, pos, Direction.WEST))
            voxelShape = VoxelShapes.or(voxelShape, MULTIPART[3]);
        if (state.get(UP) || canConnectEnergy(worldIn, pos, Direction.UP))
            voxelShape = VoxelShapes.or(voxelShape, MULTIPART[4]);
        if (state.get(DOWN) || canConnectEnergy(worldIn, pos, Direction.DOWN))
            voxelShape = VoxelShapes.or(voxelShape, MULTIPART[5]);
        return voxelShape;
    }

    @Nullable
    @Override
    public ContainerBase getContainer(int id, PlayerInventory playerInventory, TileBase inv) {
        if (inv instanceof CableTile) {
            return new CableContainer(IContainers.CABLE, id, playerInventory, (CableTile) inv);
        }
        return null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return state.get(TILE);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new CableTile(this.maxReceive, this.maxExtract, this.isCreative);
    }

    @Override
    public boolean isSolid(BlockState state) {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean addDestroyEffects(BlockState state, World world, BlockPos pos, ParticleManager manager) {
        VoxelShape voxelshape = CABLE;
        voxelshape.forEachBox((p_199284_3_, p_199284_5_, p_199284_7_, p_199284_9_, p_199284_11_, p_199284_13_) -> {
            double d1 = Math.min(1.0D, p_199284_9_ - p_199284_3_);
            double d2 = Math.min(1.0D, p_199284_11_ - p_199284_5_);
            double d3 = Math.min(1.0D, p_199284_13_ - p_199284_7_);
            int i = Math.max(2, MathHelper.ceil(d1 / 0.25D));
            int j = Math.max(2, MathHelper.ceil(d2 / 0.25D));
            int k = Math.max(2, MathHelper.ceil(d3 / 0.25D));
            for (int l = 0; l < i; ++l) {
                for (int i1 = 0; i1 < j; ++i1) {
                    for (int j1 = 0; j1 < k; ++j1) {
                        double d4 = ((double) l + 0.5D) / (double) i;
                        double d5 = ((double) i1 + 0.5D) / (double) j;
                        double d6 = ((double) j1 + 0.5D) / (double) k;
                        double d7 = d4 * d1 + p_199284_3_;
                        double d8 = d5 * d2 + p_199284_5_;
                        double d9 = d6 * d3 + p_199284_7_;
                        Minecraft.getInstance().particles.addEffect((new DiggingParticle(world, (double) pos.getX() + d7, (double) pos.getY() + d8, (double) pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, state)).setBlockPos(pos));
                    }
                }
            }
        });
        return true;
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos) {
        return newState(world, currentPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return newState(context.getWorld(), context.getPos());
    }

    private BlockState newState(IWorld world, BlockPos pos) {
        final BlockState state = getDefaultState();

        boolean[] north = canAttach(state, world, pos, Direction.NORTH);
        boolean[] south = canAttach(state, world, pos, Direction.SOUTH);
        boolean[] west = canAttach(state, world, pos, Direction.WEST);
        boolean[] east = canAttach(state, world, pos, Direction.EAST);
        boolean[] up = canAttach(state, world, pos, Direction.UP);
        boolean[] down = canAttach(state, world, pos, Direction.DOWN);

        boolean tile = false;
        TileEntity tileEntity = world.getTileEntity(pos);
        if (north[1] || south[1] || west[1] || east[1] || up[1] || down[1]) {
            tile = true;
        } else {
            if (tileEntity instanceof CableTile) {
                CableTile cable = (CableTile) tileEntity;
                cable.getInventory().drop((World) world, pos);
                if (world instanceof ServerWorld) {
                    CableNoneTileData data = Server.getData(CableNoneTileData::new, ((ServerWorld) world).dimension);
                    data.add(pos, cable.getSideConfig().write(new CompoundNBT()));
                    ((ServerWorld) world).getChunkAt(pos).markDirty();
                }
                tileEntity.remove();
            }
        }
        IFluidState ifluidstate = world.getFluidState(pos);
        return state
                .with(NORTH, north[0] && !north[1])
                .with(SOUTH, south[0] && !south[1])
                .with(WEST, west[0] && !west[1])
                .with(EAST, east[0] && !east[1])
                .with(UP, up[0] && !up[1])
                .with(DOWN, down[0] && !down[1])
                .with(TILE, tile)
                .with(WATERLOGGED, ifluidstate.getFluid() == Fluids.WATER);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (worldIn.isRemote) return;
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof CableTile) {
            CableTile cable = (CableTile) tileEntity;
            for (Direction direction : Direction.values()) {
                if (cable.canExtract(direction)) {
                    cable.search(this, direction);
                }
            }
        } else {
            searchCables(worldIn, pos, pos);
        }
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof CableTile) {
            CableTile cable = (CableTile) tileEntity;
            cable.energySides.clear();
            for (Direction direction : Direction.values()) {
                if (canAttach(state, world, pos, direction)[1]) {
                    cable.energySides.add(direction);
                }
            }
            if (world instanceof ServerWorld && cable.ticks == 0) {
                CableNoneTileData data = Server.getData(CableNoneTileData::new, ((ServerWorld) world).dimension);
                CompoundNBT nbt = data.get(pos);
                if (nbt != null) {// TODO keep data on tile entity remove
                    cable.getSideConfig().read(nbt);
                }
            }
            cable.sync(1);
        }
        super.onBlockAdded(state, world, pos, oldState, isMoving);
    }

    public boolean[] canAttach(BlockState state, IWorld world, BlockPos pos, Direction direction) {
        return new boolean[]{world.getBlockState(pos.offset(direction)).getBlock() == this || checkEnergyTile(world, pos, direction), checkEnergyTile(world, pos, direction)};
    }

    public boolean checkEnergyTile(IWorld world, BlockPos pos, Direction direction) {
        return canConnectEnergy(world, pos, direction);
    }

    public boolean canConnectEnergy(IBlockReader world, BlockPos pos, Direction direction) {
        BlockPos pos1 = pos.offset(direction);
        TileEntity tileEntity = world.getTileEntity(pos1);
        return !(tileEntity instanceof CableTile) && Energy.isPresent(tileEntity, direction);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, TILE);
        super.fillStateContainer(builder);
    }

    public void searchCables(IWorld world, BlockPos pos, CableTile first, Direction side) {
        if (!first.linkedCables.get(side).searchCach.contains(pos)) {
            for (Direction direction : Direction.values()) {
                BlockPos blockPos = pos.offset(direction);
                BlockState state = world.getBlockState(blockPos);
                if (state.getBlock() == this) {
                    TileEntity tileEntity = world.getTileEntity(blockPos);
                    if (tileEntity instanceof CableTile) {
                        CableTile cable = (CableTile) tileEntity;
                        first.linkedCables.get(side).add(blockPos);
                    }
                    CableBlock cableBlock = (CableBlock) state.getBlock();
                    first.linkedCables.get(side).searchCach.add(pos);
                    cableBlock.searchCables(world, blockPos, first, side);
                }
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
        if (Energy.isPresent(world.getTileEntity(fromPos), Side.fromNeighbor(pos, fromPos))) {
            searchCables(world, pos, pos);
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof CableTile) {
                CableTile cable = (CableTile) tileEntity;
                cable.energySides.clear();
                for (Direction direction : Direction.values()) {
                    if (canAttach(state, world, pos, direction)[1]) {
                        cable.energySides.add(direction);
                    }
                }
                cable.markDirtyAndSync();
            }
        }
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        searchCables(worldIn, pos, pos);
        super.onReplaced(state, worldIn, pos, newState, isMoving);
    }

    static final Map<BlockPos, Set<BlockPos>> CACHE = new HashMap<>();

    public void searchCables(IWorld world, BlockPos poss, BlockPos pos) {
        Set<BlockPos> ss = CACHE.get(poss);
        if (ss == null) {
            ss = new HashSet<>();
        }
        if (!ss.contains(pos)) {
            for (Direction direction : Direction.values()) {
                BlockPos blockPos = pos.offset(direction);
                BlockState state = world.getBlockState(blockPos);
                if (state.getBlock() == this) {
                    TileEntity tileEntity = world.getTileEntity(blockPos);
                    if (tileEntity instanceof CableTile) {
                        CableTile cable = (CableTile) tileEntity;
                        for (Direction side : Direction.values()) {
                            cable.linkedCables.get(side).cables().clear();
                            cable.search(this, side);
                        }
                    }
                    CableBlock cableBlock = (CableBlock) state.getBlock();
                    ss.add(pos);
                    CACHE.put(poss, ss);
                    cableBlock.searchCables(world, poss, blockPos);
                }
            }
        }
        CACHE.clear();
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult blockRayTraceResult) {
        ItemStack stack = player.getHeldItem(hand);
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof CableTile) {
            CableTile cable = (CableTile) tileEntity;
            if (stack.getItem() instanceof WrenchItem) {
                Vec3d hit = blockRayTraceResult.getHitVec();
                Optional<Direction> sides = getHitSide(hit, pos);
                boolean[] flag = {false};
                sides.ifPresent(direction -> {
                    if (checkEnergyTile(world, pos, direction)) {
                        cable.getSideConfig().nextPowerMode(direction);
                        flag[0] = true;
                    }
                });
                if (!world.isRemote && flag[0]) {
                    cable.markDirtyAndSync();
                    return true;
                }
            }
        }
        return super.onBlockActivated(state, world, pos, player, hand, blockRayTraceResult);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean renderHud(BlockState state, World world, PlayerEntity player, Hand hand, BlockRayTraceResult result) {
        ItemStack stack = player.getHeldItem(hand);
        TileEntity tileEntity = world.getTileEntity(result.getPos());
        boolean[] flag = {false};
        if (tileEntity instanceof CableTile) {
            CableTile cable = (CableTile) tileEntity;
            if (stack.getItem() instanceof WrenchItem) {
                Vec3d hit = result.getHitVec();
                Optional<Direction> sides = getHitSide(hit, result.getPos());
                sides.ifPresent(direction -> {
                    if (checkEnergyTile(world, result.getPos(), direction)) {
                        Minecraft mc = Minecraft.getInstance();
                        FontRenderer font = mc.fontRenderer;
                        int width = mc.mainWindow.getScaledWidth();
                        int height = mc.mainWindow.getScaledHeight();
                        String s = I18n.format("info.lollipop.side." + direction.getName(), TextFormatting.WHITE);
                        int sw = font.getStringWidth(s);
                        font.drawStringWithShadow(s, -(sw / 2) + (width / 2), height - 84, 0xbfbfbf);
                        PowerMode mode = cable.getSideConfig().getPowerMode(direction);
                        TextFormatting tf = mode.equals(PowerMode.ALL) ? TextFormatting.GRAY : mode.equals(PowerMode.IN) ? TextFormatting.DARK_AQUA : mode.equals(PowerMode.OUT) ? TextFormatting.GOLD : TextFormatting.YELLOW;
                        String sideConf = I18n.format("info.powah.mode.cable." + mode.name().toLowerCase(), TextFormatting.YELLOW);
                        int sc = font.getStringWidth(sideConf);
                        font.drawStringWithShadow(sideConf, -(sc / 2) + (width / 2), height - 98, 0xbfbfbf);
                        flag[0] = true;
                    }
                });
            }
        }
        return flag[0];
    }

    public Optional<Direction> getHitSide(Vec3d hit, BlockPos pos) {
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        if (x > 0.0D && x < 0.4D) {
            return Optional.of(Direction.WEST);
        } else if (x > 0.6D && x < 1.0D) {
            return Optional.of(Direction.EAST);
        } else if (z > 0.0D && z < 0.4D) {
            return Optional.of(Direction.NORTH);
        } else if (z > 0.6D && z < 1.0D) {
            return Optional.of(Direction.SOUTH);
        } else if (y > 0.6D && y < 1.0D) {
            return Optional.of(Direction.UP);
        } else if (y > 0.0D && y < 0.4D) {
            return Optional.of(Direction.DOWN);
        }
        return Optional.empty();
    }
}
