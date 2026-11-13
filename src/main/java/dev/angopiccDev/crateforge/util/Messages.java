package dev.angopiccDev.crateforge.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Every player-facing string comes from config.yml so owners can restyle or translate it. */
public final class Messages {

    private String prefix = "";
    private ConfigurationSection section;

    public void reload(FileConfiguration config) {
        this.section = config.getConfigurationSection("messages");
        this.prefix = section == null ? "" : section.getString("prefix", "");
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(prefixed(key, resolvers));
    }

    public Component prefixed(String key, TagResolver... resolvers) {
        return Text.parse(prefix + raw(key), resolvers);
    }

    /** Without the prefix - for list lines and other multi-line output. */
    public Component plain(String key, TagResolver... resolvers) {
        return Text.parse(raw(key), resolvers);
    }

    private String raw(String key) {
        String value = section == null ? null : section.getString(key);
        return value != null ? value : "<red>Missing message 'messages." + key + "' in config.yml";
    }
}
