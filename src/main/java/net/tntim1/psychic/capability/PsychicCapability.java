package net.tntim1.psychic.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.tntim1.psychic.player_data.PsychicData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exposes the {@link PsychicData} capability using the 1.20.1 Forge API.
 *
 * <p><b>Setup checklist:</b>
 * <ol>
 *   <li>Subscribe to {@code RegisterCapabilitiesEvent} on the MOD bus and call
 *       {@code event.register(PsychicData.class)} — see {@link PsychicCapabilityEventHandler}.</li>
 *   <li>Subscribe {@link PsychicCapabilityEventHandler} on the FORGE bus for
 *       attach / clone / sync events.</li>
 * </ol>
 */
public class PsychicCapability {

    /**
     * The capability token — obtained via {@link CapabilityManager#get} with an
     * anonymous {@link CapabilityToken} so Forge can infer the generic type.
     * This replaces the removed {@code @CapabilityInject} annotation in 1.20.1.
     */
    public static final Capability<PsychicData> PSYCHIC_DATA_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    // ── ICapabilityProvider implementation ────────────────────────────────────

    /**
     * Attach one of these to a player entity in {@code AttachCapabilitiesEvent<Entity>}.
     */
    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final PsychicData data = new PsychicData();
        private final LazyOptional<PsychicData> optional = LazyOptional.of(() -> data);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return PSYCHIC_DATA_CAP.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            data.save(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.load(tag);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}