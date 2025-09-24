package net.minex.customname.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class HelpCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Register /customname help
        dispatcher.register(literal("customname")
            .then(literal("help")
                .executes(HelpCommand::execute)
            )
        );
        
        // Also register standalone /itemhelp
        dispatcher.register(literal("itemhelp")
            .executes(HelpCommand::execute)
        );
    }
    
    private static int execute(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("=== Item Name Changer Help ===")
            .formatted(Formatting.GOLD, Formatting.BOLD));
            
        context.getSource().sendFeedback(Text.literal(""));
        
        context.getSource().sendFeedback(Text.literal("Commands:")
            .formatted(Formatting.YELLOW, Formatting.UNDERLINE));
            
        context.getSource().sendFeedback(Text.literal("  /customname \"<name>\"")
            .formatted(Formatting.AQUA)
            .append(Text.literal(" - Rename the item in your main hand")
                .formatted(Formatting.WHITE)));
                
        context.getSource().sendFeedback(Text.literal("  /customname reset")
            .formatted(Formatting.AQUA)
            .append(Text.literal(" - Reset item name to original")
                .formatted(Formatting.WHITE)));
                
        context.getSource().sendFeedback(Text.literal("  /resetitem")
            .formatted(Formatting.AQUA)
            .append(Text.literal(" - Same as /customname reset")
                .formatted(Formatting.WHITE)));
        
        context.getSource().sendFeedback(Text.literal(""));
        
        context.getSource().sendFeedback(Text.literal("Examples:")
            .formatted(Formatting.YELLOW, Formatting.UNDERLINE));
            
        context.getSource().sendFeedback(Text.literal("  /customname \"§cMy Cool Sword\"")
            .formatted(Formatting.GREEN)
            .append(Text.literal(" - Red colored name")
                .formatted(Formatting.GRAY)));
                
        context.getSource().sendFeedback(Text.literal("  /customname \"§l§6Golden Blade\"")
            .formatted(Formatting.GREEN)
            .append(Text.literal(" - Bold golden name")
                .formatted(Formatting.GRAY)));
                
        context.getSource().sendFeedback(Text.literal(""));
        
        context.getSource().sendFeedback(Text.literal("Features:")
            .formatted(Formatting.YELLOW, Formatting.UNDERLINE));
            
        context.getSource().sendFeedback(Text.literal("• Rename items with any display name")
            .formatted(Formatting.WHITE));
            
        context.getSource().sendFeedback(Text.literal("• Supports color codes and formatting")
            .formatted(Formatting.WHITE));
            
        context.getSource().sendFeedback(Text.literal("• Persistent storage across sessions")
            .formatted(Formatting.WHITE));
            
        context.getSource().sendFeedback(Text.literal("• Reset items back to their original state")
            .formatted(Formatting.WHITE));
            
        context.getSource().sendFeedback(Text.literal("• Works in survival, creative, and minigames")
            .formatted(Formatting.WHITE));
            
        context.getSource().sendFeedback(Text.literal("• Client-side only - no server permission required")
            .formatted(Formatting.WHITE));
        
        return 1;
    }
}