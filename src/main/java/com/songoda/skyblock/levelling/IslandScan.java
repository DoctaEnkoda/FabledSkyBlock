package com.songoda.skyblock.levelling;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.api.event.island.IslandLevelChangeEvent;
import com.songoda.skyblock.blockscanner.BlockInfo;
import com.songoda.skyblock.blockscanner.BlockScanner;
import com.songoda.skyblock.blockscanner.ChunkLoader;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandLevel;
import com.songoda.skyblock.island.IslandWorld;
import com.songoda.skyblock.levelling.amount.AmountMaterialPair;
import com.songoda.skyblock.levelling.amount.BlockAmount;
import com.songoda.skyblock.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;

public final class IslandScan extends BukkitRunnable {

    private static final NumberFormat FORMATTER = NumberFormat.getInstance();

    private final Set<Location> doubleBlocks;
    private final Island island;
    private final Map<CompatibleMaterial, BlockAmount> amounts;
    private final Configuration language;
    private final int runEveryX;
    private final SkyBlock plugin;

    private int totalScanned;
    private int blocksSize;
    private Queue<BlockInfo> blocks;

    public IslandScan(SkyBlock plugin, Island island) {
        if (island == null) throw new IllegalArgumentException("island cannot be null");
        this.plugin = plugin;
        this.island = island;
        this.amounts = new EnumMap<>(CompatibleMaterial.class);
        this.language = this.plugin.getLanguage();
        this.runEveryX = language.getInt("Command.Island.Level.Scanning.Progress.Display-Every-X-Scan");
        this.doubleBlocks = new HashSet<>();
    }

    public IslandScan start() {
        final SkyBlock plugin = SkyBlock.getInstance();

        final FileConfiguration config = this.plugin.getConfiguration();
        final FileConfiguration islandData = plugin.getFileManager().getConfig(new File(new File(plugin.getDataFolder().toString() + "/island-data"), this.island.getOwnerUUID().toString() + ".yml")).getFileConfiguration();

        final boolean hasNether = config.getBoolean("Island.World.Nether.Enable") && islandData.getBoolean("Unlocked.Nether", false);
        final boolean hasEnd = config.getBoolean("Island.World.End.Enable") && islandData.getBoolean("Unlocked.End", false);

        final Map<World, List<ChunkSnapshot>> snapshots = new HashMap<>(3);


        if (plugin.isPaperAsync()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                initScan(plugin, hasNether, hasEnd, snapshots);
            });
        } else {
            initScan(plugin, hasNether, hasEnd, snapshots);
        }


        return this;
    }

    private void initScan(SkyBlock plugin, boolean hasNether, boolean hasEnd, Map<World, List<ChunkSnapshot>> snapshots) {
        populate(snapshots, IslandWorld.Normal, plugin.isPaperAsync(), () -> {

            if (hasNether) {
                populate(snapshots, IslandWorld.Nether, plugin.isPaperAsync(), () -> {
                    if (hasEnd) {
                        populate(snapshots, IslandWorld.End, plugin.isPaperAsync(), () -> {
                            BlockScanner.startScanner(snapshots, island, true, true, true, false, (blocks) -> {
                                this.blocks = blocks;
                                this.blocksSize = blocks.size();
                                this.runTaskTimer(SkyBlock.getInstance(), 20, 20);
                            });
                        });
                    } else {
                        BlockScanner.startScanner(snapshots, island, true, true, true, false, (blocks) -> {
                            this.blocks = blocks;
                            this.blocksSize = blocks.size();
                            this.runTaskTimer(SkyBlock.getInstance(), 20, 20);
                        });
                    }
                });
            } else {
                BlockScanner.startScanner(snapshots, island, true, true, true, false, (blocks) -> {
                    this.blocks = blocks;
                    this.blocksSize = blocks.size();
                    this.runTaskTimer(SkyBlock.getInstance(), 20, 20);
                });
            }
        });
    }

    private void finalizeBlocks() {

        final Map<String, Long> materials = new HashMap<>(amounts.size());

        for (Entry<CompatibleMaterial, BlockAmount> entry : amounts.entrySet()) {
            materials.put(entry.getKey().name(), entry.getValue().getAmount());
        }

        final IslandLevel level = island.getLevel();

        level.setMaterials(materials);
        level.setLastCalculatedLevel(level.getLevel());
        level.setLastCalculatedPoints(level.getPoints());

        Bukkit.getServer().getPluginManager().callEvent(new IslandLevelChangeEvent(island.getAPIWrapper(), island.getAPIWrapper().getLevel()));
    }

    private int executions;

    @Override
    public void run() {
        executions += 1;

        int scanned = 0;

        for (Iterator<BlockInfo> it = blocks.iterator(); it.hasNext(); ) {

            final BlockInfo info = it.next();

            if (scanned == 8500) break;

            final AmountMaterialPair pair = SkyBlock.getInstance().getLevellingManager().getAmountAndType(this, info);

            if (pair.getType() != null) {

                BlockAmount cachedAmount = amounts.get(pair.getType());

                if (cachedAmount == null) {
                    cachedAmount = new BlockAmount(pair.getAmount());
                } else {
                    cachedAmount.increaseAmount(pair.getAmount());
                }

                amounts.put(pair.getType(), cachedAmount);
            }

            scanned += 1;
            it.remove();
        }

        totalScanned += scanned;

        if (blocks.isEmpty()) {
            finalizeBlocks();
            cancel();
            SkyBlock.getInstance().getLevellingManager().stopScan(island);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (language.getBoolean("Command.Island.Level.Scanning.Progress.Should-Display-Message") && executions == 1 || totalScanned == blocksSize || executions % runEveryX == 0) {

                double percent = ((double) totalScanned / (double) blocksSize) * 100;

                if(Double.isNaN(percent)) {
                    percent = 0d;
                }
                
                String message = language.getString("Command.Island.Level.Scanning.Progress.Message");
                message = message.replace("%current_scanned_blocks%", String.valueOf(totalScanned));
                message = message.replace("%max_blocks%", String.valueOf(blocksSize));
                message = message.replace("%percent_whole%", String.valueOf((int) percent));
                message = message.replace("%percent%", FORMATTER.format(percent));

                final boolean displayComplete = totalScanned == blocksSize && language.getBoolean("Command.Island.Level.Scanning.Finished.Should-Display-Message");
                final MessageManager messageManager = SkyBlock.getInstance().getMessageManager();

                for (Player player : SkyBlock.getInstance().getIslandManager().getPlayersAtIsland(island)) {

                    messageManager.sendMessage(player, message);
                    if (displayComplete)
                        messageManager.sendMessage(player, language.getString("Command.Island.Level.Scanning.Finished.Message"));

                    // Check for level ups
                    island.getLevel().checkLevelUp();
                }
            }
        });
    }

    private void populate(Map<World, List<ChunkSnapshot>> snapshots, IslandWorld world, boolean paper, PopulateTask task) {

        final SkyBlock plugin = SkyBlock.getInstance();
    
        List<ChunkSnapshot> positions = new LinkedList<>();
        
        ChunkLoader.startChunkLoadingPerChunk(island, IslandWorld.Normal, paper, (chunkCompletableFuture) ->
                positions.add(chunkCompletableFuture.join().getChunkSnapshot()),
                value -> {
            snapshots.put(plugin.getWorldManager().getWorld(world), positions);
            task.onComplete();
        });
    }

    private interface PopulateTask {
        void onComplete();
    }

    public Set<Location> getDoubleBlocks() {
        return doubleBlocks;
    }

}
