package dev.angopiccDev.crateforge.gui;

import dev.angopiccDev.crateforge.CrateForgePlugin;
import dev.angopiccDev.crateforge.crate.Crate;
import dev.angopiccDev.crateforge.crate.Reward;
import dev.angopiccDev.crateforge.util.Items;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks who is mid-spin and hands out rewards. */
public final class SessionManager {

    private final CrateForgePlugin plugin;
    private final Map<UUID, SpinSession> active = new HashMap<>();

    public SessionManager(CrateForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOpening(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public void open(Player player, Crate crate) {
        SpinSession session = new SpinSession(plugin, player, crate);
        active.put(player.getUniqueId(), session);
        session.start();
    }

    /** Skips straight to the result, e.g. when the player disconnects. */
    public void finish(Player player) {
        SpinSession session = active.get(player.getUniqueId());
        if (session != null) {
            session.finish();
        }
    }

    /** Used on reload and shutdown so no one is left mid-spin without their reward. */
    public void finishAll() {
        for (SpinSession session : new ArrayList<>(active.values())) {
            session.finish();
        }
    }

    /** Called exactly once per session, from SpinSession#finish. */
    void complete(SpinSession session) {
        active.remove(session.player().getUniqueId(), session);
        grant(session.player(), session.crate(), session.winner());
    }

    private void grant(Player player, Crate crate, Reward reward) {
        if (!player.isOnline()) {
            // Quit is handled before the player is saved, so this should never happen; log it if it does.
            plugin.getLogger().warning(player.getName() + " went offline before receiving '"
                    + reward.id() + "' from crate '" + crate.id() + "'.");
            return;
        }

        if (reward.giveItem() && Items.give(player, reward.displayItem(), reward.amount())) {
            plugin.messages().send(player, "inventory-full");
        }
        for (String command : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }

        plugin.messages().send(player, "won",
                Placeholder.component("reward", reward.name()),
                Placeholder.component("crate", crate.displayName()));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        if (reward.broadcast()) {
            Bukkit.broadcast(plugin.messages().prefixed("broadcast",
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.component("reward", reward.name()),
                    Placeholder.component("crate", crate.displayName())));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        }
    }
}
