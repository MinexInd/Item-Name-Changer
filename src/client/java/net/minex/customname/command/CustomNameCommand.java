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

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class CustomNameCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("customname")
            .then(argument("name", StringArgumentType.greedyString())
                .executes(CustomNameCommand::execute)
            )
        );
    }
    
    private static int execute(CommandContext<FabricClientCommandSource> context) {
        String displayName = StringArgumentType.getString(context, "name");
        MinecraftClient client = context.getSource().getClient();
        
        if (client.player == null) {
            return 0;
        }
        
        ItemStack heldItem = client.player.getMainHandStack();
        
        if (heldItem.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("You must be holding an item to rename it!")
                .formatted(Formatting.RED));
            return 0;
        }
        
        // Apply the display name
        ItemModifier.setDisplayName(heldItem, displayName);
        
        // Store the item data for persistence
        ItemDataStorage.storeItem(heldItem, displayName);
        
        context.getSource().sendFeedback(Text.literal("Successfully renamed item to \"")
            .append(Text.literal(displayName).formatted(Formatting.YELLOW))
            .append(Text.literal("\"!"))
            .formatted(Formatting.GREEN));
        
        return 1;
    }
}
