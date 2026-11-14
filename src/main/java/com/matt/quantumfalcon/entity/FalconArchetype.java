package com.matt.quantumfalcon.entity;

import com.matt.quantumfalcon.QuantumFalcon;
import net.minecraft.resources.ResourceLocation;

/**
 * Body archetypes (tools/gen_falcon_archetypes.py): each variant uses Matthew's rig plus archetype accessory bones,
 * with its own proportions. Values: overall size, body width, body height, head size, wing span.
 */
public enum FalconArchetype {
    SLEEK(1.0f, 0.9f, 1.0f, 1.0f, 1.1f), STOCKY(1.08f, 1.16f, 1.06f, 1.1f, 0.92f), CRESTED(1.0f, 1.06f, 1.0f, 1.05f, 1.0f),
    HORNED(1.22f, 1.08f, 1.04f, 1.06f, 1.05f), ETHEREAL(0.95f, 0.92f, 1.0f, 1.0f, 1.14f), PHOENIX(1.1f, 0.98f, 1.0f, 1.0f, 1.16f),
    FUNGAL(1.0f, 1.1f, 1.02f, 1.04f, 0.96f),
    // v4 legendary / rift archetypes (tools/gen_falcon_v2.py extras_v4): unique silhouettes for the rarest variants
    SINGULARITY(1.25f, 1.06f, 1.04f, 1.06f, 1.12f), COSMIC(1.05f, 0.94f, 1.0f, 1.0f, 1.18f), ASCENDANT(1.18f, 1.0f, 1.02f, 1.02f, 1.2f),
    PRISM(1.06f, 0.96f, 1.0f, 1.02f, 1.08f);
    public final float size, bodyW, bodyH, head, wings;
    FalconArchetype(float size, float bodyW, float bodyH, float head, float wings) { this.size = size; this.bodyW = bodyW; this.bodyH = bodyH; this.head = head; this.wings = wings; }
    public static final java.util.Map<String, FalconArchetype> BY_VARIANT = java.util.Map.ofEntries(
        java.util.Map.entry("peregrine", SLEEK), java.util.Map.entry("crimson", SLEEK), java.util.Map.entry("stormrider", SLEEK), java.util.Map.entry("tempest", SLEEK),
        java.util.Map.entry("prairie", STOCKY), java.util.Map.entry("deepslate", STOCKY),
        java.util.Map.entry("tundra", CRESTED), java.util.Map.entry("coral", CRESTED),
        java.util.Map.entry("abyssal", HORNED), java.util.Map.entry("voidstalker", HORNED), java.util.Map.entry("singularity", SINGULARITY),
        java.util.Map.entry("cosmic", COSMIC), java.util.Map.entry("entangled_prism", PRISM), java.util.Map.entry("aetherwing", ETHEREAL),
        java.util.Map.entry("quantum_phoenix", ASCENDANT), java.util.Map.entry("soulfire", PHOENIX),
        java.util.Map.entry("mushroom", FUNGAL), java.util.Map.entry("warped", FUNGAL),
        // breed-only hybrids
        java.util.Map.entry("tidecaller", CRESTED), java.util.Map.entry("emberfrost", PHOENIX), java.util.Map.entry("sporewraith", FUNGAL),
        java.util.Map.entry("aurora_seraph", ETHEREAL), java.util.Map.entry("rift_reaver", HORNED), java.util.Map.entry("chronowing", SLEEK),
        java.util.Map.entry("nebula_prism", PRISM), java.util.Map.entry("paradox", ASCENDANT));
    public static FalconArchetype of(String variant) { return variant == null ? SLEEK : BY_VARIANT.getOrDefault(variant.toLowerCase(), SLEEK); }
    public static FalconArchetype of(QuantumFalconEntity e) { return e == null ? SLEEK : of(e.getVariant()); }
    public ResourceLocation geo() { return ResourceLocation.fromNamespaceAndPath(QuantumFalcon.MOD_ID, "geo/quantum_falcon_" + name().toLowerCase() + ".geo.json"); }
}

