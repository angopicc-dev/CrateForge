package dev.angopiccDev.crateforge.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Items {

    private Items() {
    }

    /**
     * Builds an item from a config section. Throws IllegalArgumentException with a readable
     * message on bad input, so a typo in crates.yml names the exact line instead of a stack trace.
     */
    public static ItemStack fromConfig(ConfigurationSection section, String where) {
        if (section == null) {
            throw new IllegalArgumentException(where + " is missing");
        }
        String materialName = section.getString("material", "");
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir() || !material.isItem()) {
            throw new IllegalArgumentException(where + ": '" + materialName + "' is not an item material");
        }

        // The menu icon never exceeds one legal stack; the real amount is given separately (see Reward).
        int amount = Math.max(1, Math.min(material.getMaxStackSize(), section.getInt("amount", 1)));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (section.isString("name")) {
            meta.displayName(Text.item(section.getString("name")));
        }
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(Text.itemLines(lore));
        }

        ConfigurationSection enchants = section.getConfigurationSection("enchants");
        if (enchants != null) {
            for (String key : enchants.getKeys(false)) {
                Enchantment enchantment = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.ENCHANTMENT)
                        .get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
                if (enchantment == null) {
                    throw new IllegalArgumentException(where + ": unknown enchantment '" + key + "'");
                }
                // true = allow levels above vanilla max (Sharpness VI etc.), which crate owners always want.
                meta.addEnchant(enchantment, Math.max(1, enchants.getInt(key, 1)), true);
            }
        }
        if (section.getBoolean("glow", false)) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (section.getBoolean("unbreakable", false)) {
            meta.setUnbreakable(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    /** A menu filler pane with a blank name. */
    public static ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        pane.editMeta(meta -> meta.displayName(Component.text(" ")));
        return pane;
    }

    /**
     * Gives {@code total} copies of {@code template}, split into legal stack sizes, dropping
     * whatever doesn't fit at the player's feet. Returns true if anything was dropped.
     * The template's own amount is ignored, so no ItemStack ever holds an illegal count.
     */
    public static boolean give(Player player, ItemStack template, int total) {
        boolean dropped = false;
        int remaining = total;
        int maxStack = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int count = Math.min(remaining, maxStack);
            remaining -= count;
            ItemStack part = template.clone();
            part.setAmount(count);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(part);
            for (ItemStack rest : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
                dropped = true;
            }
        }
        return dropped;
    }
}
