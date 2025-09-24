package net.minex.customname.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minex.customname.sync.InventorySyncManager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class RestoreCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Register /customname restore
        dispatcher.register(literal("customname")
            .then(literal("restore")
                .executes(RestoreCommand::execute)
            )
        );
        
        // Also register standalone /restoreitems
        dispatcher.register(literal("restoreitems")
            .executes(RestoreCommand::execute)
        );
    }
    
    private static int execute(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("Restoring all items from storage...")
            .formatted(Formatting.YELLOW));
            
        InventorySyncManager.restoreAllItems();
        
        context.getSource().sendFeedback(Text.literal("Item restoration complete! Check your inventory.")
            .formatted(Formatting.GREEN));
        
        return 1;
    }
}