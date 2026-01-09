package com.techmonkey.bottledlight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BottledLightClient implements ClientModInitializer {

    private static final int RADIUS_XZ = 10;
    private static final int RADIUS_Y  = 6;

    // How often to run the scan (lower = more sparkle, higher = less CPU)
    private static final int TICK_INTERVAL = 2;

    private static final int SPARKLE_CHANCE = 2; // 1 = always, 2 = 50%, 3 = 33%, etc.

    private static final ParticleOptions SPARKLE = ParticleTypes.ELECTRIC_SPARK;

    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            if (!isHoldingBottledLight(mc)) return;

            tickCounter++;
            if (tickCounter % TICK_INTERVAL != 0) return;

            spawnSparkles(mc.level, mc.player.blockPosition());
        });
    }

    private static boolean isHoldingBottledLight(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off  = mc.player.getOffhandItem();
        return main.is(BottledLight.BOTTLED_LIGHT) || off.is(BottledLight.BOTTLED_LIGHT);
    }

    private static void spawnSparkles(Level level, BlockPos center) {
        RandomSource rng = level.getRandom();

        for (int dx = -RADIUS_XZ; dx <= RADIUS_XZ; dx++) {
            for (int dy = -RADIUS_Y; dy <= RADIUS_Y; dy++) {
                for (int dz = -RADIUS_XZ; dz <= RADIUS_XZ; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);

                    BlockState state = level.getBlockState(pos);
                    if (!state.is(BottledLight.BOTTLED_LIGHT_BLOCK)) continue;

                    // Not every light sparkles every pass
                    if (rng.nextInt(SPARKLE_CHANCE) != 0) continue;

                    // 1–3 sparkles near the top-center of the block
                    int count = 1 + rng.nextInt(3);
                    for (int i = 0; i < count; i++) {
                        double x = pos.getX() + 0.5 + (rng.nextDouble() - 0.5) * 0.18;
                        double y = pos.getY() + 0.75 + (rng.nextDouble() * 0.2);
                        double z = pos.getZ() + 0.5 + (rng.nextDouble() - 0.5) * 0.18;

                        // Tiny drift, keep it tight so it doesn't "jet" away
                        double vx = (rng.nextDouble() - 0.5) * 0.003;
                        double vy = rng.nextDouble() * 0.006;
                        double vz = (rng.nextDouble() - 0.5) * 0.003;

                        level.addParticle(SPARKLE, x, y, z, vx, vy, vz);
                    }
                }
            }
        }
    }
}
