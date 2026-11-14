package com.matt.quantumfalcon.dungeon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Original lore fragments for the dungeon lecterns, keyed "<realm>_<0..2>". */
public final class LoreBooks {
    private LoreBooks() {}

    public record Entry(String title, List<String> pages) {}

    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    private static void add(String key, String title, String... pages) { ENTRIES.put(key, new Entry(title, List.of(pages))); }

    static {
        add("riftslate_0", "Surveyor's Note",
            "We cut the first hall where the slate hummed loudest. The falcons would not enter. They circled the shaft until dusk, then roosted on the hut roof facing east, every one of them.",
            "Lio says the stone remembers being sky. I say it remembers being cut.");
        add("riftslate_1", "Work Order 14", "Seal the lower vault. Post one construct at the inner door with the key. Do not give it a name. Named constructs wander.");
        add("riftslate_2", "Scratched Into the Lintel", "IF YOU CAN READ THIS THE DOOR STILL WORKS. TAKE THE LEVER PATH HOME.");

        // A10 Frozen Hours: Eastwatch Aerie
        add("eastwatch_ledger", "Roost Ledger",
            "Morning lesson, as every morning: perch, hood off, face east. The fledglings ask why east. The answer is in the ledger's first line, written before any of us: east, because that is where the stitch will be.",
            "We did not know what a stitch was. We taught the birds to watch for it anyway. Today the whole flight went up at once, every head turned the same way, and Irel lifted her arm, and the light came from the east.",
            "If you are reading this, the lesson held. Ring the three bells. They were hung in the good hour and they remember it.");
        add("eastwatch_egg", "Hatching Note",
            "This egg was laid the night before the morning flight. It has been waiting a long time.",
            "Take it, but do not hatch it here. A bird hatched in a held hour wakes up facing east and never turns round. Carry it far from Eastwatch, somewhere with an ordinary morning.");

        // story review H: Irel's lesson rules, and two endgame pages (finishes chronoslate_2's line)
        add("eastwatch_irel", "Flightmaster's Slate",
            "First flight at first light. East window. No exceptions.",
            "If the light is late, wait. It has never been late.");
        add("sanctum_morning", "The Last Page",
            "We wrote: a later visitor, who is also you. We ran out of ink, not of certainty.",
            "We could not mend the hour. We built a door instead, and left it for whoever came after us with a bird on their arm.",
            "We called that person morning. Not every hour came back. Look up.");
        add("overseer_altar", "The High Seat's Order",
            "By order of the High Seat: the dawn ceremony is postponed. One more night.",
            "(below, in a shakier hand) Postponed again. Everyone is so happy. Why would anyone want it to end?");

        add("aurorite_0", "Cold Psalm",
            "Light falls slowly here. You can watch a sunrise pour down the walls like honey and pool around your boots.",
            "The keepers learned to read the colour of the ice. Green for safe. Violet for listen. White for run.");
        add("aurorite_1", "Keeper's Ledger", "Three strays seen near the lower halls. Arrows of frozen light. The guardian at the vault was quiet today, which is worse.");
        add("aurorite_2", "A Child's Drawing", "A falcon made of ribbons, drawn in chalk. Beneath it: 'she flew into the aurora and came back brighter'.");

        add("prismite_0", "On Refraction",
            "Every surface splits you into seven. The phantoms are what is left when one of the seven does not come back.",
            "Keep a lever in your pocket of thought. True locks answer to the hand that is patient.");
        add("prismite_1", "Puzzle Wright's Boast", "Four of my levers lie. The fifth sits under the sigil and tells the truth. A thief pulls them all. A falconer looks first.");
        add("prismite_2", "Burnt Page", "...the stalker wears the light like a coat. Do not fight it in the open hall...");

        add("cinderite_0", "Forge Liturgy",
            "We fed the vault-heart coal for a hundred years and it gave us doors that cannot be forced.",
            "The blazes were never invited. They simply liked the warmth.");
        add("cinderite_1", "Quartermaster's Warning", "Netherite at minimum past the second hall. The plates in the trap corridor are armed day and night.");
        add("cinderite_2", "Ash Prayer", "Let the key be heavy. Let the door be heavier. Let the one who carries both walk out.");

        add("chronoslate_0", "Clockwright's Diary",
            "Day 1. Day 1. Day 1. The corridor loops if you hurry. Walk slowly and it lets you through.",
            "The constructs here move a beat behind the world. Strike where they are going.");
        add("chronoslate_1", "Echo Log", "A void echo stands guard at the vault. It is our own warden from tomorrow, sent back to be early.");
        add("chronoslate_2", "Unfinished Line", "When the treasury opens, take only what fits in one hand. The rest belongs to a later visitor, who is also y");

        add("abyssite_0", "Deep Survey",
            "Below the third shaft the lamps dim by themselves. Not from lack of fuel. From modesty.",
            "Something down here prefers the dark and is polite about it.");
        add("abyssite_1", "Rope Tally", "Rope out: 40. Rope back: 31. Men out: 6. Men back: 6. We have stopped asking where the rope goes.");
        add("abyssite_2", "Warden's Oath", "I keep the key. I keep the door. I keep the dark. Bring light and I will keep that too.");

        add("voidshale_0", "Edge Notes",
            "The walls here end in nothing. Not a drop. Nothing. Lean too far and the world forgets your shape.",
            "The falcons hatched near the edge never cry. They watch.");
        add("voidshale_1", "Vault Ward", "Two elites. One for the key, one for the hoard. If you see the second before the first, you took the wrong stairs.");
        add("voidshale_2", "Last Entry", "The return rift hums like a held note. Step through and the surface catches you.");

        add("singularite_0", "The Point",
            "All halls lead inward here. Even the ones that go up.",
            "We built the vault around the singularity's shadow because the shadow is the only part that stays still.");
        add("singularite_1", "Sovereign Fragment", "Quantum-tier gear or do not come. The guardians here were made from the last falconers who ignored this sign.");
        add("singularite_2", "Gift", "To whoever opens the treasury: the egg in the vault is the only thing here that was not built. Hatch it far away from this place.");
    }

    public static Entry get(String key) {
        return ENTRIES.getOrDefault(key, new Entry("Faded Page", List.of("The ink has worn away.")));
    }

    public static int size() { return ENTRIES.size(); }
}
