package net.minex;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;

import net.minex.customname.core.CustomNameManager;
import net.minex.customname.storage.StorageManager;
import net.minex.customname.restore.RestoreEngine;
import net.minex.customname.command.CommandHandler;
import net.minex.customname.keybinding.ModKeyBindings;
import net.minex.customname.gui.ItemEditorScreen;

public class ItemNameChangerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		StorageManager.init();
		RestoreEngine.init();
		CommandHandler.register();

		KeyBindingHelper.registerKeyBinding(ModKeyBindings.OPEN_GUI);

		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		while (ModKeyBindings.OPEN_GUI.wasPressed()) {
			var stack = CustomNameManager.getHeldItem();
			if (!stack.isEmpty()) {
				client.setScreen(new ItemEditorScreen(client.currentScreen, stack));
			}
		}
	}
}
