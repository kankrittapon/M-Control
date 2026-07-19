package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.mana.ManaProvider;
import com.zexqm.rpgproject.network.RpgNetwork;
import com.zexqm.rpgproject.network.SyncManaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

public final class ManaShieldService {
    public record Result(float remainingDamage, int manaSpent, float absorbedDamage) {}

    public static Result absorb(LivingEntity target, float incomingDamage) {
        if (incomingDamage <= 0) return new Result(Math.max(0, incomingDamage), 0, 0);
        RpgCombatState state = target.getCapability(RpgCombatStateProvider.DATA).orElse(null);
        if (state == null || state.manaShieldRatio() <= 0)
            return new Result(incomingDamage, 0, 0);
        var mana = target.getCapability(ManaProvider.MANA).orElse(null);
        if (mana == null || mana.getMana() <= 0) return new Result(incomingDamage, 0, 0);

        float requested = (float) (incomingDamage * state.manaShieldRatio());
        int spent = mana.drain((int) Math.ceil(requested));
        float absorbed = Math.min(requested, spent);
        float remaining = Math.max(0, incomingDamage - absorbed);
        if (target instanceof ServerPlayer player) {
            RpgNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncManaPacket(mana.getMana(), mana.getMaxMana()));
        }
        RpgProject.LOGGER.info("[RPG Mana Shield] target={} incoming={} ratio={} requested={} spent={} absorbed={} remaining={} mp={}/{}",
                target.getScoreboardName(), incomingDamage, state.manaShieldRatio(), requested, spent,
                absorbed, remaining, mana.getMana(), mana.getMaxMana());
        return new Result(remaining, spent, absorbed);
    }

    private ManaShieldService() {}
}
