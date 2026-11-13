package dev.angopiccDev.crateforge.crate;

import dev.angopiccDev.crateforge.CrateForgePlugin;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Which blocks are crates. Stored in data.yml as "world;x;y;z;crateId" lines. */
public final class CrateLocations {

    /** A block position that survives chunk unloads and server restarts. */
    public record BlockKey(String world, int x, int y, int z) {

        public static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }

        public int chunkX() {
            return x >> 4;
        }

        public int chunkZ() {
            return z >> 4;
        }
    }

    private final CrateForgePlugin plugin;
    private final File file;
    private final Map<BlockKey, String> crates = new HashMap<>();

    public CrateLocations(CrateForgePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        crates.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String line : yaml.getStringList("crates")) {
            String[] parts = line.split(";");
            if (parts.length != 5) {
                plugin.getLogger().warning("Ignoring malformed line in data.yml: " + line);
                continue;
            }
            try {
                BlockKey key = new BlockKey(parts[0],
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                crates.put(key, parts[4]);
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Ignoring malformed line in data.yml: " + line);
            }
        }
    }

    private void save() {
        List<String> lines = new ArrayList<>(crates.size());
        crates.forEach((k, id) -> lines.add(k.world() + ";" + k.x() + ";" + k.y() + ";" + k.z() + ";" + id));
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("crates", lines);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", ex);
        }
    }

    public String crateAt(Block block) {
        return crates.get(BlockKey.of(block));
    }

    public void set(Block block, String crateId) {
        crates.put(BlockKey.of(block), crateId);
        save();
    }

    public boolean remove(Block block) {
        boolean removed = crates.remove(BlockKey.of(block)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public Map<BlockKey, String> all() {
        return Collections.unmodifiableMap(crates);
    }

    public long countFor(String crateId) {
        return crates.values().stream().filter(crateId::equals).count();
    }
}
