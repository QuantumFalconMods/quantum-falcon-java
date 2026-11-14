package com.matt.quantumfalcon.registry;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound Event Registry for Quantum Falcon Mod
 * 
 * All sounds are registered here and referenced in sounds.json
 * Sound files go in: assets/quantumfalcon/sounds/
 * Sound definitions go in: assets/quantumfalcon/sounds.json
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = 
        DeferredRegister.create(Registries.SOUND_EVENT, QuantumFalcon.MOD_ID);

    // ========================================
    // FALCON SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_AMBIENT = 
        SOUNDS.register("entity_falcon_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_HURT = 
        SOUNDS.register("entity_falcon_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_DEATH = 
        SOUNDS.register("entity_falcon_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_FLAP = 
        SOUNDS.register("entity_falcon_flap", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_SCREECH = 
        SOUNDS.register("entity_falcon_screech", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // BREEDING SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> BREEDING_SUCCESS = 
        SOUNDS.register("breeding_success", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BREEDING_RARE = 
        SOUNDS.register("breeding_rare", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BREEDING_LEGENDARY = 
        SOUNDS.register("breeding_legendary", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // QUANTUM EFFECTS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_SPARK = 
        SOUNDS.register("quantum_spark", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_BLINK = 
        SOUNDS.register("quantum_blink", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // UI SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_SKILL_UNLOCK = 
        SOUNDS.register("ui_skill_unlock", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_LEVEL_UP = 
        SOUNDS.register("ui_level_up", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // QUEST SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_ACCEPT = 
        SOUNDS.register("quest_accept", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_COMPLETE = 
        SOUNDS.register("quest_complete", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // FALCON CALLS (10 Variants)
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_1 = 
        SOUNDS.register("falcon_call_1", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_2 = 
        SOUNDS.register("falcon_call_2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_3 = 
        SOUNDS.register("falcon_call_3", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_4 = 
        SOUNDS.register("falcon_call_4", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_5 = 
        SOUNDS.register("falcon_call_5", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_6 = 
        SOUNDS.register("falcon_call_6", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_7 = 
        SOUNDS.register("falcon_call_7", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_8 = 
        SOUNDS.register("falcon_call_8", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_9 = 
        SOUNDS.register("falcon_call_9", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_CALL_10 = 
        SOUNDS.register("falcon_call_10", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // FALCON TAMING & INTERACTION
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_TAME = 
        SOUNDS.register("falcon_tame", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_FEED = 
        SOUNDS.register("falcon_feed", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_BOND_UP = 
        SOUNDS.register("falcon_bond_up", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_WHISTLE = 
        SOUNDS.register("falcon_whistle", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_BREED = 
        SOUNDS.register("entity_quantum_falcon_breed", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_WOBBLE = 
        SOUNDS.register("egg_wobble", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_HATCH = 
        SOUNDS.register("egg_hatch", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // RIFT & DIMENSION SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PORTAL = 
        SOUNDS.register("rift_portal", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_OPEN = 
        SOUNDS.register("rift_open", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_CLOSE = 
        SOUNDS.register("rift_close", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WHOOSH = 
        SOUNDS.register("rift_whoosh", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> DIMENSION_SHIFT = 
        SOUNDS.register("dimension_shift", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // INSTABILITY SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> INSTABILITY_WARNING = 
        SOUNDS.register("instability_warning", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> INSTABILITY_CRITICAL = 
        SOUNDS.register("instability_critical", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> INSTABILITY_CHIME = 
        SOUNDS.register("instability_chime", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> STABILIZER_ACTIVATE = 
        SOUNDS.register("stabilizer_activate", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // BOSS SOUNDS
    // ========================================
    // Falcon variant abilities (original synthesised: tools/synth_falcon_abilities.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_STRIKE =
        SOUNDS.register("falcon_ability_strike", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_FROST =
        SOUNDS.register("falcon_ability_frost", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_STORM =
        SOUNDS.register("falcon_ability_storm", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_FLAME =
        SOUNDS.register("falcon_ability_flame", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_HEAL =
        SOUNDS.register("falcon_ability_heal", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_VOID =
        SOUNDS.register("falcon_ability_void", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_WIND =
        SOUNDS.register("falcon_ability_wind", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_RIFT =
        SOUNDS.register("falcon_ability_rift", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_NATURE =
        SOUNDS.register("falcon_ability_nature", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_COSMIC =
        SOUNDS.register("falcon_ability_cosmic", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> FALCON_ABILITY_PASSIVE =
        SOUNDS.register("falcon_ability_passive", SoundEvent::createVariableRangeEvent);
    // Sha'Sha, the Hollow Falconer (original synthesised: tools/synth_sha_sha.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> SHA_SHA_AMBIENT =
        SOUNDS.register("entity_sha_sha_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> SHA_SHA_HURT =
        SOUNDS.register("entity_sha_sha_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> SHA_SHA_DEATH =
        SOUNDS.register("entity_sha_sha_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> SHA_SHA_SLAM =
        SOUNDS.register("entity_sha_sha_slam", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> SHA_SHA_BLINK =
        SOUNDS.register("entity_sha_sha_blink", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_PHASE_HORN = 
        SOUNDS.register("boss_phase_horn", SoundEvent::createVariableRangeEvent);

    // boss music v2: intro + harder loop per boss (tools/compose_boss_themes.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_OVERSEER_INTRO = SOUNDS.register("music_boss_overseer_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_OVERSEER_P2 = SOUNDS.register("music_boss_overseer_p2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_WARDEN_INTRO = SOUNDS.register("music_boss_warden_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_WARDEN_P2 = SOUNDS.register("music_boss_warden_p2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_TWINS_INTRO = SOUNDS.register("music_boss_twins_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_TWINS_P2 = SOUNDS.register("music_boss_twins_p2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_SOVEREIGN_INTRO = SOUNDS.register("music_boss_sovereign_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_SOVEREIGN_P2 = SOUNDS.register("music_boss_sovereign_p2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_ECLIPSE_INTRO = SOUNDS.register("music_boss_eclipse_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_ECLIPSE_P2 = SOUNDS.register("music_boss_eclipse_p2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_VOID_LORD_INTRO = SOUNDS.register("music_boss_void_lord_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_VOID_LORD_P2 = SOUNDS.register("music_boss_void_lord_p2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_SHA_SHA_INTRO = SOUNDS.register("music_boss_sha_sha_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_SHA_SHA_P2 = SOUNDS.register("music_boss_sha_sha_p2", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR_OVerseer = 
        SOUNDS.register("boss_roar_overseer", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR_WARDEN = 
        SOUNDS.register("boss_roar_warden", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR_TWINS = 
        SOUNDS.register("boss_roar_twins", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR_SOVEREIGN = 
        SOUNDS.register("boss_roar_sovereign", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_DEATH = 
        SOUNDS.register("boss_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_SUMMON = 
        SOUNDS.register("boss_summon", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // SINGULARITY & GRAVITY SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> SINGULARITY_HUM = 
        SOUNDS.register("singularity_hum", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> SINGULARITY_PULL = 
        SOUNDS.register("singularity_pull", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVITY_WELL = 
        SOUNDS.register("gravity_well", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_FORM = 
        SOUNDS.register("black_hole_form", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // QUANTUM ABILITY SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_BLINK_START = 
        SOUNDS.register("quantum_blink_start", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_BLINK_END = 
        SOUNDS.register("quantum_blink_end", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_DASH = 
        SOUNDS.register("quantum_dash", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTANGLEMENT_ACTIVATE = 
        SOUNDS.register("entanglement_activate", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // AMBIENT & WORLD SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_BIOME_AMBIENT = 
        SOUNDS.register("quantum_biome_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_DEPTHS_AMBIENT = 
        SOUNDS.register("echo_depths_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_STORM = 
        SOUNDS.register("quantum_storm", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> AURORA_BOREALIS = 
        SOUNDS.register("aurora_borealis", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // ITEM & CRAFTING SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_FORGE_CRAFT = 
        SOUNDS.register("quantum_forge_craft", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_EQUIP = 
        SOUNDS.register("artifact_equip", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_UNEQUIP = 
        SOUNDS.register("artifact_unequip", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> CURIOS_EQUIP = 
        SOUNDS.register("curios_equip", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOK_OPEN =
        SOUNDS.register("book_open", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_FIRE =
        SOUNDS.register("grapple_fire", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_HIT =
        SOUNDS.register("grapple_hit", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_PULL =
        SOUNDS.register("grapple_pull", SoundEvent::createVariableRangeEvent);

    // ========================================
    // ENTITY EVENTS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> ANOMALY_SPAWN =
        SOUNDS.register("anomaly_spawn", SoundEvent::createVariableRangeEvent);
    
    // ========================================
    // MUSIC & THEME SOUNDS
    // ========================================
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_OVerseer = 
        SOUNDS.register("music_boss_overseer", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_WARDEN = 
        SOUNDS.register("music_boss_warden", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_TWINS = 
        SOUNDS.register("music_boss_twins", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_SOVEREIGN = 
        SOUNDS.register("music_boss_sovereign", SoundEvent::createVariableRangeEvent);


    // ── polish stage 3: weapon & armour FX (original synthesised, tools/synth_weapon_sounds.py) ──
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_ECLIPSE = SOUNDS.register("music_boss_eclipse", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_VOID_LORD = SOUNDS.register("music_boss_void_lord", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BOSS_SHA_SHA = SOUNDS.register("music_boss_sha_sha", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR = SOUNDS.register("boss_roar", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP = SOUNDS.register("boss_windup", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_PHASE = SOUNDS.register("boss_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SWING = SOUNDS.register("weapon_swing", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RIFT_SLASH = SOUNDS.register("weapon_rift_slash", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_VOID_REAP = SOUNDS.register("weapon_void_reap", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_STAFF_CAST = SOUNDS.register("weapon_staff_cast", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_HAMMER_SLAM = SOUNDS.register("weapon_hammer_slam", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BLINK = SOUNDS.register("weapon_blink", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TEMPORAL_TICK = SOUNDS.register("weapon_temporal_tick", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARMOR_SET_COMPLETE = SOUNDS.register("armor_set_complete", SoundEvent::createVariableRangeEvent);


    // ── audio pack (2026-11): per-boss / per-mob voices, cutscene layers, arena, incubator, UI ──
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_BLINK_END = SOUNDS.register("ability_blink_end", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_BLINK_START = SOUNDS.register("ability_blink_start", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_DASH = SOUNDS.register("ability_dash", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_ENTANGLEMENT = SOUNDS.register("ability_entanglement", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_SWOOSH = SOUNDS.register("ability_swoosh", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_ECHO_DEPTHS = SOUNDS.register("ambient_echo_depths", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_RIFT_ECHO = SOUNDS.register("ambient_rift_echo", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_PYLON_BEAM = SOUNDS.register("arena_pylon_beam", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_PYLON_BREAK = SOUNDS.register("arena_pylon_break", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_PYLON_CHARGE = SOUNDS.register("arena_pylon_charge", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_SEAL_DOWN = SOUNDS.register("arena_seal_down", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_SEAL_UP = SOUNDS.register("arena_seal_up", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_TELEGRAPH = SOUNDS.register("arena_telegraph", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ARENA_TELEGRAPH_LOCK = SOUNDS.register("arena_telegraph_lock", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOK_PLACE = SOUNDS.register("book_place", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_ECLIPSE_DEATH = SOUNDS.register("boss_cutscene_eclipse_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_ECLIPSE_INTRO = SOUNDS.register("boss_cutscene_eclipse_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_ECLIPSE_PHASE = SOUNDS.register("boss_cutscene_eclipse_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_OVERSEER_DEATH = SOUNDS.register("boss_cutscene_overseer_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_OVERSEER_INTRO = SOUNDS.register("boss_cutscene_overseer_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_OVERSEER_PHASE = SOUNDS.register("boss_cutscene_overseer_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_SHA_SHA_DEATH = SOUNDS.register("boss_cutscene_sha_sha_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_SHA_SHA_INTRO = SOUNDS.register("boss_cutscene_sha_sha_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_SHA_SHA_PHASE = SOUNDS.register("boss_cutscene_sha_sha_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_SOVEREIGN_DEATH = SOUNDS.register("boss_cutscene_sovereign_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_SOVEREIGN_INTRO = SOUNDS.register("boss_cutscene_sovereign_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_SOVEREIGN_PHASE = SOUNDS.register("boss_cutscene_sovereign_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_TWINS_DEATH = SOUNDS.register("boss_cutscene_twins_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_TWINS_INTRO = SOUNDS.register("boss_cutscene_twins_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_TWINS_PHASE = SOUNDS.register("boss_cutscene_twins_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_VOID_LORD_DEATH = SOUNDS.register("boss_cutscene_void_lord_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_VOID_LORD_INTRO = SOUNDS.register("boss_cutscene_void_lord_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_VOID_LORD_PHASE = SOUNDS.register("boss_cutscene_void_lord_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_WARDEN_DEATH = SOUNDS.register("boss_cutscene_warden_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_WARDEN_INTRO = SOUNDS.register("boss_cutscene_warden_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_CUTSCENE_WARDEN_PHASE = SOUNDS.register("boss_cutscene_warden_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR_ECLIPSE = SOUNDS.register("boss_roar_eclipse", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR_SHA_SHA = SOUNDS.register("boss_roar_sha_sha", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR_VOID_LORD = SOUNDS.register("boss_roar_void_lord", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_ECLIPSE_DEATH = SOUNDS.register("boss_voice_eclipse_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_ECLIPSE_INTRO = SOUNDS.register("boss_voice_eclipse_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_ECLIPSE_PHASE = SOUNDS.register("boss_voice_eclipse_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_OVERSEER_DEATH = SOUNDS.register("boss_voice_overseer_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_OVERSEER_INTRO = SOUNDS.register("boss_voice_overseer_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_OVERSEER_PHASE = SOUNDS.register("boss_voice_overseer_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_SHA_SHA_DEATH = SOUNDS.register("boss_voice_sha_sha_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_SHA_SHA_INTRO = SOUNDS.register("boss_voice_sha_sha_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_SHA_SHA_PHASE = SOUNDS.register("boss_voice_sha_sha_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_SOVEREIGN_DEATH = SOUNDS.register("boss_voice_sovereign_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_SOVEREIGN_INTRO = SOUNDS.register("boss_voice_sovereign_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_SOVEREIGN_PHASE = SOUNDS.register("boss_voice_sovereign_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_TWINS_DEATH = SOUNDS.register("boss_voice_twins_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_TWINS_INTRO = SOUNDS.register("boss_voice_twins_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_TWINS_PHASE = SOUNDS.register("boss_voice_twins_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_VOID_LORD_DEATH = SOUNDS.register("boss_voice_void_lord_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_VOID_LORD_INTRO = SOUNDS.register("boss_voice_void_lord_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_VOID_LORD_PHASE = SOUNDS.register("boss_voice_void_lord_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_WARDEN_DEATH = SOUNDS.register("boss_voice_warden_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_WARDEN_INTRO = SOUNDS.register("boss_voice_warden_intro", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_VOICE_WARDEN_PHASE = SOUNDS.register("boss_voice_warden_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP_ECLIPSE = SOUNDS.register("boss_windup_eclipse", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP_OVERSEER = SOUNDS.register("boss_windup_overseer", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP_SHA_SHA = SOUNDS.register("boss_windup_sha_sha", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP_SOVEREIGN = SOUNDS.register("boss_windup_sovereign", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP_TWINS = SOUNDS.register("boss_windup_twins", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP_VOID_LORD = SOUNDS.register("boss_windup_void_lord", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_WINDUP_WARDEN = SOUNDS.register("boss_windup_warden", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CONSTRUCT_AMBIENT = SOUNDS.register("entity_construct_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CONSTRUCT_ATTACK = SOUNDS.register("entity_construct_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CONSTRUCT_DEATH = SOUNDS.register("entity_construct_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CONSTRUCT_HURT = SOUNDS.register("entity_construct_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CONSTRUCT_SPLIT = SOUNDS.register("entity_construct_split", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_CONSTRUCT_STEP = SOUNDS.register("entity_construct_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_ECLIPSE_AMBIENT = SOUNDS.register("entity_eclipse_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_ECLIPSE_ATTACK = SOUNDS.register("entity_eclipse_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_ECLIPSE_DEATH = SOUNDS.register("entity_eclipse_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_ECLIPSE_FLAP = SOUNDS.register("entity_eclipse_flap", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_ECLIPSE_HURT = SOUNDS.register("entity_eclipse_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GOBLIN_AMBIENT = SOUNDS.register("entity_goblin_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GOBLIN_DEATH = SOUNDS.register("entity_goblin_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GOBLIN_HURT = SOUNDS.register("entity_goblin_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GOBLIN_STEP = SOUNDS.register("entity_goblin_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GUARDIAN_AMBIENT = SOUNDS.register("entity_guardian_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GUARDIAN_ATTACK = SOUNDS.register("entity_guardian_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GUARDIAN_CRYSTALLIZE = SOUNDS.register("entity_guardian_crystallize", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GUARDIAN_DEATH = SOUNDS.register("entity_guardian_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GUARDIAN_HURT = SOUNDS.register("entity_guardian_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_GUARDIAN_STEP = SOUNDS.register("entity_guardian_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MERCHANT_AMBIENT = SOUNDS.register("entity_merchant_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MERCHANT_DEATH = SOUNDS.register("entity_merchant_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MERCHANT_HURT = SOUNDS.register("entity_merchant_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MERCHANT_STEP = SOUNDS.register("entity_merchant_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MINION_AMBIENT = SOUNDS.register("entity_minion_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MINION_ATTACK = SOUNDS.register("entity_minion_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MINION_DEATH = SOUNDS.register("entity_minion_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MINION_HURT = SOUNDS.register("entity_minion_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MINION_STEP = SOUNDS.register("entity_minion_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_OVERSEER_AMBIENT = SOUNDS.register("entity_overseer_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_OVERSEER_ATTACK = SOUNDS.register("entity_overseer_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_OVERSEER_DEATH = SOUNDS.register("entity_overseer_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_OVERSEER_HURT = SOUNDS.register("entity_overseer_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_OVERSEER_STEP = SOUNDS.register("entity_overseer_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_PHANTOM_AMBIENT = SOUNDS.register("entity_phantom_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_PHANTOM_ATTACK = SOUNDS.register("entity_phantom_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_PHANTOM_DEATH = SOUNDS.register("entity_phantom_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_PHANTOM_FLAP = SOUNDS.register("entity_phantom_flap", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_PHANTOM_HURT = SOUNDS.register("entity_phantom_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SHA_SHA_ATTACK = SOUNDS.register("entity_sha_sha_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SHA_SHA_STEP = SOUNDS.register("entity_sha_sha_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SOVEREIGN_AMBIENT = SOUNDS.register("entity_sovereign_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SOVEREIGN_ATTACK = SOUNDS.register("entity_sovereign_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SOVEREIGN_DEATH = SOUNDS.register("entity_sovereign_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SOVEREIGN_HURT = SOUNDS.register("entity_sovereign_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_SOVEREIGN_STEP = SOUNDS.register("entity_sovereign_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STALKER_AMBIENT = SOUNDS.register("entity_stalker_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STALKER_ATTACK = SOUNDS.register("entity_stalker_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STALKER_DEATH = SOUNDS.register("entity_stalker_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STALKER_HURT = SOUNDS.register("entity_stalker_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STALKER_PHASE = SOUNDS.register("entity_stalker_phase", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_STALKER_STEP = SOUNDS.register("entity_stalker_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_TWINS_AMBIENT = SOUNDS.register("entity_twins_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_TWINS_ATTACK = SOUNDS.register("entity_twins_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_TWINS_DEATH = SOUNDS.register("entity_twins_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_TWINS_HURT = SOUNDS.register("entity_twins_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_TWINS_STEP = SOUNDS.register("entity_twins_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_ECHO_AMBIENT = SOUNDS.register("entity_void_echo_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_ECHO_ATTACK = SOUNDS.register("entity_void_echo_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_ECHO_DEATH = SOUNDS.register("entity_void_echo_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_ECHO_HURT = SOUNDS.register("entity_void_echo_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_ECHO_STEP = SOUNDS.register("entity_void_echo_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_ECHO_TELEPORT = SOUNDS.register("entity_void_echo_teleport", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_LORD_AMBIENT = SOUNDS.register("entity_void_lord_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_LORD_ATTACK = SOUNDS.register("entity_void_lord_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_LORD_DEATH = SOUNDS.register("entity_void_lord_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_LORD_HURT = SOUNDS.register("entity_void_lord_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_VOID_LORD_STEP = SOUNDS.register("entity_void_lord_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WARDEN_AMBIENT = SOUNDS.register("entity_warden_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WARDEN_ATTACK = SOUNDS.register("entity_warden_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WARDEN_DEATH = SOUNDS.register("entity_warden_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WARDEN_HURT = SOUNDS.register("entity_warden_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WARDEN_STEP = SOUNDS.register("entity_warden_step", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WISP_AMBIENT = SOUNDS.register("entity_wisp_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WISP_ATTACK = SOUNDS.register("entity_wisp_attack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WISP_BOTTLE = SOUNDS.register("entity_wisp_bottle", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WISP_DEATH = SOUNDS.register("entity_wisp_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WISP_FLAP = SOUNDS.register("entity_wisp_flap", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_WISP_HURT = SOUNDS.register("entity_wisp_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVITY_BLACK_HOLE = SOUNDS.register("gravity_black_hole", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_COMPLETE = SOUNDS.register("incubator_complete", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> INCUBATOR_HUM = SOUNDS.register("incubator_hum", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> INSTABILITY_RUPTURE = SOUNDS.register("instability_rupture", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> QUANTUM_ORE_BREAK = SOUNDS.register("quantum_ore_break", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_SHATTER = SOUNDS.register("rift_shatter", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_SHA_SHA_AMBIENT = SOUNDS.register("sha_sha_ambient", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_SHA_SHA_BLINK = SOUNDS.register("sha_sha_blink", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_SHA_SHA_DEATH = SOUNDS.register("sha_sha_death", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_SHA_SHA_HURT = SOUNDS.register("sha_sha_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_SHA_SHA_SLAM = SOUNDS.register("sha_sha_slam", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK = SOUNDS.register("ui_click", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_TAB = SOUNDS.register("ui_tab", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WEATHER_QUANTUM_SLEET = SOUNDS.register("weather_quantum_sleet", SoundEvent::createVariableRangeEvent);

    /** Looks up an event by id (e.g. {@code boss_voice_warden_intro}); null when there is no such event. */
    public static SoundEvent byId(String id) {
        for (var h : SOUNDS.getEntries()) if (h.getId().getPath().equals(id)) return h.get();
        return null;
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
        QuantumFalcon.LOGGER.info("⚡ Registered {} sound events", SOUNDS.getEntries().size());
    }
}
