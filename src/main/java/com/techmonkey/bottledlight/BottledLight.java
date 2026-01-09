package com.techmonkey.bottledlight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.CreativeModeTabs;

public class BottledLight implements ModInitializer {

    public static final String MOD_ID = "bottledlight";

    public static Item BOTTLED_LIGHT;
    public static BottledLightBlock BOTTLED_LIGHT_BLOCK;

    @Override
    public void onInitialize() {
        System.out.println("[bottledlight] Initializing Bottled Light mod");

        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bottled_light");
        BOTTLED_LIGHT = net.minecraft.core.Registry.register(
                BuiltInRegistries.ITEM,
                itemId,
                new BottledLightItem(new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, itemId)))
        );

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(content -> content.accept(BOTTLED_LIGHT));

        ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bottled_light_block");
        BOTTLED_LIGHT_BLOCK = net.minecraft.core.Registry.register(
                BuiltInRegistries.BLOCK,
                blockId,
                new BottledLightBlock(
                        net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                .setId(ResourceKey.create(Registries.BLOCK, blockId))
                                .noCollision()
                                .noOcclusion()
                                .strength(0.3F)
                                .sound(net.minecraft.world.level.block.SoundType.GLASS)
                                .lightLevel(state -> state.getValue(BottledLightBlock.LEVEL))
                )
        );

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide()) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);

            BlockPos hitPos = hit.getBlockPos();
            Direction face = hit.getDirection();
            BlockPos offsetPos = hitPos.relative(face);

            net.minecraft.world.level.block.state.BlockState stateAtHit = world.getBlockState(hitPos);
            net.minecraft.world.level.block.state.BlockState stateAtOffset = world.getBlockState(offsetPos);

            if (stack.is(BOTTLED_LIGHT)) {
                BlockPos posToPlace = stateAtHit.is(BOTTLED_LIGHT_BLOCK) ? hitPos : offsetPos;

                net.minecraft.world.level.block.state.BlockState existing = world.getBlockState(posToPlace);
                boolean success = false;

                if (existing.is(BOTTLED_LIGHT_BLOCK)) {
                    world.setBlock(posToPlace,
                            BOTTLED_LIGHT_BLOCK.defaultBlockState().setValue(BottledLightBlock.LEVEL, 15),
                            3);
                    success = true;
                } else if (existing.isAir() || !existing.isSolidRender()) {
                    world.setBlock(posToPlace,
                            BOTTLED_LIGHT_BLOCK.defaultBlockState().setValue(BottledLightBlock.LEVEL, 15),
                            3);
                    success = true;
                }

                if (success) {
                    world.playSound(null, posToPlace, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.9F, 1.15F);
                    spawnFizz((ServerLevel) world, posToPlace, 15);

                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }

                return InteractionResult.PASS;
            }

            if (stack.is(Items.POTION)) {
                PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
                if (pc == null) return InteractionResult.PASS;

                // Water bottle check
                var waterKey = Potions.WATER.unwrapKey().orElse(null);
                var heldKey  = pc.potion().flatMap(Holder::unwrapKey).orElse(null);
                boolean isWater = (waterKey != null && waterKey.equals(heldKey));
                if (!isWater) return InteractionResult.PASS;

                BlockPos posToDim = null;
                if (stateAtHit.is(BOTTLED_LIGHT_BLOCK)) {
                    posToDim = hitPos;
                } else if (stateAtOffset.is(BOTTLED_LIGHT_BLOCK)) {
                    posToDim = offsetPos;
                }

                if (posToDim == null) return InteractionResult.PASS;

                net.minecraft.world.level.block.state.BlockState lightState = world.getBlockState(posToDim);
                int current = lightState.getValue(BottledLightBlock.LEVEL);

                int next = current - 1;
                if (next <= 0) {
                    world.removeBlock(posToDim, false);
                } else {
                    world.setBlock(posToDim, lightState.setValue(BottledLightBlock.LEVEL, next), 3);
                }

                world.playSound(null, posToDim, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.35F, 1.15F);
                spawnFizz((ServerLevel) world, posToDim, Math.max(next, 1));

                if (!player.isCreative()) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                }

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        System.out.println("[bottledlight] Bottled Light mod initialized");
    }

    private static void spawnFizz(ServerLevel level, BlockPos pos, int light) {
        int count = (light >= 8) ? 2 : 1;

        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.POOF,
                pos.getX() + 0.5,
                pos.getY() + 0.55,
                pos.getZ() + 0.5,
                count,
                0.002, 0.008, 0.002,
                0.01
        );
    }

}
