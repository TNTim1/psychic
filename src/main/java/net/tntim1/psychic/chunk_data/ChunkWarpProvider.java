package net.tntim1.psychic.chunk_data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class ChunkWarpProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ChunkWarpData> CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final ChunkWarpData data = new ChunkWarpData();
    private final LazyOptional<ChunkWarpData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("warp", data.getWarpStrength());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.addWarp(tag.getInt("warp") - data.getWarpStrength());
    }
}
