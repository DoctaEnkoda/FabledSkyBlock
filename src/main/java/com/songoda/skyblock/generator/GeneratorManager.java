package com.songoda.skyblock.generator;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleSound;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.config.FileManager.Config;
import com.songoda.skyblock.island.IslandWorld;
import com.songoda.skyblock.utils.version.NMSUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneratorManager {

    private final SkyBlock plugin;
    private final List<Generator> generatorStorage = new ArrayList<>();

    public GeneratorManager(SkyBlock plugin) {
        this.plugin = plugin;
        registerGenerators();
    }

    public void registerGenerators() {
        FileConfiguration configLoad = this.plugin.getGenerators();

        if (configLoad.getString("Generators") == null)
            return;

        for (String generatorList : configLoad.getConfigurationSection("Generators").getKeys(false)) {
            if (configLoad.getString("Generators." + generatorList + ".Name") == null)
                continue;

            List<GeneratorMaterial> generatorMaterials = new ArrayList<>();
            if (configLoad.getString("Generators." + generatorList + ".Materials") != null) {
                for (String materialList : configLoad.getConfigurationSection("Generators." + generatorList + ".Materials").getKeys(false)) {
                    CompatibleMaterial materials = CompatibleMaterial.getMaterial(materialList);
                    if (materials != null) {
                        generatorMaterials.add(new GeneratorMaterial(materials, configLoad.getDouble(
                                "Generators." + generatorList + ".Materials." + materialList + ".Chance")));
                    }
                }
            }
            
            Random rnd = new Random();
            CompatibleMaterial icon;
            if(!generatorMaterials.isEmpty()) {
                icon = generatorMaterials.get(rnd.nextInt(generatorMaterials.size())).getMaterials();
            } else {
                icon = CompatibleMaterial.STONE;
            }

            generatorStorage.add(new Generator(configLoad.getString("Generators." + generatorList + ".Name"),
                    IslandWorld.valueOf(configLoad.getString("Generators." + generatorList + ".World", "Normal")),
                    icon, generatorMaterials,
                    configLoad.getLong("Generators." + generatorList + ".UnlockLevel", 0L),
                    configLoad.getBoolean("Generators." + generatorList + ".Permission")));
        }
    }

    public void unregisterGenerators() {
        generatorStorage.clear();
    }

    private boolean isFlowingTowardsBlock(Block from) {
        if (!from.isLiquid())
            return false;

        return isWater(from) && isFlowingBlock(from);
    }

    private boolean isLava(Block block) {
        return block.getType().name().contains("LAVA");
    }

    private boolean isWater(Block block) {
        return block.getType().name().contains("WATER");
    }

    public boolean isGenerator(Block block) {
        BlockFace[] blockFaces = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

        for (BlockFace blockFace1 : blockFaces) {
            for (BlockFace blockFace2 : blockFaces) {
                if (blockFace1.equals(blockFace2))
                    continue;

                Block from1 = block.getRelative(blockFace1);
                Block from2 = block.getRelative(blockFace2);
                if (isLava(from1) && isWater(from2) && isFlowingTowardsBlock(from2))
                    return true;
            }
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    private int getLiquidLevel(Block block) {
        if (NMSUtil.getVersionNumber() > 12 && block.getState().getBlockData() instanceof Levelled) {
            Levelled levelled = (Levelled) block.getState().getBlockData();
            return levelled.getLevel();
        } else {
            return block.getData();
        }
    }

    private boolean isFlowingBlock(Block block) {
        return getLiquidLevel(block) != 0;
    }

    @SuppressWarnings("deprecation")
    public BlockState generateBlock(Generator generator, Block block) {
        CompatibleMaterial materials = getRandomMaterials(generator);
        if (materials == null) return block.getState();
        
        plugin.getSoundManager().playSound(block.getLocation(), CompatibleSound.BLOCK_FIRE_EXTINGUISH.getSound(), 1.0F, 10.0F);


        if (NMSUtil.getVersionNumber() > 12) {
            block.setType(materials.getMaterial());
        } else {
            ItemStack is = materials.getItem();
            block.setType(is.getType());

            try {
                block.getClass().getMethod("setData", byte.class).invoke(block, (byte) is.getDurability());
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }
        }

        return block.getState();
    }

    public CompatibleMaterial getRandomMaterials(Generator generator) {
        if (generator.getGeneratorMaterials() != null && generator.getGeneratorMaterials().stream().anyMatch(x -> x.getChance() > 0)) {
            List<CompatibleMaterial> weightedList = new ArrayList<>();
            for (GeneratorMaterial generatorMaterial : generator.getGeneratorMaterials())
                for (int i = 0; i < generatorMaterial.getChance() * 30; i++)
                    weightedList.add(generatorMaterial.getMaterials());

            int choice = new Random().nextInt(weightedList.size());
            return weightedList.get(choice);
        }

        return CompatibleMaterial.COBBLESTONE;
    }

    public void addGenerator(String name, IslandWorld isWorld, List<GeneratorMaterial> generatorMaterials, int level, boolean permission) {
        CompatibleMaterial[] oreMaterials = new CompatibleMaterial[]{CompatibleMaterial.COAL, CompatibleMaterial.CHARCOAL, CompatibleMaterial.DIAMOND,
                CompatibleMaterial.IRON_INGOT, CompatibleMaterial.GOLD_INGOT, CompatibleMaterial.EMERALD};
        generatorStorage.add(new Generator(name, isWorld, oreMaterials[new Random().nextInt(oreMaterials.length)],
                generatorMaterials, level, permission));
    }

    public void removeGenerator(Generator generator) {
        generatorStorage.remove(generator);
    }

    public Generator getGenerator(String name) {
        for (Generator generatorList : generatorStorage) {
            if (generatorList.getName().equalsIgnoreCase(name)) {
                return generatorList;
            }
        }

        return null;
    }

    public boolean containsGenerator(String name) {
        for (Generator generatorList : generatorStorage) {
            if (generatorList.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

    public List<Generator> getGeneratorStorage() {
        return generatorStorage;
    }

    public List<Generator> getGenerators() {
        return generatorStorage;
    }
}
