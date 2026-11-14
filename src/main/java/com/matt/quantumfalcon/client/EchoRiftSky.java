package com.matt.quantumfalcon.client;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * the Echo Rift sky. The dimension is locked at midnight (fixed_time 18000) and used overworld effects, which scale
 * the sky colour to black and the fog to its floor, so the 64 biome palettes never showed. These effects draw no vanilla
 * sky dome (no sun, moon or black sky) and the fog/clear colour becomes the biome's own sky and fog colours, eased between
 * biomes and kept a little dim so the Rift still reads as twilight.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class EchoRiftSky {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "echo_rift");
    private static final float DIM = 0.72f;
    private static float r = -1, g, b;

    private EchoRiftSky() {}

    public static final class Effects extends DimensionSpecialEffects {
        public Effects() { super(Float.NaN, true, SkyType.NONE, false, false); }
        @Override public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) { return fog; }
        @Override public boolean isFoggyAt(int x, int y) { return false; }
    }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent e) { e.register(ID, new Effects()); }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor e) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null || !(level.effects() instanceof Effects)) { r = -1; return; }
        var cam = e.getCamera();
        if (cam.getFluidInCamera() != FogType.NONE) return;
        if (cam.getEntity() instanceof net.minecraft.world.entity.LivingEntity le
                && (le.hasEffect(MobEffects.BLINDNESS) || le.hasEffect(MobEffects.DARKNESS))) return;
        var biome = level.getBiome(cam.getBlockPosition()).value();
        int sky = biome.getSkyColor(), fog = biome.getFogColor();
        float tr = (((sky >> 16) & 255) * 0.5f + ((fog >> 16) & 255) * 0.5f) / 255f * DIM;
        float tg = (((sky >> 8) & 255) * 0.5f + ((fog >> 8) & 255) * 0.5f) / 255f * DIM;
        float tb = ((sky & 255) * 0.5f + (fog & 255) * 0.5f) / 255f * DIM;
        if (r < 0) { r = tr; g = tg; b = tb; }
        float k = 0.02f; // ease across biome borders (about 2 s at 60 fps)
        r += (tr - r) * k; g += (tg - g) * k; b += (tb - b) * k;
        e.setRed(r); e.setGreen(g); e.setBlue(b);
    }

    /** Current eased sky colour as 0xRRGGBB, or -1 outside the Rift (smoke test readout). */
    public static int current() { return r < 0 ? -1 : ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255); }
}
