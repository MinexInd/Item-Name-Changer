package net.minex.itemnamechanger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.network.chat.Component;

import net.minex.itemnamechanger.core.CustomNameManager;
import net.minex.itemnamechanger.matching.ItemFingerprint;
import net.minex.itemnamechanger.storage.StorageManager;
import net.minex.itemnamechanger.storage.StoredItem;

import java.util.ArrayList;

public class CommandHandler {

    public static void register() {
        System.out.println("[CustomName] Registering commands...");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            System.out.println("[CustomName] Command registration callback fired");
            registerCommands(dispatcher);
            System.out.println("[CustomName] Commands registered successfully");
        });
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {

        dispatcher.register(
            ClientCommands.literal("customname")
                .then(ClientCommands.literal("restore")
                    .executes(ctx -> {
                        var stack = CustomNameManager.getHeldItem();
                        if (stack.isEmpty()) {
                            ctx.getSource().sendFeedback(Component.literal("No item in hand!"));
                            return 0;
                        }

                        String fp = ItemFingerprint.getFingerprint(stack);
                        StoredItem stored = StorageManager.getItem(fp);

                        if (stored != null) {
                            if (stored.getName() != null && !stored.getName().isEmpty()) {
                                CustomNameManager.applyName(stack, stored.getName());
                            }
                            if (stored.lore != null && !stored.lore.isEmpty()) {
                                CustomNameManager.applyLore(stack, stored.lore);
                            }
                            ctx.getSource().sendFeedback(Component.literal("Item restored."));
                        } else {
                            ctx.getSource().sendFeedback(Component.literal("No stored data for this item."));
                        }

                        return 1;
                    })
                )
                .then(ClientCommands.literal("restoreall")
                    .executes(ctx -> {
                        CustomNameManager.restoreAllItems();
                        ctx.getSource().sendFeedback(Component.literal("All items restored."));
                        return 1;
                    })
                )
                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");

                        var stack = CustomNameManager.getHeldItem();
                        if (stack.isEmpty()) {
                            ctx.getSource().sendFeedback(Component.literal("No item in hand!"));
                            return 0;
                        }

                        String fp = ItemFingerprint.getFingerprint(stack);

                        CustomNameManager.applyName(stack, name);

                        StoredItem stored = StorageManager.getItem(fp);
                        if (stored == null) {
                            stored = new StoredItem(name, new ArrayList<>());
                        } else {
                            stored.name = name;
                        }
                        StorageManager.setItem(fp, stored);

                        ctx.getSource().sendFeedback(Component.literal("Renamed to: " + name));
                        return 1;
                    })
                )
        );
    }
}
