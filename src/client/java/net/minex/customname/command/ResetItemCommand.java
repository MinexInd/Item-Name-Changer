package net.minex.customname.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minex.customname.storage.ItemDataStorage;
import net.minex.customname.util.ItemModifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ResetItemCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("resetitem")
            .executes(ResetItemCommand::execute)
        );
        
        // Also register as an alias /customname reset
        dispatcher.register(literal("customname")
            .then(literal("reset")
                .executes(ResetItemCommand::execute)
            )
        );
    }
    
    private static int execute(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        
        if (client.player == null) {
            return 0;
        }
        
        ItemStack heldItem = client.player.getMainHandStack();
        
        if (heldItem.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("You must be holding an item to reset it!")
                .formatted(Formatting.RED));
            return 0;
        }
        
        boolean hadCustomName = ItemModifier.hasCustomName(heldItem);
        
        if (!hadCustomName) {
            context.getSource().sendFeedback(Text.literal("This item doesn't have a custom name!")
                .formatted(Formatting.YELLOW));
            return 0;
        }
        
        ItemModifier.resetItem(heldItem);
        
        // Remove from storage
        ItemDataStorage.removeItem(heldItem);
        
        context.getSource().sendFeedback(Text.literal("Successfully reset item name!")
            .formatted(Formatting.GREEN));
        
        return 1;
    }
}