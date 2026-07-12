package com.zexqm.rpgproject.rpg;

public enum RpgClass {
    WIZARD("wizard"),
    NINJA("ninja");

    private final String id;

    RpgClass(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
