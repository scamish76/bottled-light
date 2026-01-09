package com.techmonkey.bottledlight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BottledLightBlock extends Block {

    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    private static final VoxelShape HITBOX = Block.box(4, 4, 4, 12, 12, 12);
    private static final int MAX_LEVEL = 15;

    public BottledLightBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, MAX_LEVEL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (isHoldingBottledLight(ctx)) {
            return Shapes.block();
        }

        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    private static boolean isHoldingBottledLight(CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ecc) {
            Entity e = ecc.getEntity();
            if (e instanceof Player p) {
                ItemStack main = p.getMainHandItem();
                ItemStack off  = p.getOffhandItem();
                return main.is(BottledLight.BOTTLED_LIGHT) || off.is(BottledLight.BOTTLED_LIGHT);
            }
        }
        return false;
    }
}
