package com.matt.quantumfalcon.network;

import java.util.Set;

public class ClientSkillData {
    private static int availablePoints = 0;
    private static int spentPoints = 0;
    private static Set<String> unlockedSkills = new java.util.HashSet<>();
    
    public static void setAvailablePoints(int points) { availablePoints = points; }
    public static void setSpentPoints(int points) { spentPoints = points; }
    public static void setUnlockedSkills(Set<String> skills) { unlockedSkills = skills; }
    private static int highestHatched = 0, masteryMarks = 0;
    public static void setLineage(int hatched, int marks) { highestHatched = hatched; masteryMarks = marks; }
    public static int getHighestHatched() { return highestHatched; }
    public static int getMasteryMarks() { return masteryMarks; }
    public static int getAvailablePoints() { return availablePoints; }
    public static int getSpentPoints() { return spentPoints; }
    public static Set<String> getUnlockedSkills() { return unlockedSkills; }
    /** Forget the last world's points and skills (called on logout). */
    public static void reset() { availablePoints = 0; spentPoints = 0; unlockedSkills = new java.util.HashSet<>(); highestHatched = 0; masteryMarks = 0; }
}
