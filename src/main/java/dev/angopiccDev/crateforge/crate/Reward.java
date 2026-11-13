package dev.angopiccDev.crateforge.crate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * One possible outcome of a crate.
 *
 * @param displayItem what the menus show (capped at one stack), and what is given when {@code giveItem} is true
 * @param amount      how many to give - may exceed a stack, e.g. 256 iron
 * @param commands    console commands run on win; %player% becomes the winner's name
 */
public record Reward(String id, double weight, ItemStack displayItem, int amount, boolean giveItem,
                     List<String> commands, boolean broadcast) {

    /** "3x Diamond", or the custom item name if the config set one. Used in chat messages. */
    public Component name() {
        ItemMeta meta = displayItem.getItemMeta();
        Component base = meta != null && meta.hasDisplayName()
                ? meta.displayName()
                : Component.translatable(displayItem.getType().translationKey(), NamedTextColor.WHITE);
        return amount > 1 ? Component.text(amount + "x ", NamedTextColor.WHITE).append(base) : base;
    }
}
