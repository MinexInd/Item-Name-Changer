package net.minex.customname.matching;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ItemFingerprint {

    public static String getFingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Registries.ITEM.getId(stack.getItem()));

        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants != null && !enchants.getEnchantments().isEmpty()) {
            List<String> entries = new ArrayList<>();
            for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                int level = enchants.getLevel(entry);
                entry.getKey().ifPresent(key -> {
                    entries.add(key.getValue() + ":" + level);
                });
            }
            entries.sort(Comparator.naturalOrder());
            sb.append("|e:").append(String.join(",", entries));
        }

        return sb.toString();
    }
}
