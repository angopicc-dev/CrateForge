package dev.angopiccDev.crateforge.crate;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record Crate(String id, Component displayName, Key key, List<String> hologramLines, List<Reward> rewards) {

    /** How this crate's key item looks. */
    public record Key(Material material, Component name, List<Component> lore) {
    }

    public double totalWeight() {
        double total = 0;
        for (Reward reward : rewards) {
            total += reward.weight();
        }
        return total;
    }

    /** Chance of a reward in percent, as shown to players in the preview. */
    public double chance(Reward reward) {
        return reward.weight() / totalWeight() * 100.0;
    }

    /** Weighted random pick: each reward owns a slice of [0, total) proportional to its weight. */
    public Reward roll() {
        double pick = ThreadLocalRandom.current().nextDouble(totalWeight());
        for (Reward reward : rewards) {
            pick -= reward.weight();
            if (pick < 0) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1); // only reachable through floating-point rounding
    }
}
