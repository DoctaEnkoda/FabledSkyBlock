package com.songoda.skyblock.playerdata;

import com.eatthepath.uuid.FastUUID;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.bank.Transaction;
import com.songoda.skyblock.config.FileManager.Config;
import com.songoda.skyblock.confirmation.Confirmation;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.menus.MenuType;
import com.songoda.skyblock.utils.structure.Area;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerData {
    
    private final SkyBlock plugin;
    private final UUID uuid;
    private UUID islandOwnerUUID;
    private UUID ownershipUUID;

    private final List<MenuPage> pages;
    private int playTime;
    private int visitTime;
    private int confirmationTime;

    private Confirmation confirmation;

    private Object type;
    private Object sort;

    private final Area area;
    
    private boolean chatSpy;
    private final Set<UUID> spiedIslands;

    private boolean chat;
    private boolean preview;

    private Object viewer;

    private List<Transaction> transactions;

    public PlayerData(Player player) {
        this.plugin = SkyBlock.getInstance();
        
        uuid = player.getUniqueId();
        islandOwnerUUID = null;

        pages = new ArrayList<>();

        confirmationTime = 0;
        playTime = getConfig().getFileConfiguration().getInt("Statistics.Island.Playtime");

        area = new Area();

        chatSpy = getConfig().getFileConfiguration().getBoolean("ChatSpy", false);
        spiedIslands = new HashSet<>();
    
        if (getConfig().getFileConfiguration().getString("ChatSpiedIslands") != null) {
            for (String islandUUID : getConfig().getFileConfiguration().getStringList("ChatSpiedIslands")) {
                spiedIslands.add(FastUUID.parseUUID(islandUUID));
            }
        }
        
        chat = false;
        preview = false;
        transactions = new ArrayList<>();
        FileConfiguration configLoad = getConfig().getFileConfiguration();
        for (int i = 0;i< configLoad.getInt("Bank.Transactions.Size");i++) {
            Transaction t = new Transaction();
            t.action = Transaction.Type.valueOf(configLoad.getString("Bank.Transactions."+i+".Action"));
            t.amount = Float.parseFloat(Objects.requireNonNull(configLoad.getString("Bank.Transactions." + i + ".Amount")));
            t.player = Bukkit.getOfflinePlayer(FastUUID.parseUUID(Objects.requireNonNull(configLoad.getString("Bank.Transactions." + i + ".Player"))));
            Date d = new Date();
            d.setTime(configLoad.getLong("Bank.Transactions."+i+".Date"));
            t.timestamp = d;
            String visibility = configLoad.getString("Bank.Transactions."+i+".Visibility");
            if(visibility != null){
                t.visibility = Transaction.Visibility.valueOf(visibility);
            } else {
                t.visibility = Transaction.Visibility.USER; // Defaulting this as it's a new field
            }
            transactions.add(t);
        }
    }

    public int getPage(MenuType type) {
        for(MenuPage menu : pages){
            if(menu.getType().equals(type)){
                return menu.getPage();
            }
        }
        return 1;
    }

    public void setPage(MenuType type, int page) {
        for(MenuPage menu : pages){
            if(menu.getType().equals(type)){
               menu.setPage(page);
               return;
            }
        }
        pages.add(new MenuPage(type, page));
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public Object getSort() {
        return sort;
    }

    public void setSort(Object sort) {
        this.sort = sort;
    }

    public UUID getIsland() {
        return islandOwnerUUID;
    }

    public void setIsland(UUID islandOwnerUUID) {
        this.islandOwnerUUID = islandOwnerUUID;
    }

    public UUID getOwnership() {
        return ownershipUUID;
    }

    public void setOwnership(UUID ownershipUUID) {
        this.ownershipUUID = ownershipUUID;
    }

    public int getConfirmationTime() {
        return confirmationTime;
    }

    public void setConfirmationTime(int confirmationTime) {
        this.confirmationTime = confirmationTime;
    }

    public Confirmation getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(Confirmation confirmation) {
        this.confirmation = confirmation;
    }

    public boolean hasConfirmation() {
        return confirmationTime > 0;
    }

    public int getPlaytime() {
        return playTime;
    }

    public void setPlaytime(int playTime) {
        this.playTime = playTime;
    }

    public boolean isScoreboard() {
        return getConfig().getFileConfiguration().getBoolean("Scoreboard", true);
    }

    public void setScoreboard(boolean scoreboard) {
        getConfig().getFileConfiguration().set("Scoreboard", scoreboard);
    }
    
    public boolean isPreview() {
        return preview;
    }
    
    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public int getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(int visitTime) {
        this.visitTime = visitTime;
    }

    public String getMemberSince() {
        return getConfig().getFileConfiguration().getString("Statistics.Island.Join");
    }

    public void setMemberSince(String date) {
        getConfig().getFileConfiguration().set("Statistics.Island.Join", date);
    }

    public UUID getOwner() {
        String islandOwnerUUID = getConfig().getFileConfiguration().getString("Island.Owner");
        return (islandOwnerUUID == null) ? null : FastUUID.parseUUID(islandOwnerUUID);
    }

    public void setOwner(UUID islandOwnerUUID) {
        if (islandOwnerUUID == null) {
            getConfig().getFileConfiguration().set("Island.Owner", null);
        } else {
            getConfig().getFileConfiguration().set("Island.Owner", islandOwnerUUID.toString());
        }
    }

    public String[] getTexture() {
        FileConfiguration configLoad = getConfig().getFileConfiguration();

        return new String[] { configLoad.getString("Texture.Signature"), configLoad.getString("Texture.Value") };
    }

    public void setTexture(String signature, String value) {
        getConfig().getFileConfiguration().set("Texture.Signature", signature);
        getConfig().getFileConfiguration().set("Texture.Value", value);
    }

    public String getLastOnline() {
        return getConfig().getFileConfiguration().getString("Statistics.Island.LastOnline");
    }

    public void setLastOnline(String date) {
        getConfig().getFileConfiguration().set("Statistics.Island.LastOnline", date);
    }
    
    public long getIslandCreationCount() {
        return getConfig().getFileConfiguration().getLong("Statistics.Island.IslandCreationCount");
    }

    public long getIslandDeletionCount() {
        return getConfig().getFileConfiguration().getLong("Statistics.Island.IslandDeleteCount");
    }

    public void setIslandCreationCount(long newNumber) {
        getConfig().getFileConfiguration().set("Statistics.Island.IslandCreationCount", newNumber);
    }

    public void setIslandDeletionCount(long newNumber) {
        getConfig().getFileConfiguration().set("Statistics.Island.IslandDeleteCount", newNumber);
    }


    public Area getArea() {
        return area;
    }

    public boolean isChat() {
        return chat;
    }

    public void setChat(boolean chat) {
        this.chat = chat;
    }

    public Object getViewer() {
        return viewer;
    }

    public void setViewer(Object viewer) {
        this.viewer = viewer;
    }

    public void deleteTransactions() {
        Config config = getConfig();
        FileConfiguration configLoad = config.getFileConfiguration();
        configLoad.set("Bank.Transactions",null);
        configLoad.set("Bank.Transactions.Size",0);
        try {
            configLoad.save(config.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        transactions = plugin.getBankManager().getTransactionList(getPlayerUUID());
        Config config = getConfig();
        FileConfiguration configLoad = config.getFileConfiguration();
        configLoad.set("Statistics.Island.Playtime", getPlaytime());
        if (transactions != null) {
            configLoad.set("Bank.Transactions.Size", transactions.size());
            for (int i = 0; i < transactions.size(); i++) {
                Transaction t = transactions.get(i);
                configLoad.set("Bank.Transactions." + i + ".Action", t.action.name());
                configLoad.set("Bank.Transactions." + i + ".Amount", t.amount);
                configLoad.set("Bank.Transactions." + i + ".Player", t.player.getUniqueId().toString());
                configLoad.set("Bank.Transactions." + i + ".Date", t.timestamp.getTime());
                configLoad.set("Bank.Transactions." + i + ".Visibility", t.visibility.name());
            }
        }else {
            configLoad.set("Bank.Transactions.Size", 0);
        }
    
        configLoad.set("ChatSpy", chatSpy);
        List<String> tempSpiedIslands = new ArrayList<>();
        for(UUID uuid : spiedIslands){
            tempSpiedIslands.add(FastUUID.toString(uuid));
        }
        configLoad.set("ChatSpiedIslands", tempSpiedIslands);
        
        try {
            configLoad.save(config.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Config getConfig() {
        SkyBlock plugin = SkyBlock.getInstance();
        return plugin.getFileManager().getConfig(new File(new File(plugin.getDataFolder().toString() + "/player-data"), FastUUID.toString(uuid) + ".yml"));
    }
    
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() {
        return uuid;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public boolean isChatSpy() {
        return chatSpy;
    }
    
    public void setChatSpy(boolean chatSpy) {
        this.chatSpy = chatSpy;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }
    
    public void addChatSpyIsland(UUID uuid) {
        spiedIslands.add(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }
    
    public boolean isChatSpyIsland(UUID uuid) {
        return spiedIslands.contains(uuid);
    }
    
    public void removeChatSpyIsland(UUID uuid) {
        spiedIslands.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }
    
    public Set<UUID> getChatSpyIslands() {
        return new HashSet<>(spiedIslands);
    }
    
    public void addChatSpyIsland(Island island) {
        this.addChatSpyIsland(island.getOwnerUUID());
    }
    
    public boolean isChatSpyIsland(Island island) {
        return this.isChatSpyIsland(island.getOwnerUUID());
    }
    
    public void removeChatSpyIsland(Island island) {
        this.removeChatSpyIsland(island.getOwnerUUID());
    }
    
    public boolean isGlobalChatSpy() {
        return spiedIslands.isEmpty();
    }
    
    public void enableGlobalChatSpy() {
        spiedIslands.clear();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }
}
