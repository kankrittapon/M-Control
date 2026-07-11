package com.zexqm.rpgproject.mana;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManaProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<Mana> MANA = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation ID = new ResourceLocation(RpgProject.MOD_ID, "mana");

    private final Mana mana = new Mana();
    private final LazyOptional<Mana> optional = LazyOptional.of(() -> mana);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MANA ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return mana.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        mana.load(nbt);
    }
}
