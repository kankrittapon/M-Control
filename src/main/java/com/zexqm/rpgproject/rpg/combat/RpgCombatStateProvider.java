package com.zexqm.rpgproject.rpg.combat;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RpgCombatStateProvider implements ICapabilityProvider {
    public static final Capability<RpgCombatState> DATA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(RpgProject.MOD_ID, "combat_state");
    private final RpgCombatState state = new RpgCombatState();
    private final LazyOptional<RpgCombatState> optional = LazyOptional.of(() -> state);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == DATA ? optional.cast() : LazyOptional.empty();
    }
}
