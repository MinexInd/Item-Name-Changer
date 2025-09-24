# Item Name Changer

A lightweight Fabric mod that lets players rename their held items directly through simple commands - no anvils or experience required!

## Description

Normally, survival players can rename items using an anvil, but in gamemodes like Bedwars, Skywars, or custom minigames, anvils aren't always available. This mod solves that problem by giving players quick access to cosmetic item renaming with full persistence support.

## 🔹 Features

-  **Rename items** with any custom display name (supports formatting codes)
-  **Reset items** back to their original state with one command
-  **Persistent storage** - item names survive server sync, GUI interactions, and reconnections
-  **World-specific** - separate storage for different worlds/servers
-  **Auto-restore** - automatically restores item names when server resets them
-  **Works great** for survival, creative, and minigame servers
-  **Client-side only** - no server permission required
-  **Lightweight** - minimal performance impact

## 📋 Commands

### `/customname "<name>"`
Renames the item in your main hand to the specified name.
- Example: `/customname "§cMy Cool Sword"` (creates a red-colored name)
- Example: `/customname "§l§6Golden Blade"` (creates a bold golden name)

### `/customname reset` or `/resetitem`
Resets the item in your main hand back to its original state.
- Removes the custom name
- Also removes the item from persistent storage

### `/customname restore` or `/restoreitems` 
Manually restores all items in your inventory from storage.
- Useful if items lose their names due to server synchronization
- Automatically triggered when needed

### `/customname help` or `/itemhelp`
Shows detailed help information and examples.

##  Formatting Codes

You can use Minecraft formatting codes in item names:
- `§c` - Red
- `§a` - Green  
- `§b` - Aqua
- `§e` - Yellow
- `§l` - Bold
- `§n` - Underline
- `§o` - Italic
- And many more!

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the mod JAR file
4. Place it in your `.minecraft/mods` folder

## 🔧 Requirements

- Minecraft 1.21
- Fabric Loader 0.17.2+
- Fabric API
- Java 21+

## 💾 Persistence Features

The mod now includes advanced persistence to solve common issues:

- ** Server Sync Protection**: Items keep their names even when the server resets them
- ** GUI Interaction Fix**: Opening chests, inventories, or using items won't reset names  
- ** World Separation**: Each world/server has its own item storage
- ** Auto-Restoration**: Automatically detects and restores items that lose their custom properties
- ** Disk Storage**: Item names persist between game sessions
- ** Auto-Cleanup**: Old item data is automatically cleaned up after 30 days

This fixes the issues where:
- Items would revert to normal when opening GUIs
- Using items like wind charges would reset their names
- Hitting entities would cause items to lose customization
- Rejoining servers/worlds would lose all custom names

##  Use Cases

Perfect for:
- **Survival players** who want to rename items without finding an anvil
- **Creative builders** working on custom maps
- **Minigame servers** where anvils aren't available
- **Role-playing servers** for custom item naming
- **Players who want persistent item names** that survive server changes

## 🛠️ Technical Details

This mod is **client-side only**, meaning:
- Works on any server (no server-side installation needed)
- No special permissions required
- Changes are visible only to you
- Safe to use in multiplayer environments

The mod uses Minecraft's built-in item component system to modify:
- `CustomName` component for display names

##  License

This project is licensed under the MIT License.


## 📞 Support

If you encounter any issues or have suggestions, please open an issue on the GitHub repository.

---

**Made with ❤️ for the Minecraft Fabric community**