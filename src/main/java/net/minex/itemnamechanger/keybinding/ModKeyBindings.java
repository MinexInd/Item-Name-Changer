package net.minex.itemnamechanger.keybinding;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("item-name-changer", "main")
    );

    public static final KeyMapping OPEN_GUI = new KeyMapping(
        "key.item-name-changer.opengui",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        CATEGORY
    );
}
