package com.zexqm.rpgproject.client;

public final class ClientRpgData {
    private static boolean weaponDrawn;
    private static boolean inCombat;
    private static int level = 1;
    private static long experience;
    private static long requiredExperience;
    private static int skillPoints;
    private static double stamina;
    private static double maxStamina;
    private static int breathLevel = 1, strengthLevel = 1, healthLevel = 1;
    public static boolean weaponDrawn() { return weaponDrawn; }
    public static boolean inCombat() { return inCombat; }
    public static int level() { return level; }
    public static long experience() { return experience; }
    public static long requiredExperience() { return requiredExperience; }
    public static int skillPoints() { return skillPoints; }
    public static double stamina() { return stamina; }
    public static double maxStamina() { return maxStamina; }
    public static int breathLevel() { return breathLevel; }
    public static int strengthLevel() { return strengthLevel; }
    public static int healthLevel() { return healthLevel; }
    public static void set(boolean drawn, boolean combat, int newLevel, long exp, long required, int points,
                           double currentStamina, double staminaMaximum, int breath, int strength, int health) {
        weaponDrawn = drawn; inCombat = combat; level = newLevel; experience = exp;
        requiredExperience = required; skillPoints = points;
        stamina = currentStamina; maxStamina = staminaMaximum;
        breathLevel = breath; strengthLevel = strength; healthLevel = health;
    }
    private ClientRpgData() {}
}
