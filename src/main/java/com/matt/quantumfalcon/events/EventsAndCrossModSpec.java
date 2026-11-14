/*
 * Decompiled with CFR 0.152.
 */
package com.matt.quantumfalcon.events;

import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
public class EventsAndCrossModSpec {
    public static String getCompleteSummary() {
        return "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n     EVENTS & CROSS-MOD \u2014 COMPLETE SPECIFICATION\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n\nWORLD EVENTS (10):\n\u2713 Meteor Shower (resource rain!)\n\u2713 Rift Opening (invasion!)\n\u2713 Crystal Bloom (farming bonus)\n\u2713 Void Invasion (5 waves, extreme)\n\u2713 Time Storm (chaos!)\n\u2713 5 more minor events\n\nBOSS EVENTS:\n\u2713 3 summon rituals\n\u2713 3 difficulty modes (Normal/Heroic/Mythic)\n\u2713 Scaled rewards\n\u2713 Achievements\n\nCROSS-MOD COMPATIBILITY (15+ mods):\n\u2713 JEI (recipes)\n\u2713 Curios (equipment)\n\u2713 Patchouli (guide)\n\u2713 Create (automation)\n\u2713 Farmer's Delight (food)\n\u2713 10+ more integrations\n\nWORLD FEELS ALIVE:\n- Events happen naturally\n- Announcements to all players\n- Rewards for participation\n- Danger and excitement\n\nPLAYS WELL WITH OTHERS:\n- 15+ mod integrations\n- Soft dependencies\n- Optional features\n- No conflicts\n\nTOTAL FEATURES: 30+ (events + integrations)\nINNOVATION LEVEL: MAXIMUM\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n";
    }

    public static class BossEventsSpec {
        public static String getSummary() {
            return "BOSS EVENTS SUMMARY\n===================\n\nBOSS COUNT: 3 legendary bosses\n\nSUMMON RITUALS:\n\u2713 Quantum Guardian (4 cores)\n\u2713 Void Horror (pentagram)\n\u2713 Singularity Sovereign (reality gem)\n\nDIFFICULTY MODES:\n\u2713 Normal (1\u00d7)\n\u2713 Heroic (1.5\u00d7)\n\u2713 Mythic (3\u00d7)\n\nFEATURES:\n- Dramatic summon animations\n- Announcements to all players\n- Boss music\n- Scaled rewards\n- Achievements\n\nREPLAYABILITY: Fight bosses multiple times!\nCHALLENGE: Mythic mode for hardcore players!\n";
        }

        public static class ChallengeModeSpec {
            public static final String[] CHALLENGE_MODES = new String[]{"NORMAL \u2014 Standard difficulty", "HEROIC \u2014 2\u00d7 HP, 1.5\u00d7 damage, new attacks", "MYTHIC \u2014 5\u00d7 HP, 2\u00d7 damage, all mechanics, enrage timer"};

            public static String getDescription() {
                return "BOSS CHALLENGE MODES\n====================\n\nNORMAL MODE:\n- Standard boss fight\n- Base HP and damage\n- Core mechanics only\n- Standard loot\n\nHEROIC MODE:\n- 2\u00d7 HP\n- 1.5\u00d7 damage\n- Additional attack patterns\n- Better loot (1.5\u00d7 drops)\n- Unique heroic drops\n\nMYTHIC MODE:\n- 5\u00d7 HP\n- 2\u00d7 damage\n- All mechanics active\n- Enrage timer (10 minutes)\n- BEST loot (3\u00d7 drops)\n- Legendary mythic drops\n- Achievement + title\n\nSELECTION:\n- Right-click boss with difficulty item\n- Normal: Nothing (default)\n- Heroic: Diamond Block\n- Mythic: Nether Star\n\nREWARDS SCALE WITH DIFFICULTY!\n";
            }
        }

