package com.matt.quantumfalcon.client.renderer;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.entity.boss.EntangledTwinsEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Entangled Twins v3 (rebuilt from scratch, tools/gen_twins.py): ~7-block void-metal guardians, cyan and magenta,
 * the magenta one a true mirror image (its own mirrored geo + animation). The quantum tether is drawn geometry:
 * a violet beam with a white-hot core and two helix strands (cyan + magenta) twisting between the twins' cores.
 */
public class EntangledTwinsRenderer extends GeoEntityRenderer<EntangledTwinsEntity> {
    public static final float SCALE = 2.0f;
    /** core height in model units (gen_twins core at y=40) */
    static final double CORE_Y = 40 / 16.0 * SCALE;

    public EntangledTwinsRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 1.6f;
        this.scaleWidth = SCALE; this.scaleHeight = SCALE;
        addRenderLayer(new PulsingGlowLayer<>(this));
    }

    static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, p); }

    public static class Model extends GeoModel<EntangledTwinsEntity> {
        static final ResourceLocation GEO_C = rl("geo/entity/entangled_twins.geo.json"), GEO_M = rl("geo/entity/entangled_twins_magenta.geo.json");
        static final ResourceLocation AN_C = rl("animations/entity/entangled_twins.animation.json"), AN_M = rl("animations/entity/entangled_twins_magenta.animation.json");
        static final ResourceLocation TEX_C = rl("textures/entity/boss/entangled_twins_cyan.png"), TEX_M = rl("textures/entity/boss/entangled_twins_magenta.png");
        @Override public ResourceLocation getModelResource(EntangledTwinsEntity e) { return e.isCyanTwin() ? GEO_C : GEO_M; }
        @Override public ResourceLocation getTextureResource(EntangledTwinsEntity e) { return e.isCyanTwin() ? TEX_C : TEX_M; }
        @Override public ResourceLocation getAnimationResource(EntangledTwinsEntity e) { return e.isCyanTwin() ? AN_C : AN_M; }
    }

    @Override protected float getDeathMaxRotation(EntangledTwinsEntity e) { return 0f; }

    @Override
    public boolean shouldRender(EntangledTwinsEntity e, Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(e, frustum, camX, camY, camZ)) return true;
        EntangledTwinsEntity t = e.isCyanTwin() ? e.clientTwin() : null;
        return t != null && frustum.isVisible(e.getBoundingBox().minmax(t.getBoundingBox()));
    }

    @Override
    public void render(EntangledTwinsEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(e, yaw, partialTick, pose, buffers, light);
        if (!e.isCyanTwin() || !e.isAlive()) return;
        EntangledTwinsEntity t = e.clientTwin();
        if (t == null || !t.isAlive()) return;
        Vec3 me = new Vec3(Mth.lerp(partialTick, e.xo, e.getX()), Mth.lerp(partialTick, e.yo, e.getY()), Mth.lerp(partialTick, e.zo, e.getZ()));
        Vec3 other = new Vec3(Mth.lerp(partialTick, t.xo, t.getX()), Mth.lerp(partialTick, t.yo, t.getY()), Mth.lerp(partialTick, t.zo, t.getZ()));
        Vec3 a = new Vec3(0, CORE_Y, 0), b = other.subtract(me).add(0, CORE_Y, 0);
        if (b.subtract(a).length() < 0.5 || b.subtract(a).length() > 48) return;
        drawTether(pose, buffers, me, a, b, e.tickCount + partialTick);
    }

    /** a..b in entity-local render space; me = entity world position (for camera-facing ribbons) */
    static void drawTether(PoseStack pose, MultiBufferSource buffers, Vec3 me, Vec3 a, Vec3 b, float t) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 d = b.subtract(a); double len = d.length(); Vec3 dir = d.normalize();
        Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(up).normalize(), v = dir.cross(u).normalize();
        int segs = Math.max(8, (int) (len * 3));
        float pulse = 0.75f + 0.25f * Mth.sin(t * 0.3f);
        // outer glow, inner core, two helix strands
        ribbon(vc, m, me, cam, a, dir, u, v, len, segs, t, 0.0, 0.42f * pulse, 0x6A2CFF, 0.30f, 0.10);
        ribbon(vc, m, me, cam, a, dir, u, v, len, segs, t, 0.0, 0.11f, 0xF2E6FF, 0.95f, 0.04);
        for (int k = 0; k < 2; k++)
            helix(vc, m, me, cam, a, dir, u, v, len, segs * 2, t, k * Math.PI, k == 0 ? 0x28ECFF : 0xFF38D6);
    }

    private static Vec3 wob(Vec3 p0, Vec3 u, Vec3 v, double s, float t, double amp) {
        double w = Math.sin(s * 2.1 + t * 0.35) * amp, w2 = Math.cos(s * 1.7 - t * 0.27) * amp;
        return p0.add(u.scale(w)).add(v.scale(w2));
    }

    private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 me, Vec3 cam, Vec3 a, Vec3 dir, Vec3 u, Vec3 v, double len, int segs, float t,
                               double phase, float width, int rgb, float alpha, double amp) {
        Vec3 prevL = null, prevR = null;
        for (int i = 0; i <= segs; i++) {
            double s = len * i / segs;
            double taper = Math.sin(Math.PI * i / segs) * 0.6 + 0.4;
            Vec3 p = wob(a.add(dir.scale(s)), u, v, s, t, amp * Math.sin(Math.PI * i / segs));
            Vec3 view = me.add(p).subtract(cam);
            Vec3 side = dir.cross(view).normalize().scale(width * taper * 0.5);
            Vec3 L = p.add(side), R = p.subtract(side);
            if (prevL != null) quad(vc, m, prevL, prevR, R, L, rgb, alpha);
            prevL = L; prevR = R;
        }
    }

    private static void helix(VertexConsumer vc, Matrix4f m, Vec3 me, Vec3 cam, Vec3 a, Vec3 dir, Vec3 u, Vec3 v, double len, int segs, float t,
                              double phase, int rgb) {
        Vec3 prevL = null, prevR = null;
        for (int i = 0; i <= segs; i++) {
            double s = len * i / segs, ang = s * 2.4 - t * 0.25 + phase;
            double r = 0.26 * (Math.sin(Math.PI * i / segs) * 0.7 + 0.3);
            Vec3 p = a.add(dir.scale(s)).add(u.scale(Math.cos(ang) * r)).add(v.scale(Math.sin(ang) * r));
            Vec3 view = me.add(p).subtract(cam);
            Vec3 side = dir.cross(view).normalize().scale(0.035);
            Vec3 L = p.add(side), R = p.subtract(side);
            if (prevL != null) quad(vc, m, prevL, prevR, R, L, rgb, 0.85f);
            prevL = L; prevR = R;
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int rgb, float alpha) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, al = (int) (alpha * 255);
        for (Vec3 p : new Vec3[]{p1, p2, p3, p4, p4, p3, p2, p1})   // both windings: visible from either side
            vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, al);
    }
}
