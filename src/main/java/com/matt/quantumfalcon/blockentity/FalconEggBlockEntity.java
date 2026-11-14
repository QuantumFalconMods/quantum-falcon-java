package com.matt.quantumfalcon.blockentity;

import com.matt.quantumfalcon.breeding.AbilityEngine;
import com.matt.quantumfalcon.breeding.BreedingData;
import com.matt.quantumfalcon.breeding.BreedingEngine;
import com.matt.quantumfalcon.breeding.BreedingService;
import com.matt.quantumfalcon.breeding.FalconGenome;
import com.matt.quantumfalcon.entity.QuantumFalconEntity;
import com.matt.quantumfalcon.genetics.FalconGenetics;
import com.matt.quantumfalcon.registry.ModBlocks;
import com.matt.quantumfalcon.registry.ModEntities;
import com.matt.quantumfalcon.registry.ModParticles;
import com.matt.quantumfalcon.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Falcon egg. Holds the parents' genomes and the child's genome (rolled when the egg is laid, re-rolled once if a catalyst
 * is applied before 25%), incubates according to the child's tier (BREEDING.md), shows five hatch stages and hatches the
 * chick with its genome, tamed to the breeder. Eggs never spoil: missing conditions just pause them.
 */
public class FalconEggBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String[] STAGES = {"fresh", "warming", "hairline cracks", "spreading cracks", "breaking"};
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.falcon_egg.idle");
    private static final RawAnimation WOBBLE1 = RawAnimation.begin().thenLoop("animation.falcon_egg.wobble_1");
    private static final RawAnimation WOBBLE2 = RawAnimation.begin().thenLoop("animation.falcon_egg.wobble_2");
    private static final RawAnimation WOBBLE3 = RawAnimation.begin().thenLoop("animation.falcon_egg.wobble_3");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable private CompoundTag parentA, parentB, child;
    @Nullable private String catalyst;
    @Nullable private UUID owner;
    private float progress;           // 0..1
    private int stage;
    private boolean tierUpRolled, tierUp;
    private int baseTier;
    @Nullable private String hybrid, fusion;
    private String waitingFor = "";
    private int tickCounter;

    public FalconEggBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    // ── setup ─────────────────────────────────────────────────────────────────────────────────────────────────
    /** A bred egg: rolls the child now so the shell shows its tier and crack colour. */
    public void setParents(FalconGenome a, FalconGenome b, @Nullable UUID owner, BreedingEngine.Context ctx) {
        this.parentA = a.save(); this.parentB = b.save(); this.owner = owner;
        roll(ctx);
    }
    /** A wild egg (nest site, cache, loot): fixed genome, no parents. */
    public void setWildChild(FalconGenome g) { this.child = g.save(); this.baseTier = g.tier; sync(); }
    /** Worldgen variant: no block update (the chunk is still being generated). */
    public void setWildChildWorldgen(FalconGenome g) { this.child = g.save(); this.baseTier = g.tier; }
    /** Legacy hook (old FalconGenetics tags): converts to genomes. */
    public void setParentGenetics(CompoundTag p1, CompoundTag p2) {
        var r = net.minecraft.util.RandomSource.create();
        FalconGenetics g1 = FalconGenetics.deserialize(p1), g2 = FalconGenetics.deserialize(p2);
        FalconGenome a = BreedingEngine.wild(g1 != null ? g1.getVariantFromGenes() : "peregrine", 1, r);
        FalconGenome b = BreedingEngine.wild(g2 != null ? g2.getVariantFromGenes() : "peregrine", 1, r);
        setParents(a, b, null, level instanceof ServerLevel sl ? BreedingService.contextAt(sl, worldPosition) : new BreedingEngine.Context());
    }

    private void roll(BreedingEngine.Context ctx) {
        FalconGenome a = FalconGenome.load(parentA), b = FalconGenome.load(parentB);
        if (a == null || b == null) return;
        ctx.catalyst = catalyst;
        Player p = ownerPlayer();
        if (p != null) {
            int bt = Math.max(Math.min(a.tier, b.tier), (a.tier + b.tier) / 2);
            ctx.pity = BreedingService.pity(p, bt);
        }
        var rand = level != null ? level.getRandom() : net.minecraft.util.RandomSource.create();
        var res = BreedingEngine.breed(a, b, ctx, rand);
        child = res.child.save();
        tierUpRolled = res.tierUpRolled; tierUp = res.tierUp;
        baseTier = res.tierUp ? res.child.tier - 1 : res.child.tier;
        hybrid = res.hybrid; fusion = res.fusion;
        sync();
    }

    /** Use a catalyst on the egg. Allowed once, before 25% progress. */
    public Component applyCatalyst(String id) {
        if (catalyst != null) return Component.literal("This egg already absorbed a catalyst.").withStyle(ChatFormatting.GRAY);
        if (progress >= 0.25f) return Component.literal("Too late: catalysts only take before 25% incubation.").withStyle(ChatFormatting.GRAY);
        catalyst = id;
        if (parentA != null && parentB != null && level instanceof ServerLevel sl) roll(BreedingService.contextAt(sl, worldPosition));
        else if (child != null) {   // wild egg: catalysts still shift traits and stats
            FalconGenome g = FalconGenome.load(child);
            var ctx = new BreedingEngine.Context().catalyst(id);
            var res = BreedingEngine.breed(g, g.copy(), ctx, level.getRandom());
            res.child.variant = g.variant; res.child.tier = Math.max(g.tier, res.child.tier);
            res.child.parents.clear(); res.child.grandparents.clear(); res.child.generation = g.generation;
            child = res.child.save(); sync();
        }
        setChanged();
        return Component.literal("The egg absorbs the " + id.replace('_', ' ') + ".").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    // ── accessors ─────────────────────────────────────────────────────────────────────────────────────────────
    @Nullable public FalconGenome getChild() { return child == null ? null : FalconGenome.load(child); }
    public boolean hasParents() { return (parentA != null && parentB != null) || child != null; }
    public int getHatchProgress() { return (int) (progress * 100); }
    public float progress() { return progress; }
    public int stage() { return stage; }
    public String waitingFor() { return waitingFor; }
    @Nullable public String catalyst() { return catalyst; }
    public int tier() { FalconGenome g = getChild(); return g == null ? 1 : g.tier; }
    public String getPredictedVariant() { FalconGenome g = getChild(); return g == null ? "peregrine" : g.variant; }
    @Nullable public CompoundTag getParent1Genetics() { return parentA == null ? null : parentA.copy(); }
    @Nullable public CompoundTag getParent2Genetics() { return parentB == null ? null : parentB.copy(); }
    @Nullable private Player ownerPlayer() { return owner != null && level != null ? level.getPlayerByUUID(owner) : null; }

    /** Status line for the action bar and the Codex. */
    public Component status() {
        FalconGenome g = getChild();
        if (g == null) return Component.literal("An empty shell.");
        var td = g.tierDef();
        String need = waitingFor.isEmpty() ? "incubating" : "waiting for " + waitingFor;
        return Component.literal(td.name() + " egg").withColor(td.colour())
                .append(Component.literal(" | " + getHatchProgress() + "% (" + STAGES[stage] + ") | " + need
                        + (catalyst != null ? " | catalyst: " + catalyst.replace('_', ' ') : "")).withStyle(ChatFormatting.GRAY));
    }

    // ── ticking ───────────────────────────────────────────────────────────────────────────────────────────────
    public static void tick(Level level, BlockPos pos, BlockState state, FalconEggBlockEntity be) {
        if (be.frozen) return;   // test/screenshot eggs hold their stage
        if (level.isClientSide()) { be.clientTick(level, pos); return; }
        be.serverTick((ServerLevel) level, pos);
    }

    private void clientTick(Level level, BlockPos pos) {
        if (stage >= 3 && level.getRandom().nextInt(6) == 0)
            level.addParticle(ModParticles.QUANTUM_GLOW.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    (level.getRandom().nextDouble() - 0.5) * 0.08, 0.04, (level.getRandom().nextDouble() - 0.5) * 0.08);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (child == null) return;
        if (++tickCounter < 20) return;
        tickCounter = 0;
        FalconGenome g = getChild();
        String missing = missingCondition(level, pos, g.tierDef().incubation());
        if (!missing.equals(waitingFor)) { waitingFor = missing; sync(); }
        if (!missing.isEmpty()) return;
        float speed = 1f;
        if (AbilityEngine.eggWarmerNear(level, pos)) speed *= 1.25f;
        if (g.bloodlines().contains("VERDANT")) speed *= 1.2f;
        if ("chrono_sand".equals(catalyst)) speed *= 1.4f;
        if (near(level, pos, 3, s -> s.is(ModBlocks.QUANTUM_FORGE.get()))) speed *= 1.5f;
        if (com.matt.quantumfalcon.skill.SkillEffects.warmthNear(level, pos)) speed *= com.matt.quantumfalcon.skill.SkillEffects.WARMTH_MULT;   // lin_warmth
        var nq = com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(level, pos.below());
        if (nq != null) speed *= nq.speed;
        if (broodingFalconNear(level, pos)) speed *= 1.2f;
        progress = Math.min(1f, progress + 20f * speed / BreedingEngine.hatchTicks(g.tier));
        int st = progress < 0.05f ? 0 : progress < 0.4f ? 1 : progress < 0.7f ? 2 : progress < 0.9f ? 3 : 4;
        if (st != stage) {
            stage = st; sync();
            if (st >= 2) level.playSound(null, pos, ModSounds.EGG_WOBBLE.get(), SoundSource.BLOCKS, 0.5f + 0.1f * st, 1.0f);
        }
        setChanged();
        if (progress >= 1f) hatch(level, pos);
    }

    /** Empty string if the tier's incubation needs are met, otherwise what's missing. */
    public String missingCondition(ServerLevel level, BlockPos pos, String need) {
        BlockState below = level.getBlockState(pos.below());
        com.matt.quantumfalcon.breeding.NestQuality nq = com.matt.quantumfalcon.block.FalconNestBlock.qualityAt(level, pos.below());
        boolean nest = nq != null;
        boolean incubator = below.is(ModBlocks.QUANTUM_INCUBATOR.get());
        boolean fuelled = incubator && level.getBlockEntity(pos.below()) instanceof QuantumIncubatorBlockEntity ib && ib.hasFuel();
        boolean heat = heatNear(level, pos, 2) || (nq != null && nq.heat);
        boolean player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, false) != null;
        int layer = BreedingService.riftLayer(level);
        boolean riftEnergy = layer >= 0 || fuelled || (nq != null && nq.riftEnergy)
                || near(level, pos, 6, s -> s.is(com.matt.quantumfalcon.registry.ModFamilyBlocks.RIFT_STABILIZER.get()));
        boolean forge = near(level, pos, 4, s -> s.is(ModBlocks.QUANTUM_FORGE.get())) || (nq != null && nq.quantum);
        return switch (need) {
            case "warmth" -> player || nest || heat || incubator ? "" : "warmth (stay within 8 blocks, or use a nest or heat source)";
            case "heat" -> heat || incubator ? "" : "a heat source within 2 blocks, a Crystal Nest or better, or an Incubator";
            case "nest_or_incubator_heat" -> (nest && heat) || incubator ? "" : "a nest beside a heat source, a Crystal Nest or better, or a Quantum Incubator";
            case "rift_energy" -> riftEnergy ? "" : "rift energy (Echo Rift, a Rift Stabilizer within 6, a Void-forged Nest or better, or a fuelled Incubator)";
            case "rift_energy_and_heat" -> !riftEnergy ? "rift energy (Echo Rift, a Rift Stabilizer, or a fuelled Incubator)"
                    : (heat || incubator) ? "" : "a heat source or an Incubator";
            case "quantum" -> (fuelled && layer >= 3) || forge ? "" : "a Quantum Aerie, a fuelled Quantum Incubator in Echo Rift layer 3+, or a Quantum Forge within 4";
            default -> "";
        };
    }

    private static boolean heatNear(Level level, BlockPos pos, int r) {
        return near(level, pos, r, s -> s.is(BlockTags.FIRE) || s.is(BlockTags.CAMPFIRES) || s.is(Blocks.MAGMA_BLOCK) || s.getFluidState().is(FluidTags.LAVA));
    }
    private static boolean near(Level level, BlockPos pos, int r, java.util.function.Predicate<BlockState> p) {
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 2, r))) if (p.test(level.getBlockState(q))) return true;
        return false;
    }
    /** A falcon resting on/beside the egg broods it. */
    private static boolean broodingFalconNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(QuantumFalconEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1.0, 1.0, 1.0),
                f -> f.isResting() || f.isInSittingPose()).isEmpty();
    }

    /** Test/debug hook. */
    public void forceHatch(ServerLevel level) {
        if (child == null) setWildChild(BreedingEngine.wild("peregrine", 1, level.getRandom()));
        progress = 1f; hatch(level, worldPosition);
    }
    /** Test hook: set progress directly (0..1). */
    private boolean frozen;
    public void freezeForTest() { frozen = true; }
    public void setProgressForTest(float p) { progress = p; stage = p < 0.05f ? 0 : p < 0.4f ? 1 : p < 0.7f ? 2 : p < 0.9f ? 3 : 4; sync(); }

    @Nullable public QuantumFalconEntity lastHatched;

    private void hatch(ServerLevel level, BlockPos pos) { hatch(level, pos, true); }

    /** Hatches an egg that lives in an item (Quantum Incubator): same rewards, discovery and taming as a placed egg. */
    @Nullable public static QuantumFalconEntity hatchDetached(ServerLevel level, BlockPos at, CompoundTag eggData, @Nullable UUID fallbackOwner) {
        FalconEggBlockEntity be = new FalconEggBlockEntity(com.matt.quantumfalcon.registry.ModBlockEntities.FALCON_EGG.get(), at, ModBlocks.FALCON_EGG_BLOCK.get().defaultBlockState());
        be.setLevel(level);
        be.loadEggData(eggData);
        if (be.child == null) be.child = BreedingEngine.wild("peregrine", 1, level.getRandom()).save();
        if (be.owner == null) be.owner = fallbackOwner;
        be.hatch(level, at, false);
        return be.lastHatched;
    }

    /** the breed_legendary advancement (criterion "breed_legendary", trigger impossible) is granted here. */
    /** Grants breed_legendary. True when the advancement exists and the award was issued (fake players ignore awards). */
    public static boolean awardLegendary(Player p) {
        if (!(p instanceof net.minecraft.server.level.ServerPlayer sp)) return false;
        var adv = sp.server.getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quantumfalcon", "breed_legendary"));
        if (adv == null || !adv.value().criteria().containsKey("breed_legendary")) return false;
        sp.getAdvancements().award(adv, "breed_legendary");
        return true;
    }

    private void hatch(ServerLevel level, BlockPos pos, boolean placed) {
        FalconGenome g = getChild();
        if (g == null) return;
        if (placed && level.getBlockState(pos.below()).is(ModBlocks.QUANTUM_INCUBATOR.get()) && level.getBlockEntity(pos.below()) instanceof QuantumIncubatorBlockEntity ib
                && g.tier >= 5) ib.consumeFuel();
        QuantumFalconEntity baby = ModEntities.QUANTUM_FALCON.get().create(level);
        Player p = ownerPlayer();
        if (baby != null) {
            baby.setBaby(true);
            baby.setGenome(g);
            baby.moveTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
            if (p != null) baby.tame(p);
            else if (owner != null) { baby.setOwnerUUID(owner); baby.setTame(true, true); }
            Player lineageOwner = p != null ? p : (owner != null ? com.matt.quantumfalcon.skill.SkillEffects.TEST_OWNERS.get(owner) : null);
            com.matt.quantumfalcon.skill.SkillEffects.onHatched(baby, lineageOwner);   // Lineage branch
            level.addFreshEntity(baby);
            lastHatched = baby;
        }
        // the hatch chime rises with the tier
        level.playSound(null, pos, (g.tier >= 6 ? com.matt.quantumfalcon.registry.ModSounds.BREEDING_LEGENDARY : g.tier >= 3 ? com.matt.quantumfalcon.registry.ModSounds.BREEDING_RARE
            : com.matt.quantumfalcon.registry.ModSounds.BREEDING_SUCCESS).get(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
        if (p != null) {
            if (g.tier >= 6 && parentA != null && parentB != null) awardLegendary(p);   // Legendary or Quantum from a bred egg
            if (tierUpRolled) BreedingService.recordTierRoll(p, baseTier, tierUp);
            boolean newVariant = BreedingService.discover(p, "variant:" + g.variant);
            if (hybrid != null) BreedingService.discover(p, "hybrid:" + hybrid);
            if (fusion != null) BreedingService.discover(p, "fusion:" + fusion);
            for (String a : g.abilities) BreedingService.discover(p, "ability:" + a);
            var td = g.tierDef();
            Component msg = Component.literal("Hatched: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(td.name() + " ").withColor(td.colour()))
                    .append(Component.translatable("falcon_variant.quantumfalcon." + g.variant).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal((tierUp ? "  TIER UP!" : "") + (hybrid != null ? "  New hybrid!" : "")
                            + (fusion != null ? "  Fusion: " + BreedingEngine.abilityDef(fusion).name() : "")
                            + (newVariant ? "  (new to your Codex)" : "")).withStyle(ChatFormatting.LIGHT_PURPLE));
            p.displayClientMessage(msg, false);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) com.matt.quantumfalcon.quest.QuestTracker.onEggHatched(sp, baby, g.variant);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) com.matt.quantumfalcon.skill.SkillPointSystem.awardHatchMilestone(sp, g.tier);
            com.matt.quantumfalcon.story.StorylineManager.triggerStoryEvent(p, com.matt.quantumfalcon.story.StorylineManager.StoryChapter.CHAPTER_1);
        }
        // (the turtle-egg layer is gone: entity/egg_hatch.ogg already has the shell cracks and a baby kek)
        level.playSound(null, pos, ModSounds.EGG_HATCH.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        level.sendParticles(ModParticles.QUANTUM_GLOW.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30 + 10 * g.tier, 0.3, 0.3, 0.3, 0.05);
        if (placed) level.destroyBlock(pos, false);
    }

    // ── sync + NBT ────────────────────────────────────────────────────────────────────────────────────────────
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        FalconGenome g = getChild();
        if (g != null) {
            t.putString("vVariant", g.variant); t.putInt("vTier", g.tier); t.putInt("vCrack", g.expressedCrack());
        }
        t.putInt("vStage", stage); t.putFloat("vProgress", progress);
        return t;
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(CompoundTag t, HolderLookup.Provider provider) {
        clientVariant = t.getString("vVariant"); clientTier = Math.max(1, t.getInt("vTier")); clientCrack = t.getInt("vCrack");
        stage = t.getInt("vStage"); progress = t.getFloat("vProgress");
    }
    @Override public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }
    // client-side visuals
    public String clientVariant = "peregrine";
    public int clientTier = 1, clientCrack = 0;

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        saveEggData(tag);
    }
    public void saveEggData(CompoundTag tag) {
        if (parentA != null) tag.put("ParentA", parentA);
        if (parentB != null) tag.put("ParentB", parentB);
        if (child != null) tag.put("Child", child);
        if (catalyst != null) tag.putString("Catalyst", catalyst);
        if (owner != null) tag.putUUID("Owner", owner);
        if (hybrid != null) tag.putString("Hybrid", hybrid);
        if (fusion != null) tag.putString("Fusion", fusion);
        tag.putFloat("Progress", progress); tag.putInt("Stage", stage);
        tag.putBoolean("TierUpRolled", tierUpRolled); tag.putBoolean("TierUp", tierUp); tag.putInt("BaseTier", baseTier);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        loadEggData(tag);
    }
    public void loadEggData(CompoundTag tag) {
        parentA = tag.contains("ParentA") ? tag.getCompound("ParentA") : null;
        parentB = tag.contains("ParentB") ? tag.getCompound("ParentB") : null;
        child = tag.contains("Child") ? tag.getCompound("Child") : null;
        catalyst = tag.contains("Catalyst") ? tag.getString("Catalyst") : null;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        hybrid = tag.contains("Hybrid") ? tag.getString("Hybrid") : null;
        fusion = tag.contains("Fusion") ? tag.getString("Fusion") : null;
        progress = tag.getFloat("Progress"); stage = tag.getInt("Stage");
        tierUpRolled = tag.getBoolean("TierUpRolled"); tierUp = tag.getBoolean("TierUp"); baseTier = tag.getInt("BaseTier");
        FalconGenome g = getChild();
        if (g != null) { clientVariant = g.variant; clientTier = g.tier; clientCrack = g.expressedCrack(); }
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────────────────────────────────────
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "egg", 6, e -> e.setAndContinue(stage >= 4 ? WOBBLE3 : stage == 3 ? WOBBLE2 : stage == 2 ? WOBBLE1 : IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static int crackColour(int idx) { return BreedingData.CRACKS.get(Math.max(0, Math.min(BreedingData.CRACKS.size() - 1, idx))).colour(); }
}
