package com.songoda.skyblock.message;

import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.placeholder.PlaceholderManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MessageManager {

    private final SkyBlock plugin;

    public MessageManager(SkyBlock plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(CommandSender sender, String message) {

        if (message == null || message.equals("")) return;
        

        if (sender instanceof Player) {
            PlaceholderManager placeholderManager = plugin.getPlaceholderManager();
            Player player = (Player) sender;

            if (placeholderManager.isPlaceholderAPIEnabled()) {
                message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message.replace("&", "clr")).replace("clr", "&");
            }

            if (message.contains("\n") || message.contains("\\n")) {
                List<String> messages = new ArrayList<>();

                message = message.replace("\\n", "\n");

                for (String messageList : message.split("\n")) {
                    messages.add(ChatColor.translateAlternateColorCodes('&', messageList));
                }

                sender.sendMessage(messages.toArray(new String[messages.size()]));
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        } else {
            if (message.contains("\n") || message.contains("\\n")) {
                List<String> messages = new ArrayList<>();

                message = message.replace("\\n", "\n");

                for (String messageList : message.split("\n")) {
                    messages.add(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', messageList)));
                }

                sender.sendMessage(messages.toArray(new String[messages.size()]));
            } else {
                sender.sendMessage(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
            }
        }
    }

    public String replaceMessage(Player player, String message) {
        PlaceholderManager placeholderManager = plugin.getPlaceholderManager();

        if (placeholderManager.isPlaceholderAPIEnabled()) {
            message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message.replace("&", "clr")).replace("clr", "&");
        }

        return message;
    }
}