        public static class SummonRitualSpec {
            public static final Map<String, RitualSpec> RITUALS = Map.of("quantum_guardian", new RitualSpec("Quantum Guardian Summon", "Place 4 Quantum Cores on Quantum Altar", List.of("4\u00d7 Quantum Core", "1\u00d7 Quantum Altar"), "Quantum Forest", "The Guardian awakens!"), "void_horror", new RitualSpec("Void Horror Summon", "Place 10 Void Fragments in pentagram pattern", List.of("10\u00d7 Void Fragment", "5\u00d7 Obsidian"), "Void Crater", "The void consumes all..."), "singularity_sovereign", new RitualSpec("Singularity Sovereign Summon", "Activate Singularity Core with Reality Gem", List.of("1\u00d7 Reality Gem", "1\u00d7 Singularity Core", "64\u00d7 Diamond"), "Singularity Wastes", "Reality itself trembles!"));

            public static String getDescription() {
                return "BOSS SUMMON RITUALS\n===================\n\nQUANTUM GUARDIAN:\n- Materials: 4\u00d7 Quantum Core\n- Setup: Place on Quantum Altar\n- Location: Quantum Forest\n- Announcement: \"The Guardian awakens!\"\n\nVOID HORROR:\n- Materials: 10\u00d7 Void Fragment, 5\u00d7 Obsidian\n- Setup: Pentagram pattern\n- Location: Void Crater\n- Announcement: \"The void consumes all...\"\n\nSINGULARITY SOVEREIGN:\n- Materials: Reality Gem, Singularity Core, 64 Diamonds\n- Setup: Activate core with gem\n- Location: Singularity Wastes\n- Announcement: \"Reality itself trembles!\"\n\nEFFECTS:\n- Dramatic spawn animation\n- Sound effects\n- Particle effects\n- Boss music starts\n- All players notified\n\nFAILURE: Materials consumed even if failed\n";
            }

            public static class RitualSpec {
                public final String name;
                public final String description;
                public final List<String> materials;
                public final String location;
                public final String announcement;

                public RitualSpec(String name, String desc, List<String> mats, String loc, String announce) {
                    this.name = name;
                    this.description = desc;
                    this.materials = mats;
                    this.location = loc;
                    this.announcement = announce;
                }
            }
        }
    }

    public static class CrossModSpec {
        public static final Map<String, String> OTHER_MODS = Map.of("The Twilight Forest", "Portal connection, shared bosses", "Ice and Fire", "Falcon vs Dragon mechanics", "Minecolonies", "Villager professions integration", "Sophisticated Backpacks", "Falcon saddle bags", "Iron Chests", "Quantum chest upgrades", "Waystones", "Quantum teleportation", "Jade/WTHIT", "Entity info display", "The One Probe", "Block info tooltips", "FTB Quests", "Alternative quest system", "Carry On", "Carry falcons in hand");

        public static String getSummary() {
            return "CROSS-MOD COMPATIBILITY SUMMARY\n===============================\n\nMAJOR INTEGRATIONS (5):\n\u2713 JEI \u2014 Recipe viewer\n\u2713 Curios \u2014 Equipment slots\n\u2713 Patchouli \u2014 Guide book\n\u2713 Create \u2014 Automation\n\u2713 Farmer's Delight \u2014 Food\n\nMINOR INTEGRATIONS (10+):\n- Twilight Forest\n- Ice and Fire\n- Minecolonies\n- And more!\n\nTOTAL COMPATIBLE MODS: 15+\n\nIMPLEMENTATION:\n- Soft dependencies (optional)\n- Runtime detection\n- Graceful degradation\n- No hard requirements\n\nPHILOSOPHY: Play nice with others!\nPLAYER BENEFIT: Works with favorite mods!\n";
        }

        public static class CreateSpec {
            public static final String MOD_NAME = "Create";
            public static final String VERSION = "0.5+";

