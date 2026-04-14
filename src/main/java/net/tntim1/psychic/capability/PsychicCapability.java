package net.tntim1.psychic.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsychicCapability {
    public static final Capability<PsychicData> PSYCHIC_DATA_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PsychicData instance = new PsychicData();
        private final LazyOptional<PsychicData> optional = LazyOptional.of(() -> instance);

        // This is the method the event handler was looking for!
        public void invalidate() {
            optional.invalidate();
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == PSYCHIC_DATA_CAP) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag nbt = new CompoundTag();
            instance.saveNBTData(nbt);
            return nbt;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.loadNBTData(nbt);
        }
    }
}