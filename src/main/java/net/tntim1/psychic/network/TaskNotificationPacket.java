package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.player_data.ClientKnowledge;

import java.util.function.Supplier;

public class TaskNotificationPacket {

    public final String widgetId;
    public final String taskLabel;  // e.g. "Blazes slain"
    public final boolean completed; // true = this specific task is now done

    public TaskNotificationPacket(String widgetId, String taskLabel, boolean completed) {
        this.widgetId  = widgetId;
        this.taskLabel = taskLabel;
        this.completed = completed;
    }

    public static void encode(TaskNotificationPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.widgetId);
        buf.writeUtf(pkt.taskLabel);
        buf.writeBoolean(pkt.completed);
    }

    public static TaskNotificationPacket decode(FriendlyByteBuf buf) {
        return new TaskNotificationPacket(buf.readUtf(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(TaskNotificationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientKnowledge.queueTaskToast(pkt.widgetId, pkt.taskLabel, pkt.completed));
        ctx.get().setPacketHandled(true);
    }
}