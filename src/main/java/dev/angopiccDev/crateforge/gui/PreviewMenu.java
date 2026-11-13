package dev.angopiccDev.crateforge.gui;

import dev.angopiccDev.crateforge.crate.Crate;
import dev.angopiccDev.crateforge.crate.Reward;
import dev.angopiccDev.crateforge.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only view of a crate's rewards with their exact odds. Clicks are cancelled in MenuListener. */
public final class PreviewMenu implements InventoryHolder {

    private final Inventory inventory;

    public PreviewMenu(Crate crate) {
        List<Reward> rewards = crate.rewards();
        int rows = Math.max(1, Math.min(6, (rewards.size() + 8) / 9));
        Component title = Component.empty()
                .append(crate.displayName())
                .append(Component.text(" · Preview", NamedTextColor.DARK_GRAY));
        this.inventory = Bukkit.createInventory(this, rows * 9, title);

        for (int i = 0; i < rewards.size() && i < inventory.getSize(); i++) {
            Reward reward = rewards.get(i);
            inventory.setItem(i, previewIcon(reward, crate.chance(reward)));
        }
    }

    private static ItemStack previewIcon(Reward reward, double chance) {
        ItemStack copy = reward.displayItem().clone();
        copy.editMeta(meta -> {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            if (reward.amount() > copy.getAmount()) {
                lore.add(Text.item("<gray>Amount: <white>" + reward.amount()));
            }
            lore.add(Text.item("<gray>Chance: <yellow>" + String.format(Locale.ROOT, "%.2f", chance) + "%"));
            meta.lore(lore);
        });
        return copy;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
