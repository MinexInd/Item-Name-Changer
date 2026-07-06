package net.minex.itemnamechanger.storage;

import java.util.List;

/**
 * Represents a stored custom name for an item.
 * Used for JSON serialization/deserialization.
 */
public class StoredItem {
    public String name;
    public List<String> lore;

    public StoredItem() {
    }

    public StoredItem(String name, List<String> lore) {
        this.name = name;
        this.lore = lore;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }
}
