package dev.angopiccDev.crateforge.crate;

import dev.angopiccDev.crateforge.CrateForgePlugin;
import dev.angopiccDev.crateforge.crate.CrateLocations.BlockKey;
import dev.angopiccDev.crateforge.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Floating text above crates using vanilla TextDisplay entities - no hologram plugin dependency.
 *
 * The displays are non-persistent: they are never written to disk, so a crash or a removed
 * crate can't leave orphaned holograms behind. They vanish when their chunk unloads and are
 * respawned when it loads again.
 */
public final class HologramService {

    private final CrateForgePlugin plugin;
    private final Map<BlockKey, UUID> spawned = new HashMap<>();
    private boolean enabled = true;
    private double height = 1.6;

    public HologramService(CrateForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload(FileConfiguration config) {
        this.enabled = config.getBoolean("hologram.enabled", true);
        this.height = config.getDouble("hologram.height", 1.6);
    }

    public void spawnAllLoaded() {
        plugin.locations().all().forEach((key, crateId) -> {
            World world = Bukkit.getWorld(key.world());
            if (world != null && world.isChunkLoaded(key.chunkX(), key.chunkZ())) {
                spawn(key, plugin.crates().get(crateId));
            }
        });
    }

    public void onChunkLoad(Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        List<BlockKey> inChunk = new ArrayList<>();
        plugin.locations().all().forEach((key, crateId) -> {
            if (key.world().equals(worldName) && key.chunkX() == chunk.getX() && key.chunkZ() == chunk.getZ()) {
                inChunk.add(key);
            }
        });
        if (inChunk.isEmpty()) {
            return;
        }
        // Spawn on the next tick rather than inside the load event itself.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!chunk.isLoaded()) {
                return;
            }
            for (BlockKey key : inChunk) {
                spawn(key, plugin.crates().get(plugin.locations().all().get(key)));
            }
        });
    }

    public void spawn(BlockKey key, Crate crate) {
        despawn(key);
        if (!enabled || crate == null || crate.hologramLines().isEmpty()) {
            return;
        }
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        for (String line : crate.hologramLines()) {
            lines.add(Text.parse(line));
        }
        Component text = Component.join(JoinConfiguration.newlines(), lines);
        Location location = new Location(world, key.x() + 0.5, key.y() + height, key.z() + 0.5);

        TextDisplay display = world.spawn(location, TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(true);
            d.setPersistent(false);
        });
        spawned.put(key, display.getUniqueId());
    }

    public void despawn(BlockKey key) {
        UUID id = spawned.remove(key);
        if (id != null) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    public void despawnAll() {
        for (BlockKey key : new ArrayList<>(spawned.keySet())) {
            despawn(key);
        }
    }
}
