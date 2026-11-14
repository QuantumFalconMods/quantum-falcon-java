package com.matt.quantumfalcon.network;

import java.util.HashMap;
import java.util.Map;

public class ClientQuestData {
    private static java.util.List<String> activeQuests = new java.util.ArrayList<>();
    private static java.util.List<String> completedQuests = new java.util.ArrayList<>();
    private static Map<String, Map<Integer, Integer>> objectiveProgress = new HashMap<>();
    
    public static void setActiveQuests(java.util.List<String> quests) { activeQuests = quests; }
    public static void setCompletedQuests(java.util.List<String> quests) { completedQuests = quests; }
    public static void setObjectiveProgress(Map<String, Map<Integer, Integer>> progress) { objectiveProgress = progress; }
    public static java.util.List<String> getActiveQuests() { return activeQuests; }
    public static java.util.List<String> getCompletedQuests() { return completedQuests; }
    public static Map<String, Map<Integer, Integer>> getObjectiveProgress() { return objectiveProgress; }
}
