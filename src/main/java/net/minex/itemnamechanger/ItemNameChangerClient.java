package net.minex.itemnamechanger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;

import net.minex.itemnamechanger.core.CustomNameManager;
import net.minex.itemnamechanger.storage.StorageManager;
import net.minex.itemnamechanger.restore.RestoreEngine;
import net.minex.itemnamechanger.command.CommandHandler;
import net.minex.itemnamechanger.keybinding.ModKeyBindings;
import net.minex.itemnamechanger.gui.ItemEditorScreen;

public class ItemNameChangerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		StorageManager.init();
		RestoreEngine.init();
		CommandHandler.register();

		KeyMappingHelper.registerKeyMapping(ModKeyBindings.OPEN_GUI);

		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(Minecraft client) {
		if (client.player == null) {
			return;
		}

		while (ModKeyBindings.OPEN_GUI.consumeClick()) {
			var stack = CustomNameManager.getHeldItem();
			if (!stack.isEmpty()) {
				client.gui.setScreen(new ItemEditorScreen(client.gui.screen(), stack));
			}
		}
	}
}
