package com.zexqm.rpgproject.rpg;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.*;

public class RpgPlayerDataProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<RpgPlayerData> DATA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(RpgProject.MOD_ID, "player_data");
    private final RpgPlayerData data = new RpgPlayerData();
    private final LazyOptional<RpgPlayerData> optional = LazyOptional.of(() -> data);
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) { return cap == DATA ? optional.cast() : LazyOptional.empty(); }
    public CompoundTag serializeNBT() { return data.save(); }
    public void deserializeNBT(CompoundTag nbt) { data.load(nbt); }
}
