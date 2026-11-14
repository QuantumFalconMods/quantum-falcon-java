package com.matt.quantumfalcon.client.dev;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Developer-only client smoke test. Inactive unless the JVM is started with
 * -Dquantumfalcon.smoke=path/to/script.txt. Script lines (run once the player is in a world):
 *   CMD <command without slash>   run a chat command
 *   WAIT <ticks>                  wait
 *   SHOT <name>                   save screenshots/<name>.png
 *   SHOTQ <name>                  same, but only 1 tick before the next line (for frame sequences / motion clips)
 *   HUD off|on                    hide/show the HUD and chat (F1)
 *   INV open|close                open/close the survival inventory screen
 *   SLOT <0-8>                    select a hotbar slot
 *   VIEW first|back|front         camera perspective
 *   USEMOB                        right-click the nearest mod creature in reach (feed / tame)
 *   QUIT                          close the game
 */
@EventBusSubscriber(modid = QuantumFalcon.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static net.minecraft.core.BlockPos lastCentre;
    private static volatile boolean serverBusy;
    private static volatile double[] pendingMove;
    private static String waitBiome; private static int waitBiomeLeft;
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lastLevel;
    private static final String SCRIPT = System.getProperty("quantumfalcon.smoke");
    private static List<String> lines;
    private static int index;
    private static int wait = 100; // let chunks load first
    private static float panYaw, panPitch, dolly;   // per-tick camera motion for PAN (spectator walkthroughs)
    private static int panTicks;

    private static int attackLeft, attackCd;
    /** ATTACKBOSS n dodge -> play like a survival player: step out of telegraphs, eat when hungry, hit and back off. */
    private static boolean dodge, noFly;
    private static double attackFloorY;
    private static boolean floorPinned;   // ATTACKFLOOR here: keep the floor (and its guard) for the whole fight, between ATTACKBOSS bursts too
    private static boolean soundListening, soundRecording;
    private static final java.util.Set<String> PLAYED = java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());   // never follow a boss below where the fight started (the Sovereign dips into its void well)
    private static java.util.ArrayDeque<net.minecraft.core.BlockPos> pylons;

    private static void lookAt(Minecraft mc, net.minecraft.world.phys.Vec3 aim) {
        var eye = mc.player.getEyePosition();
        double ax = aim.x - eye.x, ay = aim.y - eye.y, az = aim.z - eye.z;
        mc.player.setYRot((float) (Math.toDegrees(Math.atan2(az, ax)) - 90));
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(ay, Math.sqrt(ax * ax + az * az))));
    }
    private static final java.util.Set<net.minecraft.world.entity.EntityType<?>> NO_RENDERER = new java.util.HashSet<>();
    private static String relogDir; private static int relogWait;

    private ClientSmokeTest() {}

    private static void mouseTo(Minecraft mc, double guiX, double guiY) {
        double gs = mc.getWindow().getGuiScale(), px = guiX * gs, py = guiY * gs;
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), px, py);
        try { var f = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); f.setAccessible(true); f.setDouble(mc.mouseHandler, px);
              var g = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos"); g.setAccessible(true); g.setDouble(mc.mouseHandler, py); } catch (Exception ignored) { }
    }

    /** Title-screen side of RELOG: once the old world is gone, open it again. */
    @SubscribeEvent
    public static void onTitleTick(ClientTickEvent.Post event) {
        if (relogDir == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null || mc.getSingleplayerServer() != null) return;
        if (--relogWait > 0) return;
        String d = relogDir; relogDir = null;
        QuantumFalcon.LOGGER.info("[smoke] RELOG reopening {}", d);
        mc.createWorldOpenFlows().openWorld(d, () -> mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
        wait = 200;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (SCRIPT == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (lines == null) {
            try {
                lines = new ArrayList<>(Files.readAllLines(Path.of(SCRIPT)));
            } catch (Exception e) {
                QuantumFalcon.LOGGER.error("[smoke] cannot read {}", SCRIPT, e);
                lines = List.of("QUIT");
            }
        }
        for (var e : mc.level.entitiesForRendering())   // dev: report any entity the client cannot render (it would crash the frame)
            if (mc.getEntityRenderDispatcher().getRenderer(e) == null && NO_RENDERER.add(e.getType()))
                QuantumFalcon.LOGGER.error("[smoke] NO RENDERER for {} ({}) at {}", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()), e.getClass().getName(), e.blockPosition());
        if (orbitTicks > 0) tickOrbit(mc);
        if ((attackLeft > 0 || floorPinned) && mc.player.getY() < attackFloorY - 3 && mc.getSingleplayerServer() != null) {   // every tick: a pull or knockback over an arena pit must not drop us out of the world
                var srv0 = mc.getSingleplayerServer(); var id0 = mc.player.getUUID(); double fy = attackFloorY + 1;
                srv0.execute(() -> { var sp = srv0.getPlayerList().getPlayer(id0); if (sp == null || sp.getY() >= fy - 4) return;
                    sp.teleportTo(sp.getX(), fy, sp.getZ()); sp.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); sp.fallDistance = 0;
                    sp.getAbilities().mayfly = true; sp.getAbilities().flying = true; sp.onUpdateAbilities(); });
        }
        if (attackLeft > 0 && dodge && mc.getSingleplayerServer() != null) {   // every tick, before the swing cooldown
            var srvD = mc.getSingleplayerServer(); var idD = mc.player.getUUID();
            srvD.execute(() -> {
                var sp = srvD.getPlayerList().getPlayer(idD);
                if (sp == null || !sp.isAlive()) return;
                var lvl = sp.serverLevel();
                // eat like a player: cooked beef from the inventory when the hunger bar drops
                if (sp.getFoodData().getFoodLevel() <= 16) {
                    int slot = sp.getInventory().findSlotMatchingItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF));
                    if (slot >= 0) { sp.getInventory().getItem(slot).shrink(1); sp.getFoodData().eat(8, 0.8f); }
                }
                for (var e : lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(40), m -> m instanceof com.matt.quantumfalcon.entity.boss.QFBoss && m.isAlive())) {
                    var tr = com.matt.quantumfalcon.entity.boss.BossDirector.lastTelegraph(e);
                    if (tr == null || tr.tele() == null || lvl.getGameTime() - tr.at() > tr.ticks() + 2 || !tr.tele().contains(sp.position())) continue;
                    // step out of the marked zone, the way the moveset dodge test does (same height, solid floor, within 24 of the boss)
                    for (double r = 3; r <= 14; r += 1.5) for (int k = 0; k < 16; k++) {
                        double a = k * Math.PI / 8;
                        var q = new net.minecraft.world.phys.Vec3(sp.getX() + Math.cos(a) * r, sp.getY(), sp.getZ() + Math.sin(a) * r);
                        if (tr.tele().contains(q) || q.distanceTo(e.position()) > 24) continue;
                        if (!lvl.getBlockState(net.minecraft.core.BlockPos.containing(q).below()).isSolid()) continue;
                        sp.teleportTo(q.x, q.y, q.z);
                        QuantumFalcon.LOGGER.info("[smoke] DODGE {} ({}t) -> {}", tr.move(), tr.ticks(), net.minecraft.core.BlockPos.containing(q).toShortString());
                        return;
                    }
                }
            });
        }
        if (attackLeft > 0 && !mc.player.isAlive()) {   // survival runs: dying ends the attack burst (the script RESPAWNs)
            QuantumFalcon.LOGGER.info("[smoke] ATTACKBOSS: player died with {} swings left", attackLeft); attackLeft = 0; return;
        }
        if (attackLeft > 0) {
            if (attackCd-- > 0) return;
            var srv = mc.getSingleplayerServer(); var id = mc.player.getUUID();
            if (srv != null) srv.execute(() -> { var sp = srv.getPlayerList().getPlayer(id);   // creative flight: a step over an arena pit must not drop the player out of the world
                if (sp != null && !noFly && !sp.getAbilities().flying) { sp.getAbilities().mayfly = true; sp.getAbilities().flying = true; sp.onUpdateAbilities(); } });   // survival too: a boss ignores (and heals against) a creative player
            if (pylons == null) {   // like a player would: shatter the arena's charged shield pylons first
                pylons = new java.util.ArrayDeque<>();
                var me = mc.player.blockPosition();
                for (var q : net.minecraft.core.BlockPos.betweenClosed(me.offset(-48, -24, -48), me.offset(48, 24, 48)))
                    if (mc.level.getBlockState(q).getBlock() instanceof com.matt.quantumfalcon.arena.fight.ArenaPylonBlock
                            && mc.level.getBlockState(q).getValue(com.matt.quantumfalcon.arena.fight.ArenaPylonBlock.CHARGED)) pylons.add(q.immutable());
                QuantumFalcon.LOGGER.info("[smoke] ATTACKBOSS: {} charged pylons", pylons.size());
            }
            if (!pylons.isEmpty()) {
                var q = pylons.peek();
                if (mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(q)) > 9) {   // step next to it, hit it next time
                    double tx = q.getX() + 2.5, ty = q.getY(), tz = q.getZ() + 0.5;
                    if (srv != null) srv.execute(() -> { var sp = srv.getPlayerList().getPlayer(id); if (sp != null) sp.teleportTo(tx, ty, tz); });
                    attackCd = 4; return;
                }
                lookAt(mc, net.minecraft.world.phys.Vec3.atCenterOf(q));
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                // break it through the server's player game mode (the real break path, playerWillDestroy included): a creative
                // player holding a sword cannot start a block break client-side, so pylons were never shattered before
                if (srv != null) srv.execute(() -> { var sp = srv.getPlayerList().getPlayer(id);
                    if (sp == null) return;
                    if (!sp.gameMode.destroyBlock(q)) {   // creative + sword refuses block breaks: run the block's own break hook, then remove it
                        var lvl = sp.serverLevel(); var bs = lvl.getBlockState(q);
                        bs.getBlock().playerWillDestroy(lvl, q, bs, sp); lvl.removeBlock(q, false);
                    } });
                pylons.poll(); attackCd = 6; return;
            }
            var b = nearestBoss(mc);
            if (b instanceof com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity && srv != null) {   // its Crystal Shield: walk into a crystal like a player (the touch shatters it)
                var sl0 = srv.getLevel(mc.level.dimension());
                if (sl0 != null && sl0.getEntity(b.getId()) instanceof com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity sb && sb.isShielded() && !sb.shieldCrystals().isEmpty()) {
                    var c = sb.shieldCrystals().get(0);
                    srv.execute(() -> { var sp = srv.getPlayerList().getPlayer(id); if (sp != null) sp.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5); });
                    QuantumFalcon.LOGGER.info("[smoke] ATTACKBOSS: touching shield crystal {}", c);
                    attackLeft--; attackCd = 6; return;
                }
            }
            if (b instanceof com.matt.quantumfalcon.entity.boss.SingularitySovereignEntity && srv != null) {
                // from phase 2 the arena centre under the Sovereign is an active singularity (instant void death inside its radius),
                // so a player fights from the ring: hold 12 blocks out and let the staged (player-attributed) blows land
                var bp = b.position(); var me = mc.player.position();
                double dx = me.x - bp.x, dz = me.z - bp.z, len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                if (len < 11) { double tx = bp.x + dx / len * 12, tz = bp.z + dz / len * 12, ty = attackFloorY + 2;
                    srv.execute(() -> { var sp = srv.getPlayerList().getPlayer(id); if (sp != null) sp.teleportTo(tx, ty, tz); }); }
                lookAt(mc, b.getBoundingBox().getCenter());
                attackLeft--; attackCd = 6; if (attackLeft == 0) pylons = null; return;
            }
            if (b == null) { attackLeft = 0; pylons = null; QuantumFalcon.LOGGER.info("[smoke] ATTACKBOSS: no boss left"); }
            else {
                var bp = b.position(); var me = mc.player.position();
                double dx = me.x - bp.x, dz = me.z - bp.z, len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                double reach = b.getBbWidth() / 2 + 2.2;
                if ((len > reach + 0.8 || Math.abs(me.y - Math.max(bp.y, attackFloorY)) > 3) && srv != null) {   // step into melee reach first (a staged move); swing on a later tick once the server has us there
                    double tx = bp.x + dx / len * reach, tz = bp.z + dz / len * reach, ty = Math.max(bp.y, attackFloorY);
                    srv.execute(() -> { var sp = srv.getPlayerList().getPlayer(id); if (sp != null) sp.teleportTo(tx, ty, tz); });
                    attackLeft--; attackCd = 3; return;   // moves use up the budget too, so an unreachable boss cannot stall the script
                }
                lookAt(mc, b.getBoundingBox().getCenter());
                mc.gameMode.attack(mc.player, b);
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                attackLeft--; attackCd = 12;
                if (dodge && srv != null) {   // hit and back off out of its reach until the sword has recharged
                    double bx = bp.x + dx / len * (reach + 3.5), bz = bp.z + dz / len * (reach + 3.5), by = Math.max(bp.y, attackFloorY);
                    srv.execute(() -> { var sp = srv.getPlayerList().getPlayer(id); if (sp != null && sp.serverLevel().getBlockState(net.minecraft.core.BlockPos.containing(bx, by, bz).below()).isSolid()) sp.teleportTo(bx, by, bz); });
                }
                if (attackLeft == 0) pylons = null;
            }
            if (attackLeft > 0) return;
        }
        if (panTicks > 0) {
            panTicks--;
            var pl = mc.player;
            pl.setYRot(pl.getYRot() + panYaw); pl.setXRot(pl.getXRot() + panPitch);
            pl.yRotO = pl.getYRot(); pl.xRotO = pl.getXRot();
            if (dolly != 0) {
                var look = net.minecraft.world.phys.Vec3.directionFromRotation(0, pl.getYRot());
                pl.setPos(pl.position().add(look.scale(dolly)));
            }
        }
        if (!serverBusy && pendingMove != null && mc.player != null) {   // hold the camera where LOOKFROM put it, every tick, until the next move
            double[] m = pendingMove;
            mc.player.getAbilities().flying = true;
            mc.player.moveTo(m[0], m[1], m[2], (float) m[3], (float) m[4]);
            mc.player.yRotO = (float) m[3]; mc.player.xRotO = (float) m[4]; mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (serverBusy) return;   // GOTO/AT/LOOKFROM run on the server thread; hold the script until they finish
        if (waitBiome != null && mc.player != null && mc.level != null) {   // WAITBIOME: the client has the chunk, the biome at the camera and the terrain drawn
            var bp = mc.player.blockPosition();
            boolean loaded = mc.level.getChunkSource().hasChunk(bp.getX() >> 4, bp.getZ() >> 4);
            String here = mc.level.getBiome(bp).unwrapKey().map(k -> k.location().toString()).orElse("?");
            int sections = mc.levelRenderer.countRenderedSections();
            if ((loaded && here.equals(waitBiome) && sections >= 120) || --waitBiomeLeft <= 0) {
                QuantumFalcon.LOGGER.info("[smoke] WAITBIOME {} -> {} (client biome {}, chunk loaded {}, sections {})", waitBiome,
                        here.equals(waitBiome) && loaded ? "OK" : "TIMEOUT", here, loaded, sections);
                waitBiome = null; wait = 40;   // let fog and sky colour blend in
            }
            return;
        }
        if (wait > 0) { wait--; return; }
        while (index < lines.size()) {
            String line = lines.get(index++).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            QuantumFalcon.LOGGER.info("[smoke] {}", line);
            if (line.equals("OPME")) {
                // dev-only: a freshly created quick-play world has cheats off; op the local player on the integrated server
                var srv = mc.getSingleplayerServer();
                if (srv != null) { var prof = mc.player.getGameProfile(); srv.execute(() -> { setAllowCommands(srv, true); srv.getPlayerList().op(prof); }); }
                wait = 20; return;
            } else if (line.startsWith("CMD ")) {
                mc.player.connection.sendCommand(line.substring(4));
            } else if (line.startsWith("WAITBIOME ")) {   // WAITBIOME <biome> [maxTicks]: hold until the client is in that biome with its chunk loaded and drawn
                String[] a = line.substring(10).trim().split("\\s+");
                waitBiome = a[0]; waitBiomeLeft = a.length > 1 ? Integer.parseInt(a[1]) : 900;
                return;
            } else if (line.startsWith("WAIT ")) {
                wait = Integer.parseInt(line.substring(5).trim());
                return;
            } else if (line.startsWith("SHOT ") || line.startsWith("SHOTQ ")) {
                boolean quick = line.startsWith("SHOTQ ");
                String name = line.substring(quick ? 6 : 5).trim() + ".png";
                var cam = mc.gameRenderer.getMainCamera();
                QuantumFalcon.LOGGER.info("[smoke] shot {} client player {} camera {} rot {}/{} sections {} dim {} mode {}", name, mc.player.blockPosition(),
                        net.minecraft.core.BlockPos.containing(cam.getPosition()), (int) cam.getYRot(), (int) cam.getXRot(), mc.levelRenderer.countRenderedSections(), mc.level.dimension().location(), mc.gameMode.getPlayerMode());
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                        msg -> QuantumFalcon.LOGGER.info("[smoke] screenshot {}: {}", name, msg.getString()));
                wait = quick ? 1 : 5;
                return;
            } else if (line.equals("HUD off") || line.equals("HUD on")) {
                mc.options.hideGui = line.endsWith("off");
            } else if (line.startsWith("INV ")) {
                mc.getToasts().clear();
                mc.setScreen(line.endsWith("open") ? new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player) : null);
                if (line.endsWith("open")) {   // move the cursor off the slots so no tooltip covers the shot
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 2, 2);
                    try { var f = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); f.setAccessible(true); f.setDouble(mc.mouseHandler, 2);
                          var g = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos"); g.setAccessible(true); g.setDouble(mc.mouseHandler, 2); } catch (Exception ignored) { }
                }
            } else if (line.startsWith("SKILLTREE ")) {   // SKILLTREE <CATEGORY> [skillId]: open the skill tree on a tab, hover a node
                String[] a = line.substring(10).trim().split("\\s+");
                mc.getToasts().clear();
                var scr = new com.matt.quantumfalcon.client.gui.SkillTreeScreen();
                mc.setScreen(scr);
                scr.selectCategory(com.matt.quantumfalcon.skill.SkillTreeSystem.Category.valueOf(a[0]));
                if (a.length > 1) scr.focusSkill(a[1]);
                int[] c = a.length > 1 ? scr.nodeCenter(a[1]) : null;
                double gs = mc.getWindow().getGuiScale();
                double px = c != null ? c[0] * gs : 2, py = c != null ? c[1] * gs : 2;
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), px, py);
                try { var f = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); f.setAccessible(true); f.setDouble(mc.mouseHandler, px);
                      var g = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos"); g.setAccessible(true); g.setDouble(mc.mouseHandler, py); } catch (Exception ignored) { }
            } else if (line.startsWith("QUESTLOG ")) {   // QUESTLOG <TAB> [questId]: open the J quest log on a tab with a quest selected
                String[] a = line.substring(9).trim().split("\\s+");
                mc.getToasts().clear();
                var scr = new com.matt.quantumfalcon.client.gui.QuestLogScreen();
                mc.setScreen(scr);
                scr.devSelect(com.matt.quantumfalcon.client.gui.QuestLogScreen.QuestTab.valueOf(a[0]), a.length > 1 ? a[1] : null);
            } else if (line.startsWith("STORYCHOICE ")) {   // STORYCHOICE <true|false> [relics bonds] [CONFIRM LIGHT|DARK|TRUE]: the ending choice screen
                String[] a = line.substring(12).trim().split("\\s+");
                boolean open = Boolean.parseBoolean(a[0]);
                int rel = a.length > 2 ? Integer.parseInt(a[1]) : (open ? 4 : 0), bon = a.length > 2 ? Integer.parseInt(a[2]) : (open ? 4 : 0);
                var scr = new com.matt.quantumfalcon.client.screen.StoryChoiceScreen(open, rel, bon);
                mc.setScreen(scr);
                for (int i = 0; i < a.length - 1; i++) if (a[i].equals("CONFIRM")) scr.devPick(a[i + 1]);
            } else if (line.equals("ORBIT stop")) {
                orbitTicks = 0; mc.setCameraEntity(mc.player); mc.gameRenderer.setRenderHand(true);
            } else if (line.startsWith("ORBIT ")) {   // ORBIT <radius> <height> <degrees> <ticks>: a fight camera circling the midpoint of the nearest boss and the player
                String[] a = line.substring(6).trim().split("\\s+");
                orbitR = Double.parseDouble(a[0]); orbitH = Double.parseDouble(a[1]); orbitDeg = Double.parseDouble(a[2]);
                orbitTicks = orbitTotal = Integer.parseInt(a[3]);
                orbitMob = a.length > 4 && a[4].equals("mob");
                orbitCam = new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, mc.level);
                orbitCam.setInvisible(true); orbitCam.setNoGravity(true);
                var b = orbitTarget(mc);
                var me = mc.player.position();
                orbitYaw0 = b == null ? mc.player.getYRot() : Math.toDegrees(Math.atan2(me.z - b.getZ(), me.x - b.getX())) + 35;
                tickOrbit(mc);
                orbitCam.setOldPosAndRot();
                mc.setCameraEntity(orbitCam);
                mc.gameRenderer.setRenderHand(false);   // the player's hand has no place in an outside camera
                return;
            } else if (line.startsWith("GUISCALE ")) {   // GUISCALE <n>: change the GUI scale (0 = auto) and re-layout
                mc.options.guiScale().set(Integer.parseInt(line.substring(9).trim()));
                mc.resizeDisplay();
                if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                wait = 5; return;
            } else if (line.startsWith("SOUNDLOG")) {   // SOUNDLOG start | SOUNDLOG dump <label>: record every sound the client plays (id), then log the quantumfalcon ones
                if (!soundListening) {
                    mc.getSoundManager().addListener((sound, accessor, range) -> { if (soundRecording) PLAYED.add(sound.getLocation().toString()); });
                    soundListening = true;
                }
                String[] a = line.split("\\s+");
                if (a.length > 1 && a[1].equals("start")) { PLAYED.clear(); soundRecording = true; }
                else {
                    var ours = new java.util.TreeSet<String>(); var vanilla = new java.util.TreeSet<String>();
                    for (String id : PLAYED) (id.startsWith("quantumfalcon:") ? ours : vanilla).add(id);
                    QuantumFalcon.LOGGER.info("[smoke] SOUNDLOG {} quantumfalcon={} {}", a.length > 2 ? a[2] : "", ours.size(), ours);
                    QuantumFalcon.LOGGER.info("[smoke] SOUNDLOG {} vanilla={} {}", a.length > 2 ? a[2] : "", vanilla.size(), vanilla);
                    PLAYED.clear();
                }
                wait = 1; return;
            } else if (line.equals("RESPAWN")) {   // leave the death screen if the saved player position killed us on load
                if (mc.player != null && !mc.player.isAlive()) { mc.player.respawn(); mc.setScreen(null); }
                wait = 40; return;
            } else if (line.equals("DEOP")) {   // back to a normal (permission 0) player on the integrated server
                var srv = mc.getSingleplayerServer();
                // The singleplayer host keeps permission 4 while the world allows cheats, so a plain deop changes nothing.
                // Turn the world's allowCommands off for the session too; OPME turns it back on.
                if (srv != null) { var prof = mc.player.getGameProfile(); srv.execute(() -> {
                    setAllowCommands(srv, false);
                    srv.getPlayerList().deop(prof);
                    var sp = srv.getPlayerList().getPlayer(prof.getId());
                    if (sp != null) srv.getPlayerList().sendPlayerPermissionLevel(sp);
                    QuantumFalcon.LOGGER.info("[smoke] DEOP: host permission level now {}", srv.getProfilePermissions(prof));
                }); }
                wait = 20; return;
            } else if (line.startsWith("GOTO ")) {   // GOTO <dimension> <structure> [near <biome>]: stand at the nearest structure of that type (arenas: on the floor centre)
                String[] a = line.substring(5).trim().split("\\s+");
                var srv = mc.getSingleplayerServer();
                var id = mc.player.getUUID();
                if (srv != null) { serverBusy = true; srv.execute(() -> { try {
                    var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(a[0]));
                    var lvl = srv.getLevel(key);
                    var sp = srv.getPlayerList().getPlayer(id);
                    if (lvl == null || sp == null) return;
                    var st = lvl.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                            .getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, net.minecraft.resources.ResourceLocation.parse(a[1])));
                    if (st.isEmpty()) return;
                    net.minecraft.core.BlockPos from = sp.level() == lvl ? sp.blockPosition() : new net.minecraft.core.BlockPos(sp.getBlockX(), 100, sp.getBlockZ());
                    if (a.length > 3 && a[2].equals("near")) {
                        var biome = net.minecraft.resources.ResourceLocation.parse(a[3]);
                        var b = lvl.findClosestBiome3d(h -> h.is(biome), from, 6400, 32, 64);
                        if (b != null) from = b.getFirst();
                        com.matt.quantumfalcon.QuantumFalcon.LOGGER.info("SMOKE GOTO biome {} at {}", biome, from);
                    }
                    var found = lvl.getChunkSource().getGenerator().findNearestMapStructure(lvl, net.minecraft.core.HolderSet.direct(st.get()), from, 100, false);
                    if (found == null) { com.matt.quantumfalcon.QuantumFalcon.LOGGER.info("SMOKE GOTO {} not found", a[1]); return; }
                    var p0 = found.getFirst();
                    var startChunk = lvl.getChunk(p0.getX() >> 4, p0.getZ() >> 4);
                    var start = startChunk.getStartForStructure(st.get().value());
                    if (start == null) start = net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START;
                    boolean arena = a[1].endsWith("_arena");
                    int y = start.isValid() ? (arena ? start.getBoundingBox().maxY() - com.matt.quantumfalcon.worldgen.structure.ArenaDesign.CLEAR_UP - 2 : start.getBoundingBox().maxY() + 2)
                            : lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, p0.getX(), p0.getZ()) + 2;
                    if (start.isValid()) { var bb = start.getBoundingBox(); p0 = new net.minecraft.core.BlockPos(bb.getCenter().getX(), y, bb.getCenter().getZ()); }
                    lastCentre = p0; lastLevel = key; pendingMove = null;
                    com.matt.quantumfalcon.QuantumFalcon.LOGGER.info("SMOKE GOTO {} at {} (valid start {}, biome {})", a[1], p0, start.isValid(),
                            lvl.getBiome(p0).unwrapKey().map(k -> k.location().toString()).orElse("?"));
                    // GOTO keeps the game mode (the audit shots walk in as survival/creative), so land on solid ground:
                    // arenas on the floor beside the pit, anything else on the top block; never above the void
                    double gx = p0.getX() + 0.5 + (arena ? ArenaDesignOffset.of(a[1]) : 0), gz = p0.getZ() + 0.5;
                    int gy = y + (arena ? 1 : 0);
                    var below = net.minecraft.core.BlockPos.containing(gx, gy - 1, gz);
                    if (lvl.getBlockState(below).isAir() && sp.gameMode.getGameModeForPlayer().isSurvival()) {
                        int top = lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, below.getX(), below.getZ());
                        if (top > lvl.getMinBuildHeight() + 1) gy = top; else sp.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                    }
                    sp.teleportTo(lvl, gx, gy, gz, java.util.Set.of(), sp.getYRot(), 30f);
                } finally { serverBusy = false; } }); }
                wait = 40; return;
            } else if (line.startsWith("AT ")) {   // AT <dimension> <x> <z>: make a surface point the centre for LOOKFROM
                String[] a = line.substring(3).trim().split("\\s+");
                var srv = mc.getSingleplayerServer();
                if (srv != null) { serverBusy = true; srv.execute(() -> { try {
                    var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(a[0]));
                    var lvl = srv.getLevel(key);
                    if (lvl == null) return;
                    int x = Integer.parseInt(a[1]), z = Integer.parseInt(a[2]);
                    lvl.getChunk(x >> 4, z >> 4);
                    lastCentre = new net.minecraft.core.BlockPos(x, lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
                    lastLevel = key;
                } finally { serverBusy = false; } }); }
                wait = 10; return;
            } else if (line.startsWith("SKY ")) {   // SKY <dimension> <biome> [pitch]: spectator on the open surface of the nearest such biome, looking at the horizon/sky
                String[] a = line.substring(4).trim().split("\\s+");
                var srv = mc.getSingleplayerServer();
                var id = mc.player.getUUID();
                if (srv != null) { serverBusy = true; srv.execute(() -> { try {
                    var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(a[0]));
                    var lvl = srv.getLevel(key);
                    var sp = srv.getPlayerList().getPlayer(id);
                    if (lvl == null || sp == null) return;
                    var biome = net.minecraft.resources.ResourceLocation.parse(a[1]);
                    // surface search: walk outward on a 2D spiral, and test the biome AT the generated surface (not anywhere in the column)
                    var gen = lvl.getChunkSource().getGenerator();
                    var rs = lvl.getChunkSource().randomState();
                    int ox = sp.level() == lvl ? sp.getBlockX() : 0, oz = sp.level() == lvl ? sp.getBlockZ() : 0;
                    int x = 0, z = 0, top = 0, tries = 0; boolean ok = false;
                    search:
                    for (int r = 0; r <= 4800; r += 64) for (int dx = -r; dx <= r; dx += 64) for (int dz = -r; dz <= r; dz += 64) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        int cx = ox + dx, cz = oz + dz;
                        int h = gen.getBaseHeight(cx, cz, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, lvl, rs);
                        if (h <= lvl.getMinBuildHeight() + 1 || h >= lvl.getMaxBuildHeight() - 8) continue;
                        var hb = gen.getBiomeSource().getNoiseBiome(cx >> 2, (h + 2) >> 2, cz >> 2, rs.sampler());
                        if (!hb.is(biome)) continue;
                        // the rift is 3D noise with overhangs: generate the chunk and insist on an open-sky top in the same biome
                        if (++tries > 150) break search;
                        lvl.getChunk(cx >> 4, cz >> 4);
                        int real = lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, cx, cz);
                        if (real <= lvl.getMinBuildHeight() + 1 || real >= lvl.getMaxBuildHeight() - 40) continue;
                        if (!lvl.getBiome(new net.minecraft.core.BlockPos(cx, real + 2, cz)).is(biome)) continue;
                        if (!lvl.getBiome(new net.minecraft.core.BlockPos(cx, real + 24, cz)).is(biome)) continue;   // 3D biomes: the camera's own biome sets the sky
                        x = cx; z = cz; top = real; ok = true; break search;
                    }
                    if (!ok) { QuantumFalcon.LOGGER.info("[smoke] SKY no open-sky {} after {} tries", biome, tries); return; }
                    float pitch = a.length > 2 ? Float.parseFloat(a[2]) : 8f;   // slightly down: terrain in the lower half, the biome sky above
                    // face the most open direction: the yaw whose terrain over the next 48 blocks rises least above the camera
                    float yaw = 0f; int bestRise = Integer.MAX_VALUE;
                    for (int k = 0; k < 8; k++) {
                        float yw = k * 45f;
                        double dx = -Math.sin(Math.toRadians(yw)), dz = Math.cos(Math.toRadians(yw));
                        int rise = Integer.MIN_VALUE;
                        for (int d = 8; d <= 48; d += 8)
                            rise = Math.max(rise, gen.getBaseHeight((int) (x + dx * d), (int) (z + dz * d), net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, lvl, rs) - (top + 24));
                        if (rise < bestRise) { bestRise = rise; yaw = yw; }
                    }
                    sp.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                    sp.teleportTo(lvl, x + 0.5, top + 24, z + 0.5, java.util.Set.of(), yaw, pitch);
                    pendingMove = new double[]{x + 0.5, top + 24, z + 0.5, yaw, pitch};
                    lastCentre = new net.minecraft.core.BlockPos(x, top, z); lastLevel = key;
                    QuantumFalcon.LOGGER.info("[smoke] SKY {} at {} {} {} biome-here {}", biome, x, top, z,
                            lvl.getBiome(new net.minecraft.core.BlockPos(x, top + 6, z)).unwrapKey().map(k -> k.location().toString()).orElse("?"));
                } finally { serverBusy = false; } }); }
                wait = 40; return;
            } else if (line.startsWith("PLACEARENA ")) {   // PLACEARENA <TYPE> <x> <z> | PLACEARENA <TYPE> near <biome>: build a natural arena with ArenaDesign
                String[] a = line.substring(11).trim().split("\\s+");
                var srv = mc.getSingleplayerServer();
                var id = mc.player.getUUID();
                if (srv != null) { serverBusy = true; srv.execute(() -> { try {
                    var sp = srv.getPlayerList().getPlayer(id);
                    var lvl = srv.overworld();
                    var type = com.matt.quantumfalcon.worldgen.structure.BossArenaStructure.ArenaType.valueOf(a[0]);
                    int x, z;
                    if (a[1].equals("near")) {
                        var biome = net.minecraft.resources.ResourceLocation.parse(a[2]);
                        var b = lvl.findClosestBiome3d(h -> h.is(biome), sp.blockPosition(), 6400, 32, 64);
                        if (b == null) { QuantumFalcon.LOGGER.info("SMOKE PLACEARENA no {}", biome); return; }
                        x = b.getFirst().getX(); z = b.getFirst().getZ();
                    } else { x = Integer.parseInt(a[1]); z = Integer.parseInt(a[2]); }
                    int reach = com.matt.quantumfalcon.worldgen.structure.ArenaDesign.reach(type), r = com.matt.quantumfalcon.worldgen.structure.ArenaDesign.floorRadius(type);
                    for (int cx = (x - reach - 48) >> 4; cx <= (x + reach + 48) >> 4; cx++) for (int cz = (z - reach - 48) >> 4; cz <= (z + reach + 48) >> 4; cz++) lvl.getChunk(cx, cz);
                    // same floor rule as BossArenaStructure: median surface height under the floor, plus one
                    java.util.List<Integer> hs = new java.util.ArrayList<>();
                    for (int dx = -r; dx <= r; dx += r / 3) for (int dz = -r; dz <= r; dz += r / 3)
                        if (dx * dx + dz * dz <= r * r) hs.add(lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x + dx, z + dz));
                    java.util.Collections.sort(hs);
                    var centre = new net.minecraft.core.BlockPos(x, hs.get(hs.size() / 2) + 1, z);
                    var box = new net.minecraft.world.level.levelgen.structure.BoundingBox(x - reach, centre.getY() - 60, z - reach, x + reach, centre.getY() + 60, z + reach);
                    com.matt.quantumfalcon.worldgen.structure.ArenaDesign.build(new com.matt.quantumfalcon.worldgen.structure.ArenaDesign.Sink() {
                        @Override public net.minecraft.world.level.block.state.BlockState get(net.minecraft.core.BlockPos q) { return lvl.getBlockState(q); }
                        @Override public void set(net.minecraft.core.BlockPos q, net.minecraft.world.level.block.state.BlockState st) { lvl.setBlock(q, st, 2); }
                    }, centre, type, box);
                    lastCentre = centre; lastLevel = lvl.dimension(); pendingMove = null;
                    QuantumFalcon.LOGGER.info("SMOKE PLACEARENA {} at {} biome {}", type, centre, lvl.getBiome(centre).unwrapKey().map(k -> k.location().toString()).orElse("?"));
                } finally { serverBusy = false; } }); }
                wait = 20; return;
            } else if (line.equals("PROBE")) {   // log what is actually around the last GOTO centre (floor, pylon ring, sky)
                var srv = mc.getSingleplayerServer();
                var c = lastCentre; var key = lastLevel;
                if (srv != null && c != null) srv.execute(() -> {
                    var lvl = srv.getLevel(key);
                    StringBuilder b = new StringBuilder();
                    for (int[] o : new int[][]{{0,0,0},{0,-1,0},{8,0,0},{12,0,12},{13,4,13},{-13,4,-13},{0,20,0},{0,45,0},{20,0,0},{26,0,0}}) {
                        var q = c.offset(o[0], o[1], o[2]);
                        b.append(java.util.Arrays.toString(o)).append('=').append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(lvl.getBlockState(q).getBlock()).getPath()).append(' ');
                    }
                    var pl = srv.getPlayerList().getPlayer(mc.player.getUUID());
                    if (pl != null) for (int dy = -12; dy <= 0; dy += 4) {
                        var at = pl.blockPosition().offset(0, dy, 0);
                        b.append(" | structures@").append(dy).append('=');
                        lvl.structureManager().getAllStructuresAt(at).keySet().forEach(st -> b.append(lvl.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(st)).append(','));
                        b.append(" block=").append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(lvl.getBlockState(at).getBlock()).getPath());
                    }
                    com.matt.quantumfalcon.QuantumFalcon.LOGGER.info("SMOKE PROBE centre {} player {} : {}", c, pl == null ? "?" : pl.blockPosition(), b);
                });
                wait = 10; return;
            } else if (line.startsWith("LOOKFROM ")) {   // LOOKFROM dx dy dz [lookDy]: stand offset from the last GOTO centre, facing it
                String[] a = line.substring(9).trim().split("\\s+");
                var srv = mc.getSingleplayerServer();
                var id = mc.player.getUUID();
                var c = lastCentre; var key = lastLevel;
                if (srv != null && c != null) { serverBusy = true; srv.execute(() -> { try {
                    var lvl = srv.getLevel(key);
                    var sp = srv.getPlayerList().getPlayer(id);
                    if (lvl == null || sp == null) return;
                    double x = c.getX() + 0.5 + Double.parseDouble(a[0]), y = c.getY() + Double.parseDouble(a[1]), z = c.getZ() + 0.5 + Double.parseDouble(a[2]);
                    double ty = c.getY() + (a.length > 3 ? Double.parseDouble(a[3]) : 1);
                    double ddx = c.getX() + 0.5 - x, ddz = c.getZ() + 0.5 - z, ddy = ty - (y + 1.62);
                    float yaw = (float) (Math.toDegrees(Math.atan2(ddz, ddx)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
                    // a camera move: always as a spectator, so a viewpoint over a pit or the void can never kill the player
                    if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SPECTATOR) sp.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                    sp.teleportTo(lvl, x, y, z, java.util.Set.of(), yaw, pitch);
                    pendingMove = new double[]{x, y, z, yaw, pitch};
                    com.matt.quantumfalcon.QuantumFalcon.LOGGER.info("SMOKE LOOKFROM at {} {} {} yaw {} pitch {}", (int) x, (int) y, (int) z, (int) yaw, (int) pitch);
                } finally { serverBusy = false; } }); }
                wait = 30; return;
            } else if (line.equals("RENDERERCHECK")) {   // every quantumfalcon entity type must have a client renderer (a missing one crashes the frame it appears in)
                var missing = new java.util.ArrayList<String>();
                for (var t : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                    var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t);
                    if (!key.getNamespace().equals(QuantumFalcon.MOD_ID)) continue;
                    var e = t.create(mc.level);
                    if (e == null) continue;
                    if (mc.getEntityRenderDispatcher().getRenderer(e) == null) missing.add(key.getPath());
                    e.discard();
                }
                QuantumFalcon.LOGGER.info("[smoke] RENDERERCHECK {} {}", missing.isEmpty() ? "PASS" : "FAIL missing", missing);
            } else if (line.startsWith("HOVERQUEST ")) {   // HOVERQUEST <questId>: put the mouse on a quest's entry in the open quest log (shows the full-name tooltip)
                if (mc.screen instanceof com.matt.quantumfalcon.client.gui.QuestLogScreen q) {
                    int[] c = q.devEntryCenter(line.substring(11).trim());
                    if (c == null) QuantumFalcon.LOGGER.info("[smoke] HOVERQUEST: {} not listed", line.substring(11).trim());
                    else mouseTo(mc, c[0], c[1]);
                }
            } else if (line.equals("ATTACKFLOOR here")) {   // pin the fight floor 2 below the player until ATTACKFLOOR off
                floorPinned = true; attackFloorY = mc.player.getY() - 2;
            } else if (line.equals("ATTACKFLOOR off")) {
                floorPinned = false;
            } else if (line.startsWith("ATTACKBOSS ")) {   // ATTACKBOSS <swings>: real melee swings at the nearest boss, one per 12 ticks, stepping into reach first
                String[] aa = line.substring(11).trim().split("\\s+");
                dodge = aa.length > 1 && aa[1].equals("dodge");
                noFly = java.util.Arrays.asList(aa).contains("nofly");   // honest survival: no harness flight over pits
                pylons = null; if (!floorPinned) attackFloorY = mc.player.getY() - 2; attackLeft = Integer.parseInt(aa[0]);
                wait = 1; return;
            } else if (line.startsWith("CRAFT ")) {   // CRAFT <9 item ids or ->: fill the open crafting table's grid with real menu clicks from the inventory
                String[] a = line.substring(6).trim().split("\\s+");
                if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen cs) {
                    var menu = cs.getMenu();
                    for (int g = 0; g < 9 && g < a.length; g++) {
                        if (a[g].equals("-")) continue;
                        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(a[g]));
                        int from = -1;
                        for (int sl = 10; sl < menu.slots.size(); sl++) if (menu.slots.get(sl).getItem().is(item)) { from = sl; break; }
                        if (from < 0) { QuantumFalcon.LOGGER.info("[smoke] CRAFT: no {} in inventory", a[g]); continue; }
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, from, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);    // pick the stack up
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, 1 + g, 1, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);  // right-click: drop one into the grid
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, from, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);    // put the rest back
                    }
                    mouseTo(mc, cs.width - 4, 4);
                } else QuantumFalcon.LOGGER.info("[smoke] CRAFT: no crafting screen open");
                wait = 10; return;
            } else if (line.equals("TAKE")) {   // shift-click the crafting result into the inventory
                if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs)
                    mc.gameMode.handleInventoryMouseClick(cs.getMenu().containerId, 0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
                wait = 10; return;
            } else if (line.equals("SCREENCLICK")) {   // left-click the open screen where the mouse is (dismisses cards)
                if (mc.screen != null) { double gs = mc.getWindow().getGuiScale(); double mx = mc.mouseHandler.xpos() / gs, my = mc.mouseHandler.ypos() / gs; mc.screen.mouseClicked(mx, my, 0); }
                wait = 5; return;
            } else if (line.startsWith("BUTTON ")) {   // BUTTON <label text>: click the open screen's button whose label contains the text
                String want = line.substring(7).trim().toLowerCase();
                if (mc.screen != null) for (var ch : mc.screen.children())
                    if (ch instanceof net.minecraft.client.gui.components.AbstractWidget w && w.visible && w.active && w.getMessage().getString().toLowerCase().contains(want)) {
                        double x = w.getX() + w.getWidth() / 2.0, y = w.getY() + w.getHeight() / 2.0;
                        mouseTo(mc, x, y);
                        var scr = mc.screen; scr.mouseClicked(x, y, 0); if (mc.screen == scr) scr.mouseReleased(x, y, 0);   // a button that closes its screen (Confirm) leaves no screen to release on
                        QuantumFalcon.LOGGER.info("[smoke] BUTTON clicked '{}'", w.getMessage().getString());
                        break;
                    }
                wait = 10; return;
            } else if (line.equals("RELOG")) {   // leave the world to the title screen and load it again (the server saves and unloads every chunk)
                String world = mc.getSingleplayerServer() == null ? null : mc.getSingleplayerServer().getWorldData().getLevelName();
                String dir = mc.getSingleplayerServer() == null ? null : mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
                QuantumFalcon.LOGGER.info("[smoke] RELOG {} ({})", world, dir);
                index++;
                mc.level.disconnect();
                mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
                relogDir = dir; relogWait = 60;
                return;
            } else if (line.equals("CHAT open")) {   // open the chat screen so the full recent history is visible
                mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
            } else if (line.equals("CHAT clear")) {
                mc.gui.getChat().clearMessages(false);
                mc.getToasts().clear();
            } else if (line.startsWith("SLOT ")) {
                mc.player.getInventory().selected = Integer.parseInt(line.substring(5).trim());
            } else if (line.startsWith("VIEW ")) {
                String v = line.substring(5).trim();
                mc.options.setCameraType(v.equals("first") ? net.minecraft.client.CameraType.FIRST_PERSON
                        : v.equals("back") ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            } else if (line.startsWith("USEBLOCK ")) {   // USEBLOCK dx dy dz: right-click the block at an offset from the player's feet (normal client use packet)
                String[] a = line.substring(9).trim().split("\\s+");
                var pos = mc.player.blockPosition().offset(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
                var c = net.minecraft.world.phys.Vec3.atCenterOf(pos);
                var face = net.minecraft.core.Direction.getNearest(mc.player.getX() - c.x, mc.player.getEyeY() - c.y, mc.player.getZ() - c.z);
                var eye = mc.player.getEyePosition();
                mc.player.setYRot((float) (Math.toDegrees(Math.atan2(c.z - eye.z, c.x - eye.x)) - 90));
                mc.player.setXRot((float) -Math.toDegrees(Math.atan2(c.y - eye.y, Math.hypot(c.x - eye.x, c.z - eye.z))));
                var r = mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(c.relative(face, 0.5), face, pos, false));
                QuantumFalcon.LOGGER.info("[smoke] USEBLOCK {} {} -> {}", pos, mc.level.getBlockState(pos).getBlock(), r);
                wait = 10; return;
            } else if (line.equals("USEMOB")) {   // right-click the nearest mod creature within 6 blocks with the held item (feeding, taming)
                var mobs = mc.level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, mc.player.getBoundingBox().inflate(6),
                        m -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getNamespace().equals(QuantumFalcon.MOD_ID));
                mobs.sort(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(mc.player)));
                if (!mobs.isEmpty()) {
                    var r = mc.gameMode.interact(mc.player, mobs.get(0), net.minecraft.world.InteractionHand.MAIN_HAND);
                    QuantumFalcon.LOGGER.info("[smoke] USEMOB {} -> {}", mobs.get(0).getType(), r);
                } else QuantumFalcon.LOGGER.info("[smoke] USEMOB: nothing in reach");
            } else if (line.equals("USE")) {      // right-click the block under the crosshair with the held item
                if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, bhr);
                } else QuantumFalcon.LOGGER.info("[smoke] USE: no block under crosshair");
            } else if (line.equals("KEYUSE on") || line.equals("KEYUSE off")) {   // hold / release right-click (bow draw)
                mc.options.keyUse.setDown(line.endsWith("on"));
            } else if (line.startsWith("PAN ")) {   // PAN <yaw degrees> <pitch degrees> <forward blocks> <ticks>: smooth camera move
                String[] a = line.substring(4).trim().split("\\s+");
                panTicks = Integer.parseInt(a[3]);
                panYaw = Float.parseFloat(a[0]) / panTicks; panPitch = Float.parseFloat(a[1]) / panTicks; dolly = Float.parseFloat(a[2]) / panTicks;
                wait = panTicks;
                return;
            } else if (line.startsWith("GAMMA ")) {   // brightness option 0.0 (moody) .. 1.0 (bright)
                mc.options.gamma().set(Double.parseDouble(line.substring(6).trim()));
            } else if (line.startsWith("FOV ")) {   // camera field of view in degrees (30..110)
                mc.options.fov().set(Integer.parseInt(line.substring(4).trim()));
            } else if (line.equals("ESC")) {
                mc.setScreen(null);
            } else if (line.equals("QUIT")) {
                mc.stop();
                return;
            }
        }
    }

    /** Distance from an arena's centre to stand clear of its pit. */
    private static final class ArenaDesignOffset {
        static int of(String structure) {
            return structure.contains("singularity_sovereign") ? 16 : structure.contains("void_horror") ? 8 : 0;
        }
    }

    /** Dev-only: flip the integrated world's "allow cheats" for this session (LevelSettings is immutable, so swap it). */
    private static void setAllowCommands(net.minecraft.server.MinecraftServer srv, boolean allow) {
        if (!(srv.getWorldData() instanceof net.minecraft.world.level.storage.PrimaryLevelData pld)) return;
        var cur = pld.getLevelSettings();
        if (cur.allowCommands() == allow) return;
        try {
            var f = net.minecraft.world.level.storage.PrimaryLevelData.class.getDeclaredField("settings");
            f.setAccessible(true);
            f.set(pld, new net.minecraft.world.level.LevelSettings(cur.levelName(), cur.gameType(), cur.hardcore(), cur.difficulty(), allow,
                cur.gameRules(), cur.getDataConfiguration(), cur.getLifecycle()));
        } catch (ReflectiveOperationException ex) { QuantumFalcon.LOGGER.warn("[smoke] could not set allowCommands", ex); }
    }

    private static int orbitTicks, orbitTotal;
    private static double orbitR, orbitH, orbitDeg, orbitYaw0;
    private static boolean orbitMob;   // ORBIT ... mob: centre on the nearest mod creature itself (not the boss/player midpoint)

    private static net.minecraft.world.entity.Entity orbitTarget(Minecraft mc) {
        if (!orbitMob) return nearestBoss(mc);
        net.minecraft.world.entity.Entity best = null; double bd = 48 * 48;
        for (var e : mc.level.entitiesForRendering())
            if (e instanceof net.minecraft.world.entity.LivingEntity && !(e instanceof net.minecraft.world.entity.player.Player) && e != orbitCam
                    && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(QuantumFalcon.MOD_ID)
                    && e.distanceToSqr(mc.player) < bd) { bd = e.distanceToSqr(mc.player); best = e; }
        return best;
    }
    private static net.minecraft.world.entity.decoration.ArmorStand orbitCam;

    private static net.minecraft.world.entity.Entity nearestBoss(Minecraft mc) {
        net.minecraft.world.entity.Entity best = null; double bd = 120 * 120;
        for (var e : mc.level.entitiesForRendering())
            if (e instanceof com.matt.quantumfalcon.entity.boss.QFBoss && e.isAlive() && e.distanceToSqr(mc.player) < bd) { bd = e.distanceToSqr(mc.player); best = e; }
        return best;
    }

    /** Places the ORBIT camera: around the boss/player midpoint, looking at it; hands the view back when done. */
    private static void tickOrbit(Minecraft mc) {
        if (orbitCam == null || orbitCam.level() != mc.level) { orbitTicks = 0; mc.setCameraEntity(mc.player); mc.gameRenderer.setRenderHand(true); return; }
        var b = orbitTarget(mc);
        var me = mc.player.position().add(0, 1, 0);
        var focus = b == null ? me : me.lerp(b.position().add(0, b.getBbHeight() * 0.5, 0), orbitMob ? 1.0 : 0.5);
        double t = 1.0 - (double) orbitTicks / Math.max(1, orbitTotal);
        double a = Math.toRadians(orbitYaw0 + orbitDeg * t);
        var pos = new net.minecraft.world.phys.Vec3(focus.x + Math.cos(a) * orbitR, focus.y + orbitH, focus.z + Math.sin(a) * orbitR);
        var look = focus.subtract(pos);
        float ry = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90.0);
        float rx = (float) -Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
        orbitCam.setOldPosAndRot();
        orbitCam.yRotO = orbitCam.getYRot(); orbitCam.xRotO = orbitCam.getXRot();
        orbitCam.setPos(pos.x, pos.y, pos.z);
        orbitCam.setYRot(ry); orbitCam.setXRot(rx); orbitCam.setYHeadRot(ry);
        if (mc.getCameraEntity() != orbitCam && !com.matt.quantumfalcon.client.cutscene.CutsceneClient.active()) mc.setCameraEntity(orbitCam);
        if (--orbitTicks <= 0) { mc.setCameraEntity(mc.player); mc.gameRenderer.setRenderHand(true); }
    }
}