            public static String getDescription() {
                return "CREATE MOD INTEGRATION\n======================\n\nCOMPATIBLE RECIPES:\n\u2713 Crystal crushing (Crushing Wheel)\n\u2713 Quantum shard mixing (Mechanical Mixer)\n\u2713 Automated falcon feeding\n\u2713 Crystal farm automation\n\nNEW RECIPES:\n- Crush Echo Crystals \u2192 Quantum Dust\n- Mix Quantum Dust + Water \u2192 Liquid Quantum\n- Press Quantum Shards \u2192 Quantum Plates\n\nAUTOMATION:\n- Mechanical arms for crystal harvesting\n- Belt systems for processing\n- Deployers for falcon care\n\nPLAYER BENEFIT: Automate everything!\n";
            }
        }

        public static class CuriosSpec {
            public static final String MOD_NAME = "Curios API";
            public static final String VERSION = "5.0+";

            public static String getDescription() {
                return "CURIOS API INTEGRATION\n======================\n\nCURIO SLOTS:\n- RING: 2 slots\n  Items: Quantum Ring, Void Ring\n- NECKLACE: 1 slot\n  Items: Echo Pendant, Time Amulet\n- BELT: 1 slot\n  Items: Utility Belt, Tool Belt\n- CHARM: 2 slots\n  Items: Lucky Charm, Falcon Charm\n\nCURIO ITEMS (8 total):\n1. Quantum Ring \u2014 +10% quantum damage\n2. Void Ring \u2014 Void immunity\n3. Echo Pendant \u2014 +20% XP\n4. Time Amulet \u2014 Slow falling\n5. Utility Belt \u2014 +9 inventory slots\n6. Tool Belt \u2014 Quick tool swap\n7. Lucky Charm \u2014 +10% luck\n8. Falcon Charm \u2014 Falcon buffs\n\nIMPLEMENTATION:\n- Register curio types\n- Add curio capabilities\n- Implement effects\n\nPLAYER BENEFIT: More equipment slots!\n";
            }
        }

        public static class FarmersDelightSpec {
            public static final String MOD_NAME = "Farmer's Delight";
            public static final String VERSION = "1.2+";

            public static String getDescription() {
                return "FARMER'S DELIGHT INTEGRATION\n============================\n\nNEW FOODS (5):\n1. Quantum Stew \u2014 Regeneration III, 20s\n2. Echo Salad \u2014 Speed II, 30s\n3. Void Soup \u2014 Night Vision, 60s\n4. Falcon Feast \u2014 All stats +1, 60s\n5. Crystal Candy \u2014 Saturation, 10s\n\nCOOKING POT RECIPES:\n- Quantum Stew: Echo Crystals + vegetables\n- Echo Salad: Resonance Flowers + greens\n- Void Soup: Void Fragments + mushrooms\n\nCUTTING BOARD:\n- Falcon Meat \u2192 3\u00d7 Falcon Strips\n- Quantum Fish \u2192 2\u00d7 Quantum Fillets\n\nPLAYER BENEFIT: Better food options!\n";
            }
        }

        public static class JEISpec {
            public static final String MOD_NAME = "Just Enough Items (JEI)";
            public static final String VERSION = "Any";

            public static String getDescription() {
                return "JEI INTEGRATION\n===============\n\nFEATURES:\n\u2713 All recipes shown in JEI\n\u2713 Custom recipe categories:\n  - Quantum Crafting\n  - Falcon Breeding\n  - Crystal Growing\n  - Alchemy\n\u2713 Item descriptions\n\u2713 Usage recipes\n\u2713 Bookmarks support\n\nIMPLEMENTATION:\n- Register all custom recipes\n- Add recipe handlers\n- Show loot tables (optional)\n- Quest integration hints\n\nPLAYER BENEFIT: Easy recipe lookup!\n";
            }
        }

        public static class PatchouliSpec {
            public static final String MOD_NAME = "Patchouli";
            public static final String VERSION = "Any";

