package net.tntim1.psychic.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.block.entity.CompletePuzzlePacket;
import net.tntim1.psychic.block.entity.NoPuzzlePacket;
import net.tntim1.psychic.block.entity.RequestPuzzlePacket;
import net.tntim1.psychic.block.entity.StartPuzzlePacket;
import net.tntim1.psychic.network.SyncSpellHistoryPacket;
import org.checkerframework.checker.units.qual.C;

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


        // ── SERVER → CLIENT ───────────────────────────────────────────────────

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

        net.messageBuilder(NoPuzzlePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(NoPuzzlePacket::new)
                .encoder(NoPuzzlePacket::toBytes)
                .consumerMainThread(NoPuzzlePacket::handle)
                .add();

        net.messageBuilder(StartPuzzlePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StartPuzzlePacket::new)
                .encoder(StartPuzzlePacket::toBytes)
                .consumerMainThread(StartPuzzlePacket::handle)
                .add();

        net.messageBuilder(CompletePuzzlePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CompletePuzzlePacket::new)
                .encoder(CompletePuzzlePacket::toBytes)
                .consumerMainThread(CompletePuzzlePacket::handle)
                .add();

// ── CLIENT → SERVER ───────────────────────────────────────────────────
// (Add this alongside ActivateWidgetPacket)

        net.messageBuilder(RequestPuzzlePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestPuzzlePacket::new)
                .encoder(RequestPuzzlePacket::toBytes)
                .consumerMainThread(RequestPuzzlePacket::handle)
                .add();

        // Inside ModPackets.register()
        // ADD THIS HERE:
        net.messageBuilder(SyncSpellHistoryPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncSpellHistoryPacket::decode)
                .encoder(SyncSpellHistoryPacket::encode)
                .consumerMainThread(SyncSpellHistoryPacket::handle)
                .add();

        // ── CLIENT → SERVER ───────────────────────────────────────────────────

        net.messageBuilder(ActivateWidgetPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ActivateWidgetPacket::decode)
                .encoder(ActivateWidgetPacket::encode)
                .consumerMainThread(ActivateWidgetPacket::handle)
                .add();

        net.messageBuilder(SaveGameStatePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SaveGameStatePacket::new)
                .encoder(SaveGameStatePacket::toBytes)
                .consumerMainThread(SaveGameStatePacket::handle)
                .add();

        net.messageBuilder(DeactivateWidgetPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeactivateWidgetPacket::decode)
                .encoder(DeactivateWidgetPacket::encode)
                .consumerMainThread(DeactivateWidgetPacket::handle)
                .add();


        net.messageBuilder(WarpSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(WarpSyncPacket::decode)
                .encoder(WarpSyncPacket::encode)
                .consumerMainThread(WarpSyncPacket::handle)
                .add();

        net.messageBuilder(RequestWarpPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestWarpPacket::decode)
                .encoder(RequestWarpPacket::encode)
                .consumerMainThread(RequestWarpPacket::handle)
                .add();

        net.messageBuilder(CastSpellPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CastSpellPacket::decode)
                .encoder(CastSpellPacket::encode)
                .consumerMainThread(CastSpellPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}