package net.minex.customname.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.text.Text;

import net.minex.customname.core.CustomNameManager;
import net.minex.customname.matching.ItemFingerprint;
import net.minex.customname.storage.StorageManager;
import net.minex.customname.storage.StoredItem;

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
            ClientCommandManager.literal("customname")
                .then(ClientCommandManager.literal("restore")
                    .executes(ctx -> {
                        var stack = CustomNameManager.getHeldItem();
                        if (stack.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("No item in hand!"));
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
                            ctx.getSource().sendFeedback(Text.literal("Item restored."));
                        } else {
                            ctx.getSource().sendFeedback(Text.literal("No stored data for this item."));
                        }

                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("restoreall")
                    .executes(ctx -> {
                        CustomNameManager.restoreAllItems();
                        ctx.getSource().sendFeedback(Text.literal("All items restored."));
                        return 1;
                    })
                )
                .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");

                        var stack = CustomNameManager.getHeldItem();
                        if (stack.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("No item in hand!"));
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

                        ctx.getSource().sendFeedback(Text.literal("Renamed to: " + name));
                        return 1;
                    })
                )
        );
    }
}
