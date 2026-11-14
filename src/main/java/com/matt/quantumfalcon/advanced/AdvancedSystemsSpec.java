/*
 * Decompiled with CFR 0.152.
 */
package com.matt.quantumfalcon.advanced;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class AdvancedSystemsSpec {
    public static String getCompleteSummary() {
        return "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n     PART 12 \u2014 ADVANCED SYSTEMS SPECIFICATION\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n\nSYSTEM COUNT: 4 Revolutionary Systems\n\n1. AURORA BOREALIS EVENT\n   - 5% trigger chance per night\n   - 3 minute duration\n   - 5 color variants\n   - Buff system (watch aurora)\n   - Collectible Aurora Essence\n   - 30 particles/tick\n\n2. QUANTUM STORM EVENT\n   - 2% trigger chance per day\n   - 5 minute duration\n   - 10 chaos event types\n   - High risk/reward\n   - Purple lightning\n   - Reality distortion\n\n3. NPC MEMORY SYSTEM\n   - 50 memories per player\n   - 7 day duration\n   - 10 interaction types\n   - Emotional impact tracking\n   - Dynamic greetings (6 tiers)\n   - Memory references\n\n4. FALCON GENETICS\n   - 63 total variants\n   - 9 base colors\n   - 7 patterns\n   - Mendelian inheritance\n   - 4 legendary combos\n   - Breeding system\n\nINNOVATION LEVEL: LEGENDARY\nCOMPLEXITY: EXTREME\nTOTAL FEATURES: 30+\n\nNEW GAMEPLAY:\n\u2713 Sky events (aurora, storm)\n\u2713 NPCs with memory (living psychology)\n\u2713 Genetic breeding (Mendelian)\n\u2713 Chaos mechanics (10 types)\n\u2713 Legendary hunting (1 in 100,000!)\n\nThese systems bring unparalleled depth to Minecraft!\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n";
    }

    public static class AuroraBorealisSpec {
        public static final String EVENT_NAME = "Aurora Borealis";
        public static final float TRIGGER_CHANCE = 0.05f;
        public static final int DURATION = 3600;
        public static final int COLORS = 5;

        public static String getBuffTable() {
            return "AURORA BUFF EFFECTS\n===================\n\nGREEN (Northern Lights):\n- Effect: Regeneration I\n- Duration: 10 seconds\n- Heals 1 heart over time\n\nBLUE (Azure Aurora):\n- Effect: Night Vision I\n- Duration: 10 seconds\n- See clearly at night\n\nPURPLE (Violet Veil):\n- Effect: Luck I\n- Duration: 10 seconds\n- Better loot drops\n\nPINK (Rose Radiance):\n- Effect: Damage Boost I\n- Duration: 10 seconds\n- +3 attack damage\n\nCYAN (Quantum Shimmer):\n- Effect: Quantum Resonance I\n- Duration: 10 seconds\n- +quantum item effectiveness\n\nREQUIREMENT:\n- Must look up (pitch < -30\u00b0)\n- Watching the aurora\n- Automatic detection\n- Notification on first buff\n";
        }

        public static String getSystemSummary() {
            return "AURORA BOREALIS EVENT\n=====================\n\nTRIGGER: 5% per night (check at dusk)\nDURATION: 3 minutes\nCOLORS: 5 variants\n\nVISUAL SPECTACLE:\n\u2713 30 particles/tick (curtain effect)\n\u2713 Wave motion (sin/cos)\n\u2713 5 color variants\n\u2713 Sparkle effects\n\u2713 30 blocks high\n\nGAMEPLAY:\n\u2713 Watch aurora = get buff\n\u2713 5 different buffs\n\u2713 Collectible Aurora Essence\n\u2713 Spawns every 30 seconds\n\u2713 Rare crafting material\n\nREWARDS:\n- 10 second buffs (various)\n- ~6 Aurora Essence per event\n- Beautiful visuals\n- Rare occurrence (5%)\n\nINNOVATION: Dynamic sky event!\nINSPIRATION: Real northern lights\n";
        }

        public static enum AuroraColor {
            GREEN(65416, "Northern Lights", "Regeneration"),
            BLUE(43775, "Azure Aurora", "Night Vision"),
            PURPLE(0xAA00FF, "Violet Veil", "Luck"),
            PINK(0xFF00AA, "Rose Radiance", "Damage Boost"),
            CYAN(65535, "Quantum Shimmer", "Quantum Resonance");

            public final int colorHex;
            public final String displayName;
            public final String buffEffect;

            private AuroraColor(int hex, String name, String effect) {
                this.colorHex = hex;
                this.displayName = name;
                this.buffEffect = effect;
            }
        }

        public static class MechanicsSpec {
            public static final float CHANCE_PER_NIGHT = 0.05f;
            public static final long TRIGGER_TIME = 13000L;
            public static final int TOTAL_DURATION = 3600;
            public static final int PARTICLES_PER_TICK = 30;
            public static final double AURORA_HEIGHT = 30.0;
            public static final double AURORA_RADIUS = 20.0;
            public static final int BUFF_CHECK_INTERVAL = 20;
            public static final float REQUIRED_PITCH = -30.0f;
            public static final int BUFF_DURATION = 200;
            public static final int ESSENCE_SPAWN_INTERVAL = 600;
            public static final double ESSENCE_FALL_SPEED = 0.05;

            public static String getDescription() {
                return "AURORA MECHANICS\n================\n\nTRIGGER:\n- 5% chance per night\n- Checks at dusk (13000 ticks)\n- Random color selected\n- Server-wide event\n\nDURATION:\n- 3 minutes (3600 ticks)\n- Visible to all players\n- Continuous particle display\n\nVISUAL EFFECTS:\n- 30 particles per tick\n- Curtain-like waves\n- Sin/cos wave motion\n- Height: 30 blocks up\n- Radius: 20 blocks\n- Color: Dust particles (RGB)\n- Sparkles: End rod (10% chance)\n\nWAVE MOTION:\n- Vertical curtains\n- Horizontal drift\n- Amplitude variation\n- Time-based animation\n\nPLAYER BUFFS:\n- Check every 1 second\n- Must look up (pitch < -30\u00b0)\n- Buff duration: 10 seconds\n- Effect varies by color\n- Notification on first buff\n- Happy particles around player\n\nCOLLECTIBLES:\n- Aurora Essence spawns every 30s\n- Falls slowly (0.05 speed)\n- Glowing tag (visible)\n- Near player (20 block radius)\n- Rare crafting material\n\nANNOUNCEMENTS:\n- Start: \"\ud83c\udf0c AURORA BOREALIS! \ud83c\udf0c\"\n- Color name displayed\n- End: \"The aurora fades...\"\n- Sound: Portal ambient\n";
            }
        }
    }

    public static class FalconGeneticsSpec {
        public static final String SYSTEM_NAME = "Falcon Genetics";
        public static final int BASE_COLORS = 9;
        public static final int PATTERNS = 7;
        public static final int TOTAL_VARIANTS = 63;

        public static String getSystemSummary() {
            return "FALCON GENETICS SYSTEM\n======================\n\nVARIANTS: 63 total (9 colors \u00d7 7 patterns)\nCOLORS: 9 (White to Emerald)\nPATTERNS: 7 (Solid to Galaxy)\nLEGENDARY: 4 combos (0.001% - 0.004%)\n\nGENETICS:\n\u2713 Mendelian inheritance\n\u2713 Dominant/recessive alleles\n\u2713 2 alleles per trait\n\u2713 Breeding system\n\u2713 Phenotype expression\n\nRARITY DISTRIBUTION:\n- Common: 50% (White, Gray)\n- Uncommon: 40% (Black, Brown)\n- Rare: 45% (Red, Blue, Green)\n- Very Rare: 5% (Gold)\n- Ultra Rare: 1% (Emerald)\n\nLEGENDARY FEATURES:\n\u2713 +100% all stats\n\u2713 Glowing effect\n\u2713 Golden nameplate\n\u2713 Server announcement\n\u2713 Rarest: 1 in 100,000!\n\nBREEDING:\n\u2713 Selective breeding possible\n\u2713 Trait tracking\n\u2713 Punnett square logic\n\u2713 Rare combo hunting\n\u2713 Collectible variants\n\nINNOVATION: Full genetic system!\nINSPIRATION: Real-world genetics\n";
        }

        public static class BreedingSpec {
            public static String getDescription() {
                return "MENDELIAN BREEDING MECHANICS\n=============================\n\nGENETICS:\n- Each falcon has 2 alleles per trait\n- Color: 2 alleles (e.g. WHITE/BLACK)\n- Pattern: 2 alleles (e.g. SOLID/STRIPED)\n\nINHERITANCE:\n- Each parent contributes 1 allele\n- Random selection (50% chance each)\n- Child gets 2 alleles total\n- Dominant/recessive rules apply\n\nDOMINANT/RECESSIVE:\n- If 1+ allele is dominant \u2192 express dominant\n- If both recessive \u2192 express either (50% chance)\n- Example: WHITE (D) + BLACK (R) = WHITE expressed\n- Example: BLACK (R) + BROWN (R) = BLACK or BROWN (50/50)\n\nBREEDING EXAMPLE:\nParent 1: WHITE/BLACK color, SOLID/STRIPED pattern\nParent 2: GRAY/RED color, SPOTTED/GRADIENT pattern\n\nChild possibilities:\n- Color allele from P1: WHITE or BLACK (50% each)\n- Color allele from P2: GRAY or RED (50% each)\n- Pattern allele from P1: SOLID or STRIPED (50% each)\n- Pattern allele from P2: SPOTTED or GRADIENT (50% each)\n\nPossible children:\n- WHITE/GRAY SOLID/SPOTTED\n- WHITE/GRAY SOLID/GRADIENT\n- WHITE/RED SOLID/SPOTTED\n- BLACK/GRAY STRIPED/GRADIENT\n- ... (16 total combinations)\n\nEXPRESSED PHENOTYPE:\n- WHITE or GRAY dominant for color\n- SOLID dominant for pattern\n- Likely: White Solid or Gray Solid\n\nLEGENDARY BREEDING:\n- Requires both parents have rare alleles\n- Gold \u00d7 Gold = possible Gold child\n- Phoenix \u00d7 Phoenix = possible Phoenix child\n- Gold Phoenix \u00d7 Gold Phoenix = chance at legendary!\n- Very low odds but possible with selective breeding\n";
            }
        }

        public static class ColorSpec {
            public static final String[] COLORS = new String[]{"WHITE (0xFFFFFF, Dominant, 25% spawn)", "BLACK (0x000000, Recessive, 20% spawn)", "BROWN (0x8B4513, Recessive, 20% spawn)", "GRAY (0x808080, Dominant, 25% spawn)", "RED (0xFF0000, Recessive, 15% spawn)", "BLUE (0x0000FF, Recessive, 15% spawn)", "GREEN (0x00FF00, Recessive, 15% spawn)", "GOLD (0xFFD700, Recessive, 5% spawn, RARE)", "EMERALD (0x50C878, Recessive, 1% spawn, ULTRA RARE)"};

            public static String getDescription() {
                return "FALCON COLORS (9)\n=================\n\nCOMMON (50%):\n1. WHITE (Dominant, 25%)\n2. GRAY (Dominant, 25%)\n\nUNCOMMON (40%):\n3. BLACK (Recessive, 20%)\n4. BROWN (Recessive, 20%)\n\nRARE (45%):\n5. RED (Recessive, 15%)\n6. BLUE (Recessive, 15%)\n7. GREEN (Recessive, 15%)\n\nVERY RARE (5%):\n8. GOLD (Recessive, 5%)\n\nULTRA RARE (1%):\n9. EMERALD (Recessive, 1%)\n\nGENETICS:\n- White & Gray: DOMINANT\n- All others: RECESSIVE\n- Mendelian inheritance\n- Two alleles per falcon\n- Expressed phenotype\n";
            }
        }

        public static class LegendarySpec {
            public static final String[] LEGENDARY_COMBOS = new String[]{"Emerald Masked (0.01% \u00d7 0.10 = 0.001%)", "Gold Galaxy (0.05% \u00d7 0.05 = 0.0025%)", "Emerald Phoenix (0.01% \u00d7 0.08 = 0.0008%)", "Gold Phoenix (0.05% \u00d7 0.08 = 0.004%)"};

            public static String getDescription() {
                return "LEGENDARY FALCON COMBOS\n=======================\n\n1. EMERALD MASKED:\n   - Rarity: 0.001% (1 in 100,000!)\n   - Rarest combo\n   - Stats: +100% all attributes\n   - Glowing effect\n   - Server announcement on birth\n\n2. GOLD GALAXY:\n   - Rarity: 0.0025% (1 in 40,000)\n   - Ultra rare\n   - Stats: +100% all attributes\n   - Glowing effect\n   - Server announcement\n\n3. EMERALD PHOENIX:\n   - Rarity: 0.0008% (1 in 125,000)\n   - Extremely rare\n   - Stats: +100% all attributes\n   - Glowing effect\n   - Server announcement\n\n4. GOLD PHOENIX:\n   - Rarity: 0.004% (1 in 25,000)\n   - Very rare\n   - Stats: +100% all attributes\n   - Glowing effect\n   - Server announcement\n\nLEGENDARY BONUSES:\n- Max HP: 40 (normal: 20)\n- Attack: 10 (normal: 5)\n- Flying Speed: 1.2\u00d7 (normal: 1.0\u00d7)\n- Permanent glow effect\n- Golden name tag\n- Server-wide birth announcement\n- Level-up sound on spawn\n";
            }
        }

        public static class PatternSpec {
            public static final String[] PATTERNS = new String[]{"SOLID (30% spawn, Dominant)", "STRIPED (20% spawn, Recessive)", "SPOTTED (15% spawn, Recessive)", "GRADIENT (12% spawn, Recessive)", "MASKED (10% spawn, Recessive)", "PHOENIX (8% spawn, Recessive, RARE)", "GALAXY (5% spawn, Recessive, ULTRA RARE)"};

            public static String getDescription() {
                return "FALCON PATTERNS (7)\n===================\n\nCOMMON (30%):\n1. SOLID (Dominant, 30%)\n   - Single uniform color\n\nUNCOMMON (20%):\n2. STRIPED (Recessive, 20%)\n   - Horizontal stripes\n\nRARE (15%):\n3. SPOTTED (Recessive, 15%)\n   - Leopard-like spots\n\nVERY RARE (12%):\n4. GRADIENT (Recessive, 12%)\n   - Color fade effect\n\nSUPER RARE (10%):\n5. MASKED (Recessive, 10%)\n   - Face mask pattern\n\nULTRA RARE (8%):\n6. PHOENIX (Recessive, 8%)\n   - Flame-like pattern\n\nLEGENDARY (5%):\n7. GALAXY (Recessive, 5%)\n   - Starry pattern\n\nGENETICS:\n- Only Solid is DOMINANT\n- All others are RECESSIVE\n- Two alleles per falcon\n- Breeding combinations\n";
            }
        }
    }

    public static class NPCMemorySpec {
        public static final String SYSTEM_NAME = "Advanced NPC Memory";
        public static final int MAX_MEMORIES = 50;
        public static final long MEMORY_DURATION = 168000L;
        public static final int MEMORY_TYPES = 10;

        public static String getSystemSummary() {
            return "NPC MEMORY SYSTEM\n=================\n\nCAPACITY: 50 memories per player\nDURATION: 7 days (168,000 ticks)\nTYPES: 10 interaction types\n\nFEATURES:\n\u2713 Persistent memory storage\n\u2713 Emotional impact tracking\n\u2713 Dynamic greetings (6 tiers)\n\u2713 Memory references in dialogue\n\u2713 Time-ago formatting\n\u2713 Auto-expiration (7 days)\n\u2713 Cumulative relationship\n\nINTERACTION RECORDING:\n\u2713 Conversations (+5)\n\u2713 Trades (+10)\n\u2713 Gifts (+20)\n\u2713 Quest completion (+30)\n\u2713 Attacks (-50, very negative!)\n\u2713 Helping (+25)\n\u2713 Defending (+40)\n\nPLAYER EXPERIENCE:\n- NPCs remember everything\n- Past actions matter\n- Relationship builds over time\n- Attack = long-term consequences\n- Gift = lasting friendship\n\nINNOVATION: Living NPC psychology!\nINSPIRATION: Skyrim, Stardew Valley\n";
        }

        public static class GreetingSpec {
            public static String getDescription() {
                return "DYNAMIC GREETINGS\n=================\n\nFIRST MEETING (no memories):\n- \"Greetings, stranger. I don't believe we've met.\"\n- Color: White\n- Neutral tone\n\nBEST FRIEND (impact \u2265 200):\n- \"Welcome back, dear friend! It's wonderful to see you again!\"\n- Color: Green\n- Enthusiastic\n\nFRIEND (impact \u2265 100):\n- \"Hello, friend! Good to see you.\"\n- Color: Green\n- Friendly\n\nACQUAINTANCE (impact \u2265 0):\n- \"Greetings. [memory reference]\"\n- Color: White\n- Polite + context\n\nDISLIKED (impact \u2265 -100):\n- \"Oh, it's you. [memory reference]\"\n- Color: Gray\n- Cold tone\n\nENEMY (impact < -100):\n- \"You again. I haven't forgotten what you did.\"\n- Color: Red\n- Hostile\n\nMEMORY REFERENCES:\n- Recent memory mentioned\n- Time-ago included\n- Context-specific\n- Examples:\n  \"We traded 2 hours ago.\"\n  \"Thank you again for the gift yesterday.\"\n  \"You completed that quest for me 3 days ago. Well done!\"\n  \"I remember you attacked me 1 day ago. Why should I trust you?\"\n";
            }
        }

        public static class MechanicsSpec {
            public static final int MAX_STORED = 50;
            public static final long DURATION_TICKS = 168000L;
            public static final long DURATION_HOURS = 168L;

            public static String getDescription() {
                return "MEMORY MECHANICS\n================\n\nSTORAGE:\n- Max 50 memories per player\n- Oldest removed first\n- Per-NPC tracking\n- Persistent data\n\nDURATION:\n- 7 days (168,000 ticks)\n- Auto-expire old memories\n- Checked on interaction\n- Real-time cleanup\n\nEMOTIONAL IMPACT:\n- Range: -50 to +40\n- Cumulative total tracked\n- Affects relationship\n- Influences dialogue\n\nTIMESTAMPS:\n- Tick-precise recording\n- Time-ago calculation\n- Days/hours/recently\n- Display format\n\nRETRIEVAL:\n- Recent memory (last)\n- All memories (list)\n- Total impact (sum)\n- Filtered by type\n\nDISPLAY:\n- Shift-click for full list\n- Last 10 shown\n- Color-coded by impact\n- Time-ago format\n- Memory type verb\n";
            }
        }

        public static class MemoryTypeSpec {
            public static final String[] TYPES = new String[]{"CONVERSATION (talked with you, +5 impact)", "TRADE (traded with you, +10 impact)", "GIFT (received a gift from you, +20 impact)", "QUEST_GIVEN (gave you a quest, +0 impact)", "QUEST_COMPLETED (you completed a quest, +30 impact)", "HELPED (you helped me, +25 impact)", "ATTACKED (you attacked me, -50 impact)", "DEFENDED (you defended me, +40 impact)", "WITNESSED (witnessed you, +0 impact)", "LOCATION (saw you at, +0 impact)"};

            public static String getDescription() {
                return "MEMORY TYPES (10)\n=================\n\nCONVERSATION:\n- Basic interaction\n- Emotional impact: +5\n- \"talked with you\"\n\nTRADE:\n- Exchanged items\n- Emotional impact: +10\n- \"traded with you\"\n\nGIFT:\n- Received present\n- Emotional impact: +20\n- \"received a gift from you\"\n- Shows item name\n\nQUEST_GIVEN:\n- Gave player quest\n- Emotional impact: +0\n- \"gave you a quest\"\n\nQUEST_COMPLETED:\n- Player completed quest\n- Emotional impact: +30\n- \"you completed a quest\"\n- Shows quest name\n\nHELPED:\n- Player helped NPC\n- Emotional impact: +25\n- \"you helped me\"\n\nATTACKED:\n- Player attacked NPC\n- Emotional impact: -50\n- \"you attacked me\"\n- Shows damage amount\n- VERY NEGATIVE\n\nDEFENDED:\n- Player defended NPC\n- Emotional impact: +40\n- \"you defended me\"\n\nWITNESSED:\n- NPC saw player action\n- Emotional impact: +0\n- \"witnessed you\"\n\nLOCATION:\n- Saw player at location\n- Emotional impact: +0\n- \"saw you at\"\n";
            }
        }
    }

    public static class QuantumStormSpec {
        public static final String EVENT_NAME = "Quantum Storm";
        public static final float TRIGGER_CHANCE = 0.02f;
        public static final int DURATION = 6000;
        public static final int CHAOS_EVENTS = 10;

        public static String getSystemSummary() {
            return "QUANTUM STORM EVENT\n===================\n\nTRIGGER: 2% per day (check at dawn)\nDURATION: 5 minutes\nCHAOS EVENTS: 10 types\n\nHIGH RISK/HIGH REWARD:\n\u2713 Positive events (40%):\n  - Item duplication\n  - XP boost\n  - Quantum chests\n  - Healing\n\n\u2713 Negative events (30%):\n  - Random teleport\n  - Damage\n  - Debuffs\n  - Hostile spawns\n\n\u2713 Chaotic events (30%):\n  - Gravity reversal\n  - Random blocks\n  - Mixed buffs\n\nFEATURES:\n\u2713 10 chaos event types\n\u2713 Event every 5 seconds\n\u2713 Purple lightning (every 2s)\n\u2713 Weather effects\n\u2713 Particle storm\n\nREWARDS:\n- Quantum chests (special loot)\n- Duplicated items (30% chance)\n- XP boosts (100-300)\n- Random valuable blocks\n\nDANGER LEVEL: EXTREME\nINNOVATION: Chaos incarnate!\n";
        }

        public static class ChaosEventSpec {
            public static final String[] EVENT_TYPES = new String[]{"Random Teleport (50 block radius)", "Random Buff/Debuff (8 effects)", "Item Duplication (30% chance)", "Random Damage (5 hearts)", "Spawn Quantum Chest (loot)", "Gravity Reversal (invert velocity)", "XP Boost (100-300 XP)", "Random Blocks (5\u00d7 random materials)", "Spawn Quantum Anomaly (hostile)", "Heal Player (10 hearts)"};

            public static String getDescription() {
                return "CHAOS EVENTS (10 TYPES)\n=======================\n\n1. RANDOM TELEPORT:\n   - 50 block radius\n   - Random direction\n   - Safe landing check\n   - Message: \"Reality shifts!\"\n\n2. RANDOM BUFF/DEBUFF:\n   - 8 possible effects\n   - Speed, Jump, Regen, Strength\n   - Resistance, Weakness, Slowness, Levitation\n   - 30 second duration\n   - Message: \"Quantum energy surges!\"\n\n3. ITEM DUPLICATION:\n   - 30% success chance\n   - Held item copied\n   - Added to inventory\n   - Message: \"Quantum duplication successful!\"\n\n4. RANDOM DAMAGE:\n   - 5 hearts magic damage\n   - Message: \"Quantum backlash!\"\n\n5. SPAWN QUANTUM CHEST:\n   - 10 block radius\n   - Special loot table\n   - Quantum storm drops\n   - Message: \"Quantum treasure materialized!\"\n\n6. GRAVITY REVERSAL:\n   - Invert velocity\n   - Launched upward if falling\n   - Disorienting effect\n   - Message: \"Gravity inverts!\"\n\n7. XP BOOST:\n   - 100-300 experience points\n   - Random amount\n   - Message: \"Quantum knowledge absorbed!\"\n\n8. RANDOM BLOCKS:\n   - 5 blocks placed\n   - Diamond, Gold, Emerald, Coal, TNT, Quantum\n   - 5 block radius\n   - Message: \"Matter coalesces!\"\n\n9. SPAWN QUANTUM ANOMALY:\n   - Hostile entity\n   - 10 block radius\n   - Dangerous enemy\n   - Message: \"A quantum anomaly appears!\"\n\n10. HEAL PLAYER:\n    - Restore 10 hearts\n    - Full heal possible\n    - Message: \"Quantum energy heals you!\"\n";
            }
        }

        public static class MechanicsSpec {
            public static final float CHANCE_PER_DAY = 0.02f;
            public static final long TRIGGER_TIME = 0L;
            public static final int TOTAL_DURATION = 6000;
            public static final int CHAOS_INTERVAL = 100;
            public static final int CHAOS_EVENT_COUNT = 10;
            public static final int LIGHTNING_INTERVAL = 40;
            public static final double LIGHTNING_RADIUS = 30.0;

            public static String getDescription() {
                return "QUANTUM STORM MECHANICS\n=======================\n\nTRIGGER:\n- 2% chance per day\n- Checks at dawn (time 0)\n- Server-wide event\n- Weather changes to thunder\n\nDURATION:\n- 5 minutes (6000 ticks)\n- Continuous chaos\n- High intensity\n\nCHAOS EVENTS:\n- Trigger every 5 seconds\n- Random from 10 types\n- Affects all players\n- High risk/reward\n\nQUANTUM LIGHTNING:\n- Purple lightning bolts\n- Every 2 seconds\n- 30 block radius\n- 50% visual only\n- 50% deal damage\n- Quantum particles on strike\n\nSTORM PARTICLES:\n- Chaotic swirls\n- 5 per tick\n- Random angles\n- 5-15 block radius\n- Chaos spark particles\n\nWEATHER:\n- Thunder + rain\n- Auto-set on start\n- Clears on end\n- Atmospheric\n\nANNOUNCEMENTS:\n- Start: \"\u26a1 QUANTUM STORM INCOMING! \u26a1\"\n- Warning: \"Reality itself is unstable...\"\n- Events: Individual notifications\n- End: \"The quantum storm subsides...\"\n- Sound: Lightning thunder\n";
            }
        }
    }
}
