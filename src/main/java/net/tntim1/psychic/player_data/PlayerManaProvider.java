// PlayerManaProvider.java
package net.tntim1.psychic.player_data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerManaProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerManaData> CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerManaData data = new PlayerManaData();
    private final LazyOptional<PlayerManaData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("mana", data.getMana());
        tag.putFloat("maxMana", data.getMaxMana());
        tag.putFloat("regenPerSecond", data.getRegenPerSecond());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Use reflection-free approach: spend down to 0 then add
        float savedMax = tag.getFloat("maxMana");
        float savedMana = tag.getFloat("mana");
        if (tag.contains("regenPerSecond")) data.setRegenPerSecond(tag.getFloat("regenPerSecond"));
        data.setMaxMana(savedMax > 0 ? savedMax : 100f);
        data.spend(data.getMana()); // zero it out
        // Re-add via internal set (add a package-private setter):
        data.setMana(savedMana);
    }
}

