package com.zexqm.rpgproject.mana;

public final class ClientMana {
    private static int mana = 100;
    private static int maxMana = 100;

    public static int mana() {
        return mana;
    }

    public static int maxMana() {
        return maxMana;
    }

    public static void set(int newMana, int newMaxMana) {
        mana = newMana;
        maxMana = Math.max(1, newMaxMana);
    }

    private ClientMana() {
    }
}
