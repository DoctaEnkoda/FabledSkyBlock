package com.songoda.skyblock.ban;

import com.eatthepath.uuid.FastUUID;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.api.event.island.IslandBanEvent;
import com.songoda.skyblock.api.event.island.IslandUnbanEvent;
import com.songoda.skyblock.config.FileManager.Config;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Ban {

    private UUID islandOwnerUUID;

    public Ban(UUID islandOwnerUUID) {
        this.islandOwnerUUID = islandOwnerUUID;
    }

    public UUID getOwnerUUID() {
        return islandOwnerUUID;
    }

    public void setOwnerUUID(UUID islandOwnerUUID) {
        this.islandOwnerUUID = islandOwnerUUID;
    }

    public boolean isBanned(UUID uuid) {
        return getBans().contains(uuid);
    }

    public Set<UUID> getBans() {
        SkyBlock plugin = SkyBlock.getInstance();

        Set<UUID> islandBans = new HashSet<>();

        for (String islandBanList : plugin.getFileManager()
                .getConfig(new File(new File(plugin.getDataFolder().toString() + "/ban-data"),
                        FastUUID.toString(islandOwnerUUID) + ".yml"))
                .getFileConfiguration().getStringList("Bans")) {

            UUID uuid = FastUUID.parseUUID(islandBanList);
            if (!Bukkit.getOfflinePlayer(uuid).hasPlayedBefore())
                continue;

            islandBans.add(uuid);
        }

        return islandBans;
    }

    public void addBan(UUID issuer, UUID banned) {
        SkyBlock plugin = SkyBlock.getInstance();

        IslandBanEvent islandBanEvent = new IslandBanEvent(
                plugin.getIslandManager().getIsland(Bukkit.getServer().getOfflinePlayer(islandOwnerUUID))
                        .getAPIWrapper(),
                Bukkit.getServer().getOfflinePlayer(issuer), Bukkit.getServer().getOfflinePlayer(banned));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> Bukkit.getServer().getPluginManager().callEvent(islandBanEvent));

        if (!islandBanEvent.isCancelled()) {
            FileConfiguration configLoad = plugin.getFileManager()
                    .getConfig(new File(new File(plugin.getDataFolder().toString() + "/ban-data"),
                            FastUUID.toString(islandOwnerUUID) + ".yml"))
                    .getFileConfiguration();

            List<String> islandBans = new ArrayList<>(configLoad.getStringList("Bans"));

            islandBans.add(banned.toString());
            configLoad.set("Bans", islandBans);
        }
    }

    public void removeBan(UUID uuid) {
        SkyBlock plugin = SkyBlock.getInstance();

        List<String> islandBans = new ArrayList<>();
        FileConfiguration configLoad = plugin.getFileManager()
                .getConfig(new File(new File(plugin.getDataFolder().toString() + "/ban-data"),
                        islandOwnerUUID.toString() + ".yml"))
                .getFileConfiguration();

        for (String islandBanList : configLoad.getStringList("Bans")) {
            if (!FastUUID.toString(uuid).equals(islandBanList)) {
                islandBans.add(islandBanList);
            }
        }

        configLoad.set("Bans", islandBans);

        Bukkit.getServer().getPluginManager()
                .callEvent(new IslandUnbanEvent(plugin.getIslandManager()
                        .getIsland(Bukkit.getServer().getOfflinePlayer(islandOwnerUUID)).getAPIWrapper(),
                        Bukkit.getServer().getOfflinePlayer(uuid)));
    }

    public void save() {
        SkyBlock plugin = SkyBlock.getInstance();

        Config config = plugin.getFileManager().getConfig(new File(
                new File(plugin.getDataFolder().toString() + "/ban-data"), islandOwnerUUID.toString() + ".yml"));

        try {
            config.getFileConfiguration().save(config.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
