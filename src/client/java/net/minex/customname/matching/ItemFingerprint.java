package net.minex.customname.matching;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;

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
        sb.append(BuiltInRegistries.ITEM.getKey(stack.getItem()));

        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants != null && !enchants.keySet().isEmpty()) {
            List<String> entries = new ArrayList<>();
            for (Holder<Enchantment> entry : enchants.keySet()) {
                int level = enchants.getLevel(entry);
                entry.unwrapKey().ifPresent(key -> {
                    entries.add(key.identifier() + ":" + level);
                });
            }
            entries.sort(Comparator.naturalOrder());
            sb.append("|e:").append(String.join(",", entries));
        }

        return sb.toString();
    }
}
