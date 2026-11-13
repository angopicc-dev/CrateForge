package dev.angopiccDev.crateforge.crate;

import dev.angopiccDev.crateforge.CrateForgePlugin;
import dev.angopiccDev.crateforge.util.Items;
import dev.angopiccDev.crateforge.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads crates.yml. A broken crate is skipped with a clear warning; the rest still load. */
public final class CrateRegistry {

    private final CrateForgePlugin plugin;
    private final Map<String, Crate> crates = new LinkedHashMap<>();

    public CrateRegistry(CrateForgePlugin plugin) {
        this.plugin = plugin;
    }

    public int load() {
        File file = new File(plugin.getDataFolder(), "crates.yml");
        if (!file.exists()) {
            plugin.saveResource("crates.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        crates.clear();
        ConfigurationSection root = yaml.getConfigurationSection("crates");
        if (root == null) {
            plugin.getLogger().warning("crates.yml has no 'crates:' section - no crates loaded.");
            return 0;
        }
        for (String rawId : root.getKeys(false)) {
            try {
                Crate crate = parseCrate(rawId.toLowerCase(Locale.ROOT), root.getConfigurationSection(rawId));
                crates.put(crate.id(), crate);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping crate '" + rawId + "': " + ex.getMessage());
            }
        }
        return crates.size();
    }

    private Crate parseCrate(String id, ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("not a section");
        }
        String where = "crates." + id;

        ConfigurationSection keySection = section.getConfigurationSection("key");
        if (keySection == null) {
            throw new IllegalArgumentException(where + ".key is missing");
        }
        Material keyMaterial = Material.matchMaterial(keySection.getString("material", "TRIPWIRE_HOOK"));
        if (keyMaterial == null || keyMaterial.isAir() || !keyMaterial.isItem()) {
            throw new IllegalArgumentException(where + ".key.material is not an item material");
        }
        Crate.Key key = new Crate.Key(
                keyMaterial,
                Text.item(keySection.getString("name", id + " key")),
                Text.itemLines(keySection.getStringList("lore")));

        ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection == null || rewardsSection.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException(where + ".rewards is missing or empty");
        }
        List<Reward> rewards = new ArrayList<>();
        for (String rewardId : rewardsSection.getKeys(false)) {
            ConfigurationSection r = rewardsSection.getConfigurationSection(rewardId);
            String rewardWhere = where + ".rewards." + rewardId;
            if (r == null) {
                throw new IllegalArgumentException(rewardWhere + " is not a section");
            }
            double weight = r.getDouble("weight", 0);
            if (weight <= 0) {
                throw new IllegalArgumentException(rewardWhere + ".weight must be above 0");
            }
            ConfigurationSection itemSection = r.getConfigurationSection("item");
            rewards.add(new Reward(
                    rewardId,
                    weight,
                    Items.fromConfig(itemSection, rewardWhere + ".item"),
                    Math.max(1, Math.min(64 * 36, itemSection.getInt("amount", 1))),
                    r.getBoolean("give-item", true),
                    List.copyOf(r.getStringList("commands")),
                    r.getBoolean("broadcast", false)));
        }

        return new Crate(
                id,
                Text.item(section.getString("display-name", id)),
                key,
                List.copyOf(section.getStringList("hologram")),
                List.copyOf(rewards));
    }

    public Crate get(String id) {
        return id == null ? null : crates.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Crate> all() {
        return Collections.unmodifiableCollection(crates.values());
    }

    public List<String> ids() {
        return new ArrayList<>(crates.keySet());
    }
}
