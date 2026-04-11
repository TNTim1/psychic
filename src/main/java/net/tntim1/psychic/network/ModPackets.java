package net.tntim1.psychic.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPackets {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("psychic", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        // Client → Server: activate (unlock) a widget
        CHANNEL.registerMessage(id++,
                ActivateWidgetPacket.class,
                ActivateWidgetPacket::encode,
                ActivateWidgetPacket::decode,
                ActivateWidgetPacket::handle);

        // Server → Client: push the full unlocked-ID set after any change
        CHANNEL.registerMessage(id++,
                SyncKnowledgePacket.class,
                SyncKnowledgePacket::encode,
                SyncKnowledgePacket::decode,
                SyncKnowledgePacket::handle);

        // Client → Server: deactivate (lock + cascade) a widget
        CHANNEL.registerMessage(id++,
                DeactivateWidgetPacket.class,
                DeactivateWidgetPacket::encode,
                DeactivateWidgetPacket::decode,
                DeactivateWidgetPacket::handle);
    }
}