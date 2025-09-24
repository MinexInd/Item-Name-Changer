package net.minex.customname;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minex.customname.command.CustomNameCommand;
import net.minex.customname.command.HelpCommand;
import net.minex.customname.command.ResetItemCommand;
import net.minex.customname.command.RestoreCommand;
import net.minex.customname.storage.ItemDataStorage;
import net.minex.customname.sync.InventorySyncManager;

public class ItemNameChangerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register client-side commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			CustomNameCommand.register(dispatcher);
			ResetItemCommand.register(dispatcher);
			RestoreCommand.register(dispatcher);
			HelpCommand.register(dispatcher);
		});
		
		// Set up world join/leave events for storage management
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// Start the inventory sync manager
			InventorySyncManager.start();
		});
		
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			// Stop the inventory sync manager and cleanup
			InventorySyncManager.stop();
			ItemDataStorage.cleanup();
		});
		
		// Update world key when joining worlds/servers
		// This will be triggered by mixins when the player joins a world
		
		ItemNameChanger.LOGGER.info("Item Name Changer client initialized with persistence support!");
	}
}
