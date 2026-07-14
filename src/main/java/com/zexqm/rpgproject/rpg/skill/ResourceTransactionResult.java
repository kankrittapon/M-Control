package com.zexqm.rpgproject.rpg.skill;

public record ResourceTransactionResult(Status status, int recovered, int drained, int transferred) {
    public enum Status { APPLIED, NO_EFFECT, UNSUPPORTED_TARGET }
}
