package dev.angopiccDev.crateforge.listener;

import dev.angopiccDev.crateforge.gui.PreviewMenu;
import dev.angopiccDev.crateforge.gui.SpinSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Locks both menus. The click is checked against the top inventory, so shift-clicking from the
 * player's own inventory and number-key swaps are blocked too - the usual source of GUI dupes.
 */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (isOurs(event.getView().getTopInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (isOurs(event.getView().getTopInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SpinSession session) {
            session.finish(); // closing early skips the animation; the reward was already decided
        }
    }

    private static boolean isOurs(InventoryHolder holder) {
        return holder instanceof PreviewMenu || holder instanceof SpinSession;
    }
}
