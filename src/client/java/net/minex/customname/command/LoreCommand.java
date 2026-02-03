package net.minex.customname.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minex.customname.storage.ItemDataStorage;
import net.minex.customname.util.ItemModifier;

import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class LoreCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("customname")
            .then(literal("lore")
                .then(literal("add")
                    .then(argument("line", StringArgumentType.greedyString())
                        .executes(LoreCommand::addLore)
                    )
                )
                .then(literal("clear")
                    .executes(LoreCommand::clearLore)
                )
            )
        );
    }

    private static int addLore(CommandContext<FabricClientCommandSource> context) {
        String loreLine = StringArgumentType.getString(context, "line");
        MinecraftClient client = context.getSource().getClient();

        if (client.player == null) {
            return 0;
        }

        ItemStack heldItem = client.player.getMainHandStack();
        if (heldItem.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("You must be holding an item to add lore!")
                .formatted(Formatting.RED));
            return 0;
        }

        List<String> loreLines = new ArrayList<>();
        ItemDataStorage.ItemData stored = ItemDataStorage.getStoredItem(heldItem);
        if (stored != null && stored.loreLines != null) {
            loreLines.addAll(stored.loreLines);
        }
        loreLines.add(loreLine);

        ItemModifier.setLoreLines(heldItem, loreLines);
        ItemDataStorage.storeLore(heldItem, loreLines);

        context.getSource().sendFeedback(Text.literal("Added lore line to item!")
            .formatted(Formatting.GREEN));
        return 1;
    }

    private static int clearLore(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();

        if (client.player == null) {
            return 0;
        }

        ItemStack heldItem = client.player.getMainHandStack();
        if (heldItem.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("You must be holding an item to clear lore!")
                .formatted(Formatting.RED));
            return 0;
        }

        if (!ItemModifier.hasLore(heldItem)) {
            context.getSource().sendFeedback(Text.literal("This item doesn't have lore to clear!")
                .formatted(Formatting.YELLOW));
            return 0;
        }

        ItemModifier.setLoreLines(heldItem, List.of());
        ItemDataStorage.removeLore(heldItem);

        context.getSource().sendFeedback(Text.literal("Cleared item lore!")
            .formatted(Formatting.GREEN));
        return 1;
    }
}
