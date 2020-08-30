package com.songoda.skyblock.listeners;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandManager;
import com.songoda.skyblock.island.IslandRole;
import com.songoda.skyblock.island.IslandWorld;
import com.songoda.skyblock.permission.PermissionManager;
import com.songoda.skyblock.upgrade.Upgrade;
import com.songoda.skyblock.utils.version.NMSUtil;
import com.songoda.skyblock.utils.world.LocationUtil;
import com.songoda.skyblock.world.WorldManager;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.material.Crops;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation")
public class Grow implements Listener {

    private final SkyBlock plugin;

    public Grow(SkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks that a structure like a tree is not growing outside or into another
     * island.
     * 
     * @author LimeGlass
     */
    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        WorldManager worldManager = plugin.getWorldManager();
        if (!worldManager.isIslandWorld(event.getWorld())) return;

        IslandManager islandManager = plugin.getIslandManager();
        Island origin = islandManager.getIslandAtLocation(event.getLocation());
        for (Iterator<BlockState> it = event.getBlocks().iterator(); it.hasNext();) {
            BlockState state = it.next();
            Island growingTo = islandManager.getIslandAtLocation(state.getLocation());
            // This block is ok to continue as it's not related to Skyblock islands.
            if (origin == null && growingTo == null) continue;

            //Is in border of island
            if(origin != null && !origin.isInBorder(state.getLocation())) {
                it.remove();
                continue;
            }

            // A block from the structure is outside/inside that it's not suppose to.
            if (origin == null || growingTo == null) {
                it.remove();
                continue;
            }
            // The structure is growing from one island to another.
            if (!origin.getIslandUUID().equals(growingTo.getIslandUUID())) {
                it.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCropUpgrade(BlockGrowEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        WorldManager worldManager = plugin.getWorldManager();
        if (!plugin.getWorldManager().isIslandWorld(block.getWorld())) return;

        IslandManager islandManager = plugin.getIslandManager();
        Island island = islandManager.getIslandAtLocation(block.getLocation());
        if (island == null) return;

        // Check spawn block protection
        IslandWorld world = worldManager.getIslandWorld(block.getWorld());
        if (LocationUtil.isLocationAffectingIslandSpawn(block.getLocation(), island, world)) {
            if (plugin.getFileManager().getConfig(new File(plugin.getDataFolder(), "config.yml")).getFileConfiguration().getBoolean("Island.Spawn.Protection")) {
                event.setCancelled(true);
                return;
            }
        }

        List<Upgrade> upgrades = plugin.getUpgradeManager().getUpgrades(Upgrade.Type.Crop);
        if (upgrades == null || upgrades.size() == 0 || !upgrades.get(0).isEnabled() || !island.isUpgrade(Upgrade.Type.Crop)) return;

        if (NMSUtil.getVersionNumber() > 12) {
            try {
                Object blockData = block.getClass().getMethod("getBlockData").invoke(block);
                if (blockData instanceof org.bukkit.block.data.Ageable) {
                    org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) blockData;
                    ageable.setAge(ageable.getAge() + 1);
                    block.getClass().getMethod("setBlockData", Class.forName("org.bukkit.block.data.BlockData")).invoke(block, ageable);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            CompatibleMaterial type = CompatibleMaterial.getBlockMaterial(block.getType());
            if (block.getState().getData() instanceof Crops || type.name().equals("BEETROOT_BLOCK") || type.name().equals("CARROT") || type.name().equals("POTATO")
                    || type.name().equals("WHEAT") || type.name().equals("CROPS")) {
                try {
                    block.getClass().getMethod("setData", byte.class).invoke(block, (byte) (block.getData() + 1));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Checks that a block like a pumpkins and melons are not growing outside or
     * into another island.
     * 
     * @author LimeGlass
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        WorldManager worldManager = plugin.getWorldManager();
        BlockState state = event.getNewState();
        if (!worldManager.isIslandWorld(state.getWorld())) return;
        if (CompatibleMaterial.getBlockMaterial(state.getType()) != CompatibleMaterial.PUMPKIN && CompatibleMaterial.getBlockMaterial(state.getType()) != CompatibleMaterial.MELON)  return;

        IslandManager islandManager = plugin.getIslandManager();
        Island origin = islandManager.getIslandAtLocation(event.getBlock().getLocation());
        Island growingTo = islandManager.getIslandAtLocation(state.getLocation());
        // This block is ok to continue as it's not related to Skyblock islands.
        if (origin == null && growingTo == null) return;
        // The growing block is outside/inside that it's not suppose to.
        if (origin == null || growingTo == null) {
            event.setCancelled(true);
            return;
        }
        // The block is growing from one island to another.
        if (!origin.getIslandUUID().equals(growingTo.getIslandUUID())) {
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Checks that a structure growing like a tree, does not impact spawn location
     * of the island.
     */
    @EventHandler(ignoreCancelled = true)
    public void onStructureCreate(StructureGrowEvent event) {
        if (!plugin.getFileManager().getConfig(new File(plugin.getDataFolder(), "config.yml")).getFileConfiguration().getBoolean("Island.Spawn.Protection")) return;

        List<BlockState> blocks = event.getBlocks();
        if (blocks.isEmpty()) return;

        WorldManager worldManager = plugin.getWorldManager();
        IslandManager islandManager = plugin.getIslandManager();
        Island island = islandManager.getIslandAtLocation(event.getLocation());
        if (island == null) return;

        // Check spawn block protection
        IslandWorld world = worldManager.getIslandWorld(blocks.get(0).getWorld());
        for (BlockState block : event.getBlocks()) {
            if (LocationUtil.isLocationAffectingIslandSpawn(block.getLocation(), island, world)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() != Material.FIRE) return;

        org.bukkit.block.Block block = event.getBlock();
        if (!plugin.getWorldManager().isIslandWorld(block.getWorld())) return;

        PermissionManager permissionManager = plugin.getPermissionManager();
        if (!permissionManager.hasPermission(block.getLocation(), "FireSpread", IslandRole.Owner)) event.setCancelled(true);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        if (!plugin.getWorldManager().isIslandWorld(block.getWorld())) return;

        PermissionManager permissionManager = plugin.getPermissionManager();
        if (!permissionManager.hasPermission(block.getLocation(), "LeafDecay", IslandRole.Owner)) event.setCancelled(true);
    }

}
