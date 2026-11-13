package dev.angopiccDev.crateforge.crate;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Keys are identified by a hidden PersistentDataContainer tag, never by name or lore,
 * so a renamed tripwire hook from an anvil can't open anything.
 */
public final class KeyService {

    private final NamespacedKey tag;

    public KeyService(Plugin plugin) {
        this.tag = new NamespacedKey(plugin, "crate_key");
    }

    /** A single key; hand out multiples with Items.give. */
    public ItemStack create(Crate crate) {
        ItemStack item = new ItemStack(crate.key().material());
        item.editMeta(meta -> {
            meta.displayName(crate.key().name());
            meta.lore(crate.key().lore());
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(tag, PersistentDataType.STRING, crate.id());
        });
        return item;
    }

    public boolean isKeyFor(ItemStack item, Crate crate) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(tag, PersistentDataType.STRING);
        return crate.id().equals(id);
    }
}
