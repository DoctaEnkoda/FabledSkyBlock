package com.songoda.skyblock.levelling;

import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.api.event.island.IslandLevelChangeEvent;
import com.songoda.skyblock.config.FileManager.Config;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandLevel;
import com.songoda.skyblock.island.IslandManager;
import com.songoda.skyblock.island.IslandWorld;
import com.songoda.skyblock.menus.Levelling;
import com.songoda.skyblock.message.MessageManager;
import com.songoda.skyblock.stackable.Stackable;
import com.songoda.skyblock.stackable.StackableManager;
import com.songoda.skyblock.utils.version.Materials;
import com.songoda.skyblock.utils.version.NMSUtil;
import com.songoda.skyblock.utils.version.Sounds;
import com.songoda.skyblock.world.WorldManager;
import org.bukkit.*;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class LevellingManager {

    private final SkyBlock skyblock;

    private Island activelyScanningIsland = null;
    private Queue<QueuedIsland> islandsInQueue = new LinkedList<>();
    private List<LevellingMaterial> materialStorage = new ArrayList<>();

    public LevellingManager(SkyBlock skyblock) {
        this.skyblock = skyblock;

        registerMaterials();
    }

    public void calculatePoints(Player player, Island island) {
        IslandManager islandManager = skyblock.getIslandManager();
        WorldManager worldManager = skyblock.getWorldManager();
        MessageManager messageManager = skyblock.getMessageManager();
        StackableManager stackableManager = skyblock.getStackableManager();

        FileConfiguration languageConfig = this.skyblock.getFileManager().getConfig(new File(this.skyblock.getDataFolder(), "language.yml")).getFileConfiguration();

        if (!this.isIslandLevelBeingScanned(island) && player != null && islandManager.getIslandPlayerAt(player) != island) {
            messageManager.sendMessage(player, languageConfig.getString("Command.Island.Level.Scanning.NotOnIsland.Message"));
            return;
        }

        if (this.activelyScanningIsland != null) {
            this.islandsInQueue.add(new QueuedIsland(player, island));

            String queuedMessage = languageConfig.getString("Command.Island.Level.Scanning.Queued.Message");
            islandManager.getPlayersAtIsland(island).forEach(x -> messageManager.sendMessage(x, queuedMessage));

            return;
        }

        this.activelyScanningIsland = island;

        String nowScanningMessage = languageConfig.getString("Command.Island.Level.Scanning.Started.Message");
        islandManager.getPlayersAtIsland(island).forEach(x -> messageManager.sendMessage(x, nowScanningMessage));

        Chunk chunk = new Chunk(skyblock, island);
        chunk.prepareInitial();

        int NMSVersion = NMSUtil.getVersionNumber();

        int height = 0;

        for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
            org.bukkit.World world = worldManager.getWorld(worldList);

            if (height == 0 || height > world.getMaxHeight()) {
                height = world.getMaxHeight();
            }
        }

        int worldMaxHeight = height;

        boolean isEpicSpawnersEnabled = Bukkit.getPluginManager().isPluginEnabled("EpicSpawners");
        boolean isUltimateStackerEnabled = Bukkit.getPluginManager().isPluginEnabled("UltimateStacker");

        Map<LevellingData, Long> levellingData = new HashMap<>();
        Set<Location> spawnerLocations = new HashSet<>(); // These have to be checked synchronously :(
        Set<Location> epicSpawnerLocations = new HashSet<>();
        Set<Location> ultimateStackerSpawnerLocations = new HashSet<>();

        List<Material> blacklistedMaterials = new ArrayList<>();
        blacklistedMaterials.add(Materials.AIR.getPostMaterial());
        blacklistedMaterials.add(Materials.WATER.getPostMaterial());
        blacklistedMaterials.add(Materials.LEGACY_STATIONARY_WATER.getPostMaterial());
        blacklistedMaterials.add(Materials.LAVA.getPostMaterial());
        blacklistedMaterials.add(Materials.LEGACY_STATIONARY_LAVA.getPostMaterial());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!chunk.isReadyToScan())
                    return;

                try {
                    if (chunk.isFinished()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(skyblock, () -> finalizeMaterials(levellingData, spawnerLocations, epicSpawnerLocations, ultimateStackerSpawnerLocations, player, island), 1);
                        cancel();
                        return;
                    }

                    for (LevelChunkSnapshotWrapper chunkSnapshotList : chunk.getAvailableChunkSnapshots()) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 0; y < worldMaxHeight; y++) {
                                    ChunkSnapshot chunkSnapshot = chunkSnapshotList.getChunkSnapshot();

                                    try {
                                        org.bukkit.Material blockMaterial;
                                        int blockData = 0;
                                        EntityType spawnerType = null;

                                        if (NMSVersion > 12) {
                                            blockMaterial = chunkSnapshot.getBlockType(x, y, z);
                                        } else {
                                            LegacyChunkSnapshotData data = LegacyChunkSnapshotFetcher.fetch(chunkSnapshot, x, y, z);

                                            blockMaterial = data.getMaterial();
                                            blockData = data.getData();
                                        }

                                        if (blacklistedMaterials.contains(blockMaterial))
                                            continue;

                                        long amount = 1;

                                        if (blockMaterial == Materials.SPAWNER.parseMaterial()) {
                                            World world = Bukkit.getWorld(chunkSnapshot.getWorldName());
                                            Location location = new Location(world, chunkSnapshot.getX() * 16 + x, y, chunkSnapshot.getZ() * 16 + z);

                                            if (isEpicSpawnersEnabled) {
                                                com.songoda.epicspawners.EpicSpawners epicSpawners = com.songoda.epicspawners.EpicSpawners.getInstance();
                                                if (epicSpawners.getSpawnerManager().isSpawner(location)) {
                                                    com.songoda.epicspawners.spawners.spawner.Spawner spawner = epicSpawners.getSpawnerManager().getSpawnerFromWorld(location);
                                                    if (spawner != null)
                                                        epicSpawnerLocations.add(location);
                                                    continue;
                                                }
                                            } else if (isUltimateStackerEnabled) {
                                                com.songoda.ultimatestacker.spawner.SpawnerStack spawnerStack = com.songoda.ultimatestacker.UltimateStacker.getInstance().getSpawnerStackManager().getSpawner(location);
                                                if (spawnerStack != null)
                                                    ultimateStackerSpawnerLocations.add(location);
                                                continue;
                                            }

                                            if (chunkSnapshotList.hasWildStackerData()) {
                                                com.bgsoftware.wildstacker.api.objects.StackedSnapshot snapshot = ((WildStackerChunkSnapshotWrapper) chunkSnapshotList).getStackedSnapshot();
                                                if (snapshot.isStackedSpawner(location)) {
                                                    Map.Entry<Integer, EntityType> spawnerData = snapshot.getStackedSpawner(location);
                                                    amount = spawnerData.getKey();
                                                    spawnerType = spawnerData.getValue();
                                                }
                                            }

                                            if (spawnerType == null) {
                                                spawnerLocations.add(location);
                                                continue;
                                            }
                                        } else {
                                            if (chunkSnapshotList.hasWildStackerData()) {
                                                com.bgsoftware.wildstacker.api.objects.StackedSnapshot snapshot = ((WildStackerChunkSnapshotWrapper) chunkSnapshotList).getStackedSnapshot();
                                                World world = Bukkit.getWorld(chunkSnapshot.getWorldName());
                                                Location location = new Location(world, chunkSnapshot.getX() * 16 + x, y, chunkSnapshot.getZ() * 16 + z);
                                                if (snapshot.isStackedBarrel(location)) {
                                                    Map.Entry<Integer, Material> barrelData = snapshot.getStackedBarrel(location);
                                                    amount = barrelData.getKey();
                                                    blockMaterial = barrelData.getValue();
                                                    if (NMSUtil.getVersionNumber() > 12 && blockMaterial.name().startsWith("LEGACY_")) {
                                                        blockMaterial = Material.matchMaterial(blockMaterial.name().replace("LEGACY_", ""));
                                                    }
                                                }
                                            }

                                            if (stackableManager != null && stackableManager.getStackableMaterials().contains(blockMaterial) && amount == 1) {
                                                World world = Bukkit.getWorld(chunkSnapshot.getWorldName());
                                                Location location = new Location(world, chunkSnapshot.getX() * 16 + x, y, chunkSnapshot.getZ() * 16 + z);
                                                if (stackableManager.isStacked(location)) {
                                                    Stackable stackable = stackableManager.getStack(location, blockMaterial);
                                                    if (stackable != null) {
                                                        amount = stackable.getSize();
                                                    }
                                                }
                                            }
                                        }

                                        LevellingData data = new LevellingData(blockMaterial, (byte) blockData, spawnerType);
                                        Long totalAmountInteger = levellingData.get(data);
                                        long totalAmount = totalAmountInteger == null ? amount : totalAmountInteger + amount;
                                        levellingData.put(data, totalAmount);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    chunk.prepareNextChunkSnapshots();
                } catch (Exception ex) {
                    skyblock.getLogger().severe("An error occurred while scanning an island. This is a severe error.");
                }
            }
        }.runTaskTimerAsynchronously(skyblock, 0L, 1L);
    }

    private void finalizeMaterials(Map<LevellingData, Long> levellingData, Set<Location> spawnerLocations, Set<Location> epicSpawnerLocations, Set<Location> ultimateStackerSpawnerLocations, Player player, Island island) {
        for (Location location : spawnerLocations) {
            if (!(location.getBlock().getState() instanceof CreatureSpawner))
                continue;

            int amount = 1;
            EntityType spawnerType = ((CreatureSpawner) location.getBlock().getState()).getSpawnedType();

            LevellingData data = new LevellingData(Materials.SPAWNER.parseMaterial(), (byte) 0, spawnerType);
            Long totalAmountInteger = levellingData.get(data);
            long totalAmount = totalAmountInteger == null ? amount : totalAmountInteger + amount;
            levellingData.put(data, totalAmount);
        }

        for (Location location : epicSpawnerLocations) {
            com.songoda.epicspawners.EpicSpawners epicSpawners = com.songoda.epicspawners.EpicSpawners.getInstance();
            if (epicSpawners.getSpawnerManager().isSpawner(location)) {
                com.songoda.epicspawners.spawners.spawner.Spawner spawner = epicSpawners.getSpawnerManager().getSpawnerFromWorld(location);
                if (spawner == null)
                    continue;

                int amount = spawner.getFirstStack().getStackSize();
                EntityType spawnerType = spawner.getCreatureSpawner().getSpawnedType();

                LevellingData data = new LevellingData(Materials.SPAWNER.parseMaterial(), (byte) 0, spawnerType);
                Long totalAmountInteger = levellingData.get(data);
                long totalAmount = totalAmountInteger == null ? amount : totalAmountInteger + amount;
                levellingData.put(data, totalAmount);
            }
        }

        for (Location location : ultimateStackerSpawnerLocations) {
            com.songoda.ultimatestacker.spawner.SpawnerStack spawnerStack = com.songoda.ultimatestacker.UltimateStacker.getInstance().getSpawnerStackManager().getSpawner(location);
            if (spawnerStack == null)
                continue;

            int amount = spawnerStack.getAmount();
            EntityType spawnerType = ((CreatureSpawner) location.getBlock().getState()).getSpawnedType();

            LevellingData data = new LevellingData(Materials.SPAWNER.parseMaterial(), (byte) 0, spawnerType);
            Long totalAmountInteger = levellingData.get(data);
            long totalAmount = totalAmountInteger == null ? amount : totalAmountInteger + amount;
            levellingData.put(data, totalAmount);
        }

        Map<String, Long> materials = new HashMap<>();
        for (LevellingData data : levellingData.keySet()) {
            long amount = levellingData.get(data);
            if (data.getMaterials() != null) {
                materials.put(data.getMaterials().name(), amount);
            }
        }

        if (materials.size() == 0) {
            if (player != null) {
                skyblock.getMessageManager().sendMessage(player, skyblock.getFileManager()
                        .getConfig(new File(skyblock.getDataFolder(), "language.yml"))
                        .getFileConfiguration().getString("Command.Island.Level.Materials.Message"));
                skyblock.getSoundManager().playSound(player, Sounds.VILLAGER_NO.bukkitSound(), 1.0F, 1.0F);
            }
        } else {
            IslandLevel level = island.getLevel();
            level.setLastCalculatedPoints(level.getPoints());
            level.setLastCalculatedLevel(level.getLevel());
            level.setMaterials(materials);

            Bukkit.getServer().getPluginManager().callEvent(new IslandLevelChangeEvent(island.getAPIWrapper(), island.getAPIWrapper().getLevel()));

            if (player != null) {
                Levelling.getInstance().open(player);
            }
        }

        MessageManager messageManager = skyblock.getMessageManager();
        FileConfiguration languageConfig = this.skyblock.getFileManager().getConfig(new File(this.skyblock.getDataFolder(), "language.yml")).getFileConfiguration();
        String nowScanningMessage = languageConfig.getString("Command.Island.Level.Scanning.Done.Message");
        skyblock.getIslandManager().getPlayersAtIsland(island).forEach(x -> messageManager.sendMessage(x, nowScanningMessage));

        this.activelyScanningIsland = null;
        QueuedIsland nextInQueue = this.islandsInQueue.poll();
        if (nextInQueue != null)
            this.calculatePoints(nextInQueue.getPlayer(), nextInQueue.getIsland());
    }

    public void registerMaterials() {
        Config config = skyblock.getFileManager().getConfig(new File(skyblock.getDataFolder(), "levelling.yml"));
        FileConfiguration configLoad = config.getFileConfiguration();

        if (configLoad.getString("Materials") != null) {
            for (String materialKey : configLoad.getConfigurationSection("Materials").getKeys(false)) {
                try {
                    Materials material = Materials.fromString(materialKey);
                    if (!material.isAvailable() || material.getPostItem() == null) continue;

                    if (!containsMaterial(material)) {
                        addMaterial(material, configLoad.getLong("Materials." + materialKey + ".Points"));
                    }
                } catch (Exception e) {
                    Bukkit.getServer().getLogger().log(Level.WARNING, "SkyBlock | Error: The material '" + materialKey
                            + "' is not a Material type. Make sure the material name is a 1.14 material name. Please correct this in the 'levelling.yml' file.");
                }
            }
        }
    }

    public boolean isIslandLevelBeingScanned(Island island) {
        return this.islandsInQueue.stream().anyMatch(x -> x.getIsland() == island) || this.activelyScanningIsland == island;
    }

    public void unregisterMaterials() {
        materialStorage.clear();
    }

    public void addMaterial(Materials materials, long points) {
        materialStorage.add(new LevellingMaterial(materials, points));
    }

    public void removeMaterial(LevellingMaterial material) {
        materialStorage.remove(material);
    }

    public boolean containsMaterial(Materials materials) {
        for (LevellingMaterial materialList : materialStorage) {
            if (materialList.getMaterials().name().equals(materials.name())) {
                return true;
            }
        }

        return false;
    }

    public LevellingMaterial getMaterial(Materials materials) {
        for (LevellingMaterial materialList : materialStorage) {
            if (materialList.getMaterials().name().equals(materials.name())) {
                return materialList;
            }
        }

        return null;
    }

    public List<LevellingMaterial> getMaterials() {
        return materialStorage;
    }

    private static class LevellingData {
        private final static int version = NMSUtil.getVersionNumber();

        private final Material material;
        private final byte data;
        private final EntityType spawnerType;

        private LevellingData(Material material, byte data, EntityType spawnerType) {
            this.material = material;
            this.data = data;
            this.spawnerType = spawnerType;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LevellingData)) return false;
            LevellingData data = (LevellingData) obj;
            if (this == obj) return true;
            if (version > 12) {
                return this.material == data.material && this.spawnerType == data.spawnerType;
            } else {
                if (this.data == 0 && data.data == 0)
                    return this.material == data.material && this.spawnerType == data.spawnerType;

                Materials thisMaterials = Materials.requestMaterials(this.material.name(), this.data);
                Materials otherMaterials = Materials.requestMaterials(this.material.name(), (byte) 0);

                return thisMaterials == otherMaterials && this.spawnerType == data.spawnerType;
            }
        }

        @Override
        public int hashCode() {
            if (version > 12) {
                return Objects.hash(this.material, this.spawnerType);
            } else {
                if (this.data == 0)
                    return Objects.hash(this.material, true, this.spawnerType);

                Materials thisMaterials = Materials.requestMaterials(this.material.name(), this.data);
                Materials otherMaterials = Materials.requestMaterials(this.material.name(), (byte) 0);

                return Objects.hash(this.material, thisMaterials == otherMaterials, this.spawnerType);
            }
        }

        private Materials getMaterials() {
            if (this.spawnerType != null) {
                return Materials.getSpawner(this.spawnerType);
            }

            if (NMSUtil.getVersionNumber() > 12) {
                try {
                    return Materials.fromString(this.material.name());
                } catch (Exception ignored) {
                }
            }

            return Materials.getMaterials(this.material, this.data);
        }
    }

    private static class QueuedIsland {
        private final Player player;
        private final Island island;

        public QueuedIsland(Player player, Island island) {
            this.player = player;
            this.island = island;
        }

        public Player getPlayer() {
            return this.player;
        }

        public Island getIsland() {
            return this.island;
        }
    }
}