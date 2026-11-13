package dev.angopiccDev.crateforge.gui;

import dev.angopiccDev.crateforge.CrateForgePlugin;
import dev.angopiccDev.crateforge.crate.Crate;
import dev.angopiccDev.crateforge.crate.Reward;
import dev.angopiccDev.crateforge.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * One player opening one crate: a horizontal reel that scrolls and slows down.
 *
 * The winner is rolled the moment the session is created, before anything is shown.
 * The reel is pure presentation. Closing the menu, disconnecting or a server reload
 * all just jump to the end and pay out that same pre-rolled reward, so there is no
 * way to re-roll by escaping the animation.
 */
public final class SpinSession implements InventoryHolder {

    private static final int REEL_START = 9;   // middle row: slots 9..17
    private static final int REEL_LENGTH = 9;
    private static final int CENTER_OFFSET = 4; // slot 13

    private final CrateForgePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Reward winner;
    private final List<Reward> strip;
    private final Inventory inventory;
    private final int totalShifts;
    private final int maxDelay;
    private final int closeDelay;

    private int shiftsDone;
    private int ticksUntilShift = 1;
    private boolean finished;
    private BukkitTask task;

    SpinSession(CrateForgePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.totalShifts = Math.max(10, plugin.getConfig().getInt("spin.shifts", 40));
        this.maxDelay = Math.max(1, plugin.getConfig().getInt("spin.max-delay", 8));
        this.closeDelay = Math.max(0, plugin.getConfig().getInt("spin.close-delay", 40));

        this.winner = crate.roll();

        // Filler is rolled with the real weights, so the reel shows commons far more often than
        // rares - the animation reflects the actual odds instead of teasing the jackpot.
        this.strip = new ArrayList<>(totalShifts + REEL_LENGTH);
        for (int i = 0; i < totalShifts + REEL_LENGTH; i++) {
            strip.add(crate.roll());
        }
        strip.set(totalShifts + CENTER_OFFSET, winner); // lands in the centre slot on the final shift

        this.inventory = Bukkit.createInventory(this, 27, crate.displayName());
        paintFrame(Material.GRAY_STAINED_GLASS_PANE);
        renderReel();
    }

    void start() {
        player.openInventory(inventory);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        if (finished) {
            return;
        }
        if (--ticksUntilShift > 0) {
            return;
        }
        shiftsDone++;
        renderReel();

        float progress = shiftsDone / (float) totalShifts;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.8f + progress * 0.8f);

        if (shiftsDone >= totalShifts) {
            finish();
            return;
        }
        // Cubic ease-out: fast spin for most of the run, then a sharp slowdown into the result.
        ticksUntilShift = 1 + Math.round((float) Math.pow(progress, 3) * (maxDelay - 1));
    }

    /** Ends the spin and pays out. Safe to call more than once; only the first call does anything. */
    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (task != null) {
            task.cancel();
        }

        shiftsDone = totalShifts;
        renderReel();
        paintFrame(winner.broadcast() ? Material.ORANGE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);

        plugin.sessions().complete(this);

        if (!plugin.isEnabled()) {
            // Shutting down: the scheduler is unavailable, close right away.
            closeIfViewing();
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, this::closeIfViewing, closeDelay);
        }
    }

    private void closeIfViewing() {
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == this) {
            player.closeInventory();
        }
    }

    private void renderReel() {
        for (int i = 0; i < REEL_LENGTH; i++) {
            inventory.setItem(REEL_START + i, strip.get(shiftsDone + i).displayItem());
        }
    }

    private void paintFrame(Material pane) {
        ItemStack filler = Items.pane(pane);
        for (int slot = 0; slot < 27; slot++) {
            if (slot < REEL_START || slot >= REEL_START + REEL_LENGTH) {
                inventory.setItem(slot, filler);
            }
        }
        // Pointers above and below the centre slot.
        ItemStack pointer = Items.pane(Material.YELLOW_STAINED_GLASS_PANE);
        inventory.setItem(REEL_START + CENTER_OFFSET - 9, pointer);
        inventory.setItem(REEL_START + CENTER_OFFSET + 9, pointer);
    }

    public Player player() {
        return player;
    }

    public Crate crate() {
        return crate;
    }

    public Reward winner() {
        return winner;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
