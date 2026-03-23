package net.minex.customname.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class StorageManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, StoredItem> items = new HashMap<>();
    private static Path filePath;
    private static String currentServerId = "singleplayer";
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getCurrentServerEntry() != null) {
            currentServerId = client.getCurrentServerEntry().address;
        } else {
            currentServerId = "singleplayer";
        }

        try {
            Path dir = Paths.get("config/customname");
            Files.createDirectories(dir);
            filePath = dir.resolve(currentServerId + ".json");

            if (Files.exists(filePath)) {
                load();
            } else {
                items = new HashMap<>();
                save();
            }

            initialized = true;
            System.out.println("[CustomName] Storage initialized for: " + currentServerId);

        } catch (IOException e) {
            System.err.println("[CustomName] Failed to initialize storage: " + e.getMessage());
        }
    }

    public static void reset() {
        initialized = false;
        items.clear();
    }

    public static void save() {
        if (filePath == null) {
            return;
        }

        try (Writer writer = Files.newBufferedWriter(filePath)) {
            GSON.toJson(items, writer);
        } catch (IOException e) {
            System.err.println("[CustomName] Failed to save storage: " + e.getMessage());
        }
    }

    public static void load() {
        if (filePath == null || !Files.exists(filePath)) {
            items = new HashMap<>();
            return;
        }

        try (Reader reader = Files.newBufferedReader(filePath)) {
            Type type = new TypeToken<Map<String, StoredItem>>() {}.getType();
            items = GSON.fromJson(reader, type);
            if (items == null) {
                items = new HashMap<>();
            }
        } catch (IOException e) {
            System.err.println("[CustomName] Failed to load storage: " + e.getMessage());
            items = new HashMap<>();
        }
    }

    public static void setItem(String fingerprint, String name) {
        if (!initialized) {
            init();
        }
        StoredItem item = new StoredItem(name, new ArrayList<>());
        items.put(fingerprint, item);
        save();
        System.out.println("[CustomName] Stored name for " + fingerprint + ": " + name);
    }

    public static void storeItem(String fingerprint, String name, List<String> lore) {
        if (!initialized) {
            init();
        }
        List<String> loreList = lore != null ? lore : new ArrayList<>();
        StoredItem item = new StoredItem(name, loreList);
        items.put(fingerprint, item);
        save();
        System.out.println("[CustomName] Stored item for " + fingerprint + " (name: " + name + ", lore: " + loreList.size() + " lines)");
    }

    public static void setItem(String fingerprint, StoredItem stored) {
        if (!initialized) {
            init();
        }
        items.put(fingerprint, stored);
        save();
    }

    public static StoredItem getItem(String fingerprint) {
        if (!initialized) {
            init();
        }
        return items.get(fingerprint);
    }

    public static void removeItem(String fingerprint) {
        if (!initialized) {
            return;
        }
        items.remove(fingerprint);
        save();
        System.out.println("[CustomName] Removed stored name for: " + fingerprint);
    }

    public static Map<String, StoredItem> getAllItems() {
        if (!initialized) {
            init();
        }
        return new HashMap<>(items);
    }
}
