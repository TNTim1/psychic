package net.tntim1.psychic.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.network.SyncTaskProgressPacket;
import net.tntim1.psychic.network.WidgetAutoActivatePacket;
import net.tntim1.psychic.network.TaskNotificationPacket;
import net.tntim1.psychic.player_data.PsychicData;
import net.tntim1.psychic.player_data.TaskProgress;
import net.tntim1.psychic.widget.TabDefinition;
import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.TaskRequirement;
import net.tntim1.psychic.widget.WidgetDefinition;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Listens for player kills and item pickups and updates task progress.
 * Register this on the FORGE bus in your mod constructor:
 * <pre>
 *   MinecraftForge.EVENT_BUS.register(new TaskEventHandler());
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Psychic.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaskEventHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // We only care if a player landed the kill
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity killed = event.getEntity();
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(killed.getType()).toString();

        checkAndUpdateProgress(player, TaskRequirement.Type.KILL, entityId, 1);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem().getItem();
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();// e.g. "minecraft:blaze_rod"
        int count = stack.getCount();

        checkAndUpdateProgress(player, TaskRequirement.Type.ITEM, itemId, count);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Iterates every widget across every tab. For each task requirement matching
     * the given type+targetId, increments progress, checks for completion, and
     * fires auto-activation + sync packets when all tasks for a widget are done.
     */
    private static void checkAndUpdateProgress(ServerPlayer player,
                                               TaskRequirement.Type type,
                                               String targetId,
                                               int amount) {
        PsychicData data = PsychicData.get(player);
        TaskProgress progress = data.taskProgress;
        boolean anyChange = false;

        for (TabDefinition tab : TabRegistry.getTabs()) {
            for (WidgetDefinition widget : tab.widgets) {
                if (!widget.hasTasks()) continue;
                // Skip already-unlocked widgets (nothing to track)
                if (data.isUnlocked(widget.id)) continue;
                // Widget deps still not met? Don't count yet.
                if (!data.areDependenciesMet(widget.id)) continue;

                List<TaskRequirement> reqs = widget.taskRequirements;
                for (int i = 0; i < reqs.size(); i++) {
                    TaskRequirement req = reqs.get(i);
                    if (req.type != type || !req.targetId.equals(targetId)) continue;
                    if (progress.isMet(widget.id, i, req.required)) continue; // already done

                    // Increment by amount (for item pickups that come in stacks)
                    int current = progress.get(widget.id, i);
                    int next = Math.min(current + amount, req.required);
                    progress.set(widget.id, i, next);
                    anyChange = true;

                    // Individual task completed notification
                    if (next >= req.required) {
                        ModPackets.sendToPlayer(new TaskNotificationPacket(widget.id, req.label, true), player);
                    }
                }

                // Check if ALL tasks for this widget are now complete
                if (areAllTasksMet(widget, progress)) {
                    // Auto-activate on the server
                    data.unlock(widget.id);
                    // Notify client: "Widget X unlocked!"
                    if (areAllTasksMet(widget, progress)) {
                        data.unlock(widget.id);
                        ModPackets.sendToPlayer(new WidgetAutoActivatePacket(widget.id), player);
                    }
                }
            }
        }

        if (anyChange) {
            data.setDirty();
            // Sync full progress snapshot to the client so the GUI reflects changes
            if (anyChange) {
                data.setDirty();
                ModPackets.sendToPlayer(new SyncTaskProgressPacket(progress.snapshot()), player);
            }
        }
    }

    private static boolean areAllTasksMet(WidgetDefinition widget, TaskProgress progress) {
        List<TaskRequirement> reqs = widget.taskRequirements;
        for (int i = 0; i < reqs.size(); i++) {
            if (!progress.isMet(widget.id, i, reqs.get(i).required)) return false;
        }
        return true;
    }
}