            public static String getDescription() {
                return "PATCHOULI INTEGRATION\n=====================\n\nGUIDE BOOK: \"Quantum Falcon Manual\"\n\nCHAPTERS (10):\n1. Getting Started\n2. Biomes Guide\n3. Falcon Taming & Breeding\n4. Crystals & Materials\n5. Crafting & Progression\n6. NPCs & Trading\n7. Quests & Adventure\n8. Magic & Spells\n9. Bosses & Combat\n10. Structures & Exploration\n\nPAGES: 100+ total\nILLUSTRATIONS: Custom artwork\nRECIPES: Integrated with JEI\n\nUNLOCKING:\n- Starts with basics\n- Unlocks as player progresses\n- Quest rewards unlock pages\n\nCRAFTING:\n- 1\u00d7 Book\n- 1\u00d7 Echo Crystal\n- 1\u00d7 Feather\n\nPLAYER BENEFIT: Complete in-game guide!\n";
            }
        }
    }

    public static class WorldEventsSpec {
        public static final int TOTAL_EVENTS = 10;
        public static final Map<String, String> OTHER_EVENTS = Map.of("Falcon Migration", "Wild falcons gather (easy taming, 15min)", "Quantum Surge", "Power boost for quantum items (20min)", "Probability Flux", "Random effects on players (10min)", "Echo Resonance", "All echoes louder, bonus XP (30min)", "Gravity Anomaly", "Reduced/increased gravity (15min)");

        public static String getSummary() {
            return "WORLD EVENTS SUMMARY\n====================\n\nTOTAL EVENTS: 10\n\nMAJOR EVENTS (5):\n1. Meteor Shower (5min, 1/hour)\n2. Rift Opening (10min, 1/2hr)\n3. Crystal Bloom (1 day, 1/3hr)\n4. Void Invasion (10min, 5 waves)\n5. Time Storm (10min, chaos!)\n\nMINOR EVENTS (5):\n6. Falcon Migration\n7. Quantum Surge\n8. Probability Flux\n9. Echo Resonance\n10. Gravity Anomaly\n\nANNOUNCEMENT SYSTEM:\n- Chat message to all players\n- Title screen popup\n- Sound effect\n- Boss bar (for timed events)\n\nREWARDS: Scaled by event difficulty\nDANGER: Varies (low to extreme)\n\nEvents make the world feel ALIVE!\n";
        }

        public static class CrystalBloomSpec {
            public static final String NAME = "Crystal Bloom";
            public static final int DURATION = 1200;
            public static final int FREQUENCY = 216000;

            public static String getDescription() {
                return "CRYSTAL BLOOM\n=============\n\nTRIGGER: Random (1/3 hours)\nDURATION: 1 Minecraft day\nLOCATION: Crystalline Peaks biome\n\nEFFECT:\n- Crystals grow faster (2\u00d7 speed)\n- Extra crystals spawn\n- Glowing particles everywhere\n\nBONUSES:\n- 2\u00d7 crystal growth\n- 50% chance of bonus crystal\n- Resonance Flowers bloom\n\nANNOUNCEMENT: \"The crystals sing with energy!\"\nVISUAL: Beautiful particle effects\nSOUND: Chime sounds\n\nBEST TIME: Farm crystals during bloom!\n";
            }
        }

        public static class MeteorShowerSpec {
            public static final String NAME = "Quantum Meteor Shower";
            public static final int DURATION = 300;
            public static final int FREQUENCY = 72000;
            public static final int METEOR_COUNT = 20;

