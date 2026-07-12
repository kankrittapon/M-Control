package com.zexqm.rpgproject.rpg;

public record DerivedStats(double maxHealth, double maxMana, double attackPower, double magicPower,
                           double defense, double damageReduction, double accuracy, double evasion,
                           double criticalChance, double criticalDamage, double attackSpeed,
                           double castSpeed, double moveSpeed, double ccResistance) {}
