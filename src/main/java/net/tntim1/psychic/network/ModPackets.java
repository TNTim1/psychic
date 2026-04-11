package net.tntim1.psychic.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.network.SyncKnowledgePacket;
import net.tntim1.psychic.network.SyncTaskProgressPacket;
import net.tntim1.psychic.network.TaskNotificationPacket;
import net.tntim1.psychic.network.WidgetAutoActivatePacket;

public class ModPackets {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Psychic.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Ensure ALL of these are registered:
        net.messageBuilder(SyncKnowledgePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncKnowledgePacket::decode)
                .encoder(SyncKnowledgePacket::encode)
                .consumerMainThread(SyncKnowledgePacket::handle)
                .add();

        net.messageBuilder(WidgetAutoActivatePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(WidgetAutoActivatePacket::decode)
                .encoder(WidgetAutoActivatePacket::encode)
                .consumerMainThread(WidgetAutoActivatePacket::handle)
                .add();

        // THIS IS THE ONE CAUSING THE CRASH IF MISSING
        net.messageBuilder(TaskNotificationPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TaskNotificationPacket::decode)
                .encoder(TaskNotificationPacket::encode)
                .consumerMainThread(TaskNotificationPacket::handle)
                .add();

        net.messageBuilder(SyncTaskProgressPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncTaskProgressPacket::decode)
                .encoder(SyncTaskProgressPacket::encode)
                .consumerMainThread(SyncTaskProgressPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}