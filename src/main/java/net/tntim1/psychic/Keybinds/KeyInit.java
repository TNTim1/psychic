package net.tntim1.psychic.Keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyInit {
    public static final String CATEGORY = "key.categories.psychic";
    public static KeyMapping exampleHotKey;
    public static KeyMapping confirmKey; // Our new key

    public static void register(final RegisterKeyMappingsEvent event) {
        exampleHotKey = new KeyMapping(
                "key.psychic.cast_menu",
                KeyConflictContext.UNIVERSAL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C, // Default C
                CATEGORY
        );

        confirmKey = new KeyMapping(
                "key.psychic.confirm",
                KeyConflictContext.UNIVERSAL,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SPACE, // Default Space
                CATEGORY
        );

        event.register(exampleHotKey);
        event.register(confirmKey);
    }
}