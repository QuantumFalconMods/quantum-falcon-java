package com.matt.quantumfalcon.client.cutscene;

import com.matt.quantumfalcon.QuantumFalcon;
import com.matt.quantumfalcon.network.CutscenePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client side of boss cutscenes: a short cinematic camera with letterbox bars. The camera is a client-only invisible
 * entity that orbits (intro), pushes in (phase shift) or pulls back and rises (death) around the boss; the HUD and hand
 * are hidden, movement keys are ignored, and holding sneak skips. The player's own view is restored afterwards.
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class CutsceneClient {
    private CutsceneClient() {}

    private static CutscenePayload active;
    private static int age;
    private static ArmorStand cam;
    private static Entity previous;
    private static float startYaw;
    private static boolean skipHint;
    /** Last finished/skipped cutscene, for the smoke tests: "kind ticksPlayed skipped". */
    public static String lastResult = "";

    public static boolean active() { return active != null; }

    public static void play(CutscenePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (active != null) stop(false);
        active = p; age = 0; skipHint = true;
        cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        cam.setInvisible(true); cam.setNoGravity(true);
        previous = mc.getCameraEntity();
        Vec3 d = mc.player.position().subtract(p.x(), p.y(), p.z());
        startYaw = (float) Math.toDegrees(Math.atan2(d.z, d.x));
        place(0f);
        cam.setOldPosAndRot();
        mc.setCameraEntity(cam);
        startAudio(mc, p);
    }

    /** The audio pack's two cutscene layers (boss_cutscene_<id>_<beat> and boss_voice_<id>_<beat>), both from t=0 at the boss. */
    private static final java.util.List<CutsceneSound> LAYERS = new java.util.ArrayList<>();
    public static String lastAudio = "";

    private static void startAudio(Minecraft mc, CutscenePayload p) {
        LAYERS.clear();
        Entity boss = mc.level.getEntity(p.entityId());
        String id = boss == null ? null : bossSoundId(boss.getType());
        if (id == null) { lastAudio = "none"; return; }
        String beat = p.kind() == CutscenePayload.INTRO ? "intro" : p.kind() == CutscenePayload.PHASE ? "phase" : "death";
        for (String layer : new String[]{"boss_cutscene_", "boss_voice_"}) {
            var ev = com.matt.quantumfalcon.registry.ModSounds.byId(layer + id + "_" + beat);
            if (ev == null) continue;
            CutsceneSound s = new CutsceneSound(ev, p.x(), p.y() + p.height() / 2, p.z(), layer.equals("boss_voice_") ? 1.4f : 1.5f);
            LAYERS.add(s);
            mc.getSoundManager().play(s);
        }
        lastAudio = id + "_" + beat + " x" + LAYERS.size();
        QuantumFalcon.LOGGER.info("[cutscene] audio {}", lastAudio);
    }

    /** quantumfalcon:quantum_overseer -> overseer, rift_warden -> warden, ... (null for anything that is not a boss). */
    static String bossSoundId(EntityType<?> type) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key == null || !QuantumFalcon.MOD_ID.equals(key.getNamespace())) return null;
        return switch (key.getPath()) {
            case "quantum_overseer" -> "overseer";
            case "rift_warden" -> "warden";
            case "entangled_twins" -> "twins";
            case "singularity_sovereign" -> "sovereign";
            case "eclipse_phantom" -> "eclipse";
            case "void_lord" -> "void_lord";
            case "sha_sha" -> "sha_sha";
            default -> null;
        };
    }

    static void stop(boolean skipped) {
        Minecraft mc = Minecraft.getInstance();
        if (active != null) lastResult = active.kind() + " " + age + " " + skipped;
        if (skipped) LAYERS.forEach(CutsceneSound::fadeOut);   // a skip fades the cutscene audio; a natural end lets the tails ring out
        LAYERS.clear();
        active = null; cam = null;
        if (mc.player != null) mc.setCameraEntity(previous != null && previous.isAlive() ? previous : mc.player);
        previous = null;
    }

    /** Focus point: the live boss if the client still has it, else the position sent with the packet. */
    static Vec3 focus() {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(active.entityId());
        double h = active.height();
        if (e != null && e.isAlive()) return e.position().add(0, h * 0.6, 0);
        return new Vec3(active.x(), active.y() + h * 0.6, active.z());
    }

    static float ease(float t) { return t * t * (3 - 2 * t); }

    static void place(float partial) {
        float t = Mth.clamp((age + partial) / Math.max(1, active.ticks()), 0f, 1f), e = ease(t);
        Vec3 f = focus();
        double h = Math.max(2.0, active.height());
        double radius, height, yaw;
        switch (active.kind()) {
            case CutscenePayload.INTRO -> { radius = Mth.lerp(e, h * 2.6 + 6, h * 1.6 + 4); height = Mth.lerp(e, h * 1.2, h * 0.15); yaw = startYaw - 70 + 140 * e; }
            case CutscenePayload.PHASE -> { radius = Mth.lerp(e, h * 2.0 + 5, h * 1.1 + 3); height = h * 0.25; yaw = startYaw + 10 * e; }
            default -> { radius = Mth.lerp(e, h * 1.4 + 3, h * 2.8 + 9); height = Mth.lerp(e, h * 0.3, h * 2.2); yaw = startYaw + 60 * e; }
        }
        double a = Math.toRadians(yaw);
        Vec3 pos = new Vec3(f.x + Math.cos(a) * radius, f.y + height, f.z + Math.sin(a) * radius);
        Vec3 look = f.subtract(pos);
        float ry = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90.0);
        float rx = (float) -Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
        cam.setPos(pos.x, pos.y, pos.z);
        cam.setYRot(ry); cam.setXRot(rx); cam.setYHeadRot(ry);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (active == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || cam == null || cam.level() != mc.level) { stop(false); return; }
        if (mc.options.keyShift.isDown() && age > 5) {
            if (active.kind() != CutscenePayload.DEATH)
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.matt.quantumfalcon.network.CutsceneSkipPayload(active.entityId()));
            stop(true); return;
        }
        cam.setOldPosAndRot();
        cam.yRotO = cam.getYRot(); cam.xRotO = cam.getXRot();
        age++;
        if (age >= active.ticks()) { stop(false); return; }
        float prevY = cam.getYRot(), prevX = cam.getXRot();
        place(0f);
        cam.yRotO = prevY; cam.xRotO = prevX;
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (active == null) return;
        var in = e.getInput();
        in.forwardImpulse = 0; in.leftImpulse = 0; in.up = in.down = in.left = in.right = in.jumping = false;
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) { if (active != null) e.setCanceled(true); }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        // keep titles and chat (boss credit, quest lines) visible; hide the rest of the HUD
        if (active != null && !e.getName().equals(VanillaGuiLayers.TITLE) && !e.getName().equals(VanillaGuiLayers.CHAT)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post e) {
        if (active == null) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        float t = (age + e.getPartialTick().getGameTimeDeltaPartialTick(false)) / Math.max(1, active.ticks());
        // bars slide in over the first 8 ticks and out over the last 8
        float in = Mth.clamp(Math.min(t * active.ticks() / 8f, (1 - t) * active.ticks() / 8f), 0f, 1f);
        int bar = (int) (h * 0.12f * ease(in));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        if (skipHint && active.kind() != CutscenePayload.DEATH && in > 0.9f) {
            Minecraft mc = Minecraft.getInstance();
            Component hint = Component.translatable("cutscene.quantumfalcon.skip", mc.options.keyShift.getTranslatedKeyMessage());
            g.drawString(mc.font, hint, w - mc.font.width(hint) - 8, h - bar + (bar - 8) / 2, 0xFF9AA4B8, false);
        }
    }
}
