package dev.angopiccDev.crateforge.listener;

import dev.angopiccDev.crateforge.CrateForgePlugin;
import dev.angopiccDev.crateforge.crate.Crate;
import dev.angopiccDev.crateforge.gui.PreviewMenu;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class CrateListener implements Listener {

    private final CrateForgePlugin plugin;

    public CrateListener(CrateForgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // the event fires once per hand; only handle the main hand
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        String crateId = plugin.locations().crateAt(block);
        if (crateId == null) {
            return;
        }

        event.setCancelled(true); // never open the chest underneath or start breaking it
        Player player = event.getPlayer();
        Crate crate = plugin.crates().get(crateId);
        if (crate == null) {
            return; // crate was deleted from crates.yml; logged on reload
        }
        if (!player.hasPermission("crateforge.use")) {
            plugin.messages().send(player, "no-permission");
            return;
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            player.openInventory(new PreviewMenu(crate).getInventory());
            return;
        }

        if (plugin.sessions().isOpening(player)) {
            plugin.messages().send(player, "already-opening");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!plugin.keys().isKeyFor(hand, crate)) {
            plugin.messages().send(player, "no-key", Placeholder.component("crate", crate.displayName()));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return;
        }

        // Take the key before rolling, so there is no window where the key and the reward both exist.
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            player.getInventory().setItemInMainHand(hand);
        }
        plugin.sessions().open(player, crate);
    }

    // --- Protect crate blocks from players, explosions and pistons -----------------------------

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.locations().crateAt(event.getBlock()) != null) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "break-protected");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> plugin.locations().crateAt(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> plugin.locations().crateAt(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (touchesCrate(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (touchesCrate(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    private boolean touchesCrate(List<Block> blocks) {
        for (Block block : blocks) {
            if (plugin.locations().crateAt(block) != null) {
                return true;
            }
        }
        return false;
    }

    // --- Lifecycle ------------------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Pay out before the player's inventory is saved.
        plugin.sessions().finish(event.getPlayer());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.holograms().onChunkLoad(event.getChunk());
    }
}
