package dev.angopiccDev.crateforge.command;

import dev.angopiccDev.crateforge.CrateForgePlugin;
import dev.angopiccDev.crateforge.crate.Crate;
import dev.angopiccDev.crateforge.crate.CrateLocations.BlockKey;
import dev.angopiccDev.crateforge.util.Items;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /crate set &lt;crate&gt;                        - turn the block you're looking at into a crate
 * /crate remove                              - turn it back into a normal block
 * /crate givekey &lt;player|all&gt; &lt;crate&gt; [amount] - give physical keys
 * /crate list                                - list crates and how many are placed
 * /crate reload                              - reload config.yml and crates.yml
 */
public final class CrateCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("set", "remove", "givekey", "list", "reload");
    private static final int MAX_KEYS = 64 * 36;

    private final CrateForgePlugin plugin;

    public CrateCommand(CrateForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!sender.hasPermission("crateforge.admin")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> set(sender, args);
            case "remove" -> remove(sender);
            case "givekey" -> giveKey(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                int count = plugin.reloadAll();
                plugin.messages().send(sender, "reloaded", Placeholder.unparsed("count", String.valueOf(count)));
            }
            default -> plugin.messages().send(sender, "usage");
        }
        return true;
    }

    private void set(CommandSender sender, String[] args) {
        Block block = targetBlock(sender);
        if (block == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "usage");
            return;
        }
        Crate crate = plugin.crates().get(args[1]);
        if (crate == null) {
            plugin.messages().send(sender, "unknown-crate", Placeholder.unparsed("id", args[1]));
            return;
        }
        plugin.locations().set(block, crate.id());
        plugin.holograms().spawn(BlockKey.of(block), crate);
        plugin.messages().send(sender, "crate-set", Placeholder.component("crate", crate.displayName()));
    }

    private void remove(CommandSender sender) {
        Block block = targetBlock(sender);
        if (block == null) {
            return;
        }
        if (!plugin.locations().remove(block)) {
            plugin.messages().send(sender, "not-a-crate");
            return;
        }
        plugin.holograms().despawn(BlockKey.of(block));
        plugin.messages().send(sender, "crate-removed");
    }

    private void giveKey(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "usage");
            return;
        }
        Crate crate = plugin.crates().get(args[2]);
        if (crate == null) {
            plugin.messages().send(sender, "unknown-crate", Placeholder.unparsed("id", args[2]));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                amount = -1;
            }
            if (amount < 1 || amount > MAX_KEYS) {
                plugin.messages().send(sender, "bad-amount", Placeholder.unparsed("input", args[3]));
                return;
            }
        }

        Collection<? extends Player> targets;
        if (args[1].equalsIgnoreCase("all")) {
            targets = Bukkit.getOnlinePlayers();
        } else {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "unknown-player", Placeholder.unparsed("name", args[1]));
                return;
            }
            targets = List.of(target);
        }

        String amountText = String.valueOf(amount);
        for (Player target : targets) {
            if (Items.give(target, plugin.keys().create(crate), amount)) {
                plugin.messages().send(target, "inventory-full");
            }
            plugin.messages().send(target, "key-received",
                    Placeholder.unparsed("amount", amountText),
                    Placeholder.component("crate", crate.displayName()));
        }
        plugin.messages().send(sender, "key-given",
                Placeholder.unparsed("amount", amountText),
                Placeholder.component("crate", crate.displayName()),
                Placeholder.unparsed("player", args[1].equalsIgnoreCase("all") ? "everyone online" : args[1]));
    }

    private void list(CommandSender sender) {
        sender.sendMessage(plugin.messages().plain("list-header"));
        for (Crate crate : plugin.crates().all()) {
            sender.sendMessage(plugin.messages().plain("list-entry",
                    Placeholder.unparsed("id", crate.id()),
                    Placeholder.unparsed("rewards", String.valueOf(crate.rewards().size())),
                    Placeholder.unparsed("placed", String.valueOf(plugin.locations().countFor(crate.id())))));
        }
    }

    private Block targetBlock(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return null;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType().isAir()) {
            plugin.messages().send(sender, "look-at-block");
            return null;
        }
        return block;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!sender.hasPermission("crateforge.admin")) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && sub.equals("set")) {
            options.addAll(plugin.crates().ids());
        } else if (args.length == 2 && sub.equals("givekey")) {
            options.add("all");
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else if (args.length == 3 && sub.equals("givekey")) {
            options.addAll(plugin.crates().ids());
        } else if (args.length == 4 && sub.equals("givekey")) {
            options.addAll(List.of("1", "5", "10", "64"));
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], options, new ArrayList<>());
    }
}
