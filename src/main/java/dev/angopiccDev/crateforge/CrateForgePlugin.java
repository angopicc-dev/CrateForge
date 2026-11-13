package dev.angopiccDev.crateforge;

import dev.angopiccDev.crateforge.command.CrateCommand;
import dev.angopiccDev.crateforge.crate.CrateLocations;
import dev.angopiccDev.crateforge.crate.CrateRegistry;
import dev.angopiccDev.crateforge.crate.HologramService;
import dev.angopiccDev.crateforge.crate.KeyService;
import dev.angopiccDev.crateforge.gui.SessionManager;
import dev.angopiccDev.crateforge.listener.CrateListener;
import dev.angopiccDev.crateforge.listener.MenuListener;
import dev.angopiccDev.crateforge.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public final class CrateForgePlugin extends JavaPlugin {

    private final Messages messages = new Messages();
    private KeyService keys;
    private CrateRegistry crates;
    private CrateLocations locations;
    private HologramService holograms;
    private SessionManager sessions;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        keys = new KeyService(this);
        crates = new CrateRegistry(this);
        locations = new CrateLocations(this);
        holograms = new HologramService(this);
        sessions = new SessionManager(this);

        int loaded = reloadAll();
        getLogger().info("Loaded " + loaded + " crate(s), " + locations.all().size() + " placed.");

        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        PluginCommand command = getCommand("crate");
        if (command != null) {
            CrateCommand executor = new CrateCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (sessions != null) {
            sessions.finishAll();
        }
        if (holograms != null) {
            holograms.despawnAll();
        }
    }

    /** Reloads everything from disk. Returns the number of crates loaded. */
    public int reloadAll() {
        sessions.finishAll(); // never leave someone mid-spin across a reload

        reloadConfig();
        messages.reload(getConfig());
        holograms.reload(getConfig());
        int count = crates.load();
        locations.load();

        Set<String> missing = new HashSet<>();
        locations.all().values().forEach(id -> {
            if (crates.get(id) == null) {
                missing.add(id);
            }
        });
        if (!missing.isEmpty()) {
            getLogger().warning("Placed crates reference unknown crate ids " + missing
                    + " - they won't open until those ids exist in crates.yml again.");
        }

        holograms.despawnAll();
        holograms.spawnAllLoaded();
        return count;
    }

    public Messages messages() {
        return messages;
    }

    public KeyService keys() {
        return keys;
    }

    public CrateRegistry crates() {
        return crates;
    }

    public CrateLocations locations() {
        return locations;
    }

    public HologramService holograms() {
        return holograms;
    }

    public SessionManager sessions() {
        return sessions;
    }
}