            public static String getDescription() {
                return "QUANTUM METEOR SHOWER\n=====================\n\nTRIGGER: Random (1/hour chance)\nDURATION: 5 minutes\nEFFECT: 20 meteors fall from sky\n\nMETEOR TYPES:\n- Echo Meteor (drops Echo Crystals)\n- Quantum Meteor (drops Quantum Shards)\n- Void Meteor (drops Void Fragments)\n\nIMPACT:\n- Creates small crater\n- Drops valuable resources\n- Light show effect\n\nANNOUNCEMENT: \"The sky shimmers with quantum energy!\"\nSOUND: Whooshing + explosion\nPARTICLES: Trail particles\n\nREWARDS:\n- 3-8 crystals per meteor\n- Rare: Meteor Core (legendary item)\n";
            }
        }

        public static class RiftOpeningSpec {
            public static final String NAME = "Dimensional Rift";
            public static final int DURATION = 600;
            public static final int FREQUENCY = 144000;

            public static String getDescription() {
                return "DIMENSIONAL RIFT\n================\n\nTRIGGER: Random (1/2 hours)\nDURATION: 10 minutes\nLOCATION: Random in Echo Depth\n\nEFFECT:\n- Portal opens to Void Realm\n- Hostile mobs spawn (10-20)\n- Rare items drop from portal\n\nMOB SPAWNS:\n- Void Phantoms (5-10)\n- Echo Wraiths (3-5)\n- Void Lurkers (2-3)\n\nREWARDS:\n- Rift Essence (portal drops)\n- Void items\n- Dimensional Keys\n\nANNOUNCEMENT: \"A rift tears open nearby!\"\nDANGER LEVEL: HIGH\n";
            }
        }

        public static class TimeStormSpec {
            public static final String NAME = "Temporal Storm";
            public static final int DURATION = 600;
            public static final int FREQUENCY = 216000;

            public static String getDescription() {
                return "TEMPORAL STORM\n==============\n\nTRIGGER: Random (1/3 hours)\nDURATION: 10 minutes\nLOCATION: Temporal Anomaly biome\n\nEFFECTS:\n- Time randomly speeds up/slows down\n- Entities age/de-age\n- Random teleportation\n- Time crystals spawn\n\nTIME EFFECTS:\n- 50% chance: Time speed \u00d72\n- 30% chance: Time speed \u00d70.5\n- 20% chance: Time freeze (2 seconds)\n\nHAZARDS:\n- Random tp (5 block radius)\n- Aging damage (1 heart/10s)\n- Disorientation\n\nREWARDS:\n- Time Essence (5-10)\n- Chronos Crystals (2-5)\n- Aged Materials (rare)\n\nANNOUNCEMENT: \"Time itself unravels!\"\nVISUAL: Rippling air, particle distortion\n";
            }
        }

        public static class VoidInvasionSpec {
            public static final String NAME = "Void Invasion";
            public static final int DURATION = 600;
            public static final int FREQUENCY = 288000;
            public static final int WAVE_COUNT = 5;

            public static String getDescription() {
                return "VOID INVASION\n=============\n\nTRIGGER: Random (1/4 hours)\nDURATION: 10 minutes\nWAVES: 5 (2 minutes each)\n\nPROGRESSION:\n- Wave 1: 5 Void Phantoms\n- Wave 2: 8 Void Phantoms + 2 Wraiths\n- Wave 3: 10 Phantoms + 3 Wraiths\n- Wave 4: 15 Phantoms + 5 Wraiths + 1 Lurker\n- Wave 5: 20 Phantoms + 10 Wraiths + 3 Lurkers\n\nDIFFICULTY: Scales with waves\nTARGET: Player's location\n\nREWARDS (Per Wave):\n- Wave 1: 10 Void Fragments\n- Wave 2: 20 Fragments + Void Shard\n- Wave 3: 30 Fragments + 2 Shards\n- Wave 4: 50 Fragments + 5 Shards + Void Core\n- Wave 5: 100 Fragments + 10 Shards + Void Heart!\n\nANNOUNCEMENT: \"The void hungers for your soul!\"\nDANGER: EXTREME\nVICTORY TITLE: \"Void Vanquisher\"\n";
            }
        }
    }
}
