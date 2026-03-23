package net.minex.customname.keybinding;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
        Identifier.of("itemnamechanger", "main")
    );

    public static final KeyBinding OPEN_GUI = new KeyBinding(
        "key.itemnamechanger.opengui",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        CATEGORY
    );
}
