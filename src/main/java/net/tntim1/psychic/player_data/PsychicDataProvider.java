package net.tntim1.psychic.player_data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.tntim1.psychic.player_data.PsychicData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Forge capability provider that attaches a PsychicData instance to every player.
 *
 * Registration wiring (do these once in your main mod class):
 *
 *   1. Declare the capability token:
 *        public static final Capability<PsychicData> PSYCHIC_DATA =
 *            CapabilityManager.get(new CapabilityToken<>(){});
 *
 *   2. Register the capability type on the MOD bus:
 *        @SubscribeEvent
 *        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
 *            event.register(PsychicData.class);
 *        }
 *
 *   3. Attach the provider to players on the FORGE bus:
 *        @SubscribeEvent
 *        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
 *            if (event.getObject() instanceof Player) {
 *                event.addCapability(
 *                    new ResourceLocation("psychic", "psychic_data"),
 *                    new PsychicDataProvider()
 *                );
 *            }
 *        }
 *
 *   4. Copy data on player respawn/dimension change on the FORGE bus:
 *        @SubscribeEvent
 *        public static void onPlayerClone(PlayerEvent.Clone event) {
 *            if (!event.isWasDeath()) return; // dimension travel doesn't need copy
 *            event.getOriginal().reviveCaps();
 *            event.getOriginal().getCapability(Psychic.PSYCHIC_DATA).ifPresent(oldData ->
 *                event.getEntity().getCapability(Psychic.PSYCHIC_DATA).ifPresent(newData ->
 *                    newData.copyFrom(oldData)
 *                )
 *            );
 *            event.getOriginal().invalidateCaps();
 *        }
 */
public class PsychicDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final PsychicData data = new PsychicData();
    private final LazyOptional<PsychicData> optional = LazyOptional.of(() -> data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return net.tntim1.psychic.Psychic.PSYCHIC_DATA.orEmpty(cap, optional);
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

}