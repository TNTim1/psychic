package net.tntim1.psychic.player_data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tntim1.psychic.network.ManaSyncPacket;
import net.tntim1.psychic.network.ModPackets;

@Mod.EventBusSubscriber(modid = "psychic")
public class ManaEvents {

    @SubscribeEvent
    public static void attachMana(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation("psychic", "player_mana"),
                    new PlayerManaProvider()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        player.getCapability(PlayerManaProvider.CAP).ifPresent(data -> {
            boolean changed = data.tick();
            // Sync every second always, or immediately on change every 5 ticks
            if (player.tickCount % 20 == 0 || (changed && player.tickCount % 5 == 0)) {
                ModPackets.sendToPlayer(
                        new ManaSyncPacket(data.getMana(), data.getMaxMana()), player);
            }
        });
    }

    @SubscribeEvent
    public static void onLogin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerManaProvider.CAP).ifPresent(data ->
                    ModPackets.sendToPlayer(
                            new ManaSyncPacket(data.getMana(), data.getMaxMana()), player));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Copy mana from old player to new (respawn clones caps)
            player.getCapability(PlayerManaProvider.CAP).ifPresent(data ->
                    ModPackets.sendToPlayer(
                            new ManaSyncPacket(data.getMana(), data.getMaxMana()), player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // Preserve mana across death
        event.getOriginal().getCapability(PlayerManaProvider.CAP).ifPresent(oldData ->
                event.getEntity().getCapability(PlayerManaProvider.CAP).ifPresent(newData -> {
                    newData.setMaxMana(oldData.getMaxMana());
                    newData.setMana(oldData.getMana());
                }));
    }
}