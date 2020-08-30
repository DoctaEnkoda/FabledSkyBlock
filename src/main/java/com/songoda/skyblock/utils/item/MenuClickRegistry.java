package com.songoda.skyblock.utils.item;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.utils.StringUtil;
import com.songoda.skyblock.utils.item.nInventoryUtil.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
 

public final class MenuClickRegistry {

    private static MenuClickRegistry instance;

    public static MenuClickRegistry getInstance() {
        return instance == null ? instance = new MenuClickRegistry() : instance;
    }

    private final Set<MenuPopulator> populators;
    private final Map<RegistryKey, MenuExecutor> executors;

    private MenuClickRegistry() {
        this.executors = new HashMap<>();
        this.populators = new HashSet<>();
    }

    public void register(MenuPopulator populator) {
        populator.populate(executors);
        populators.add(populator);
    }

    public void reloadAll() {
        executors.clear();
        for (MenuPopulator populator : populators) {
            populator.populate(executors);
        }
    }

    public void dispatch(Player clicker, ClickEvent e) {

        final ItemStack item = e.getItem();

        if (item == null) return;

        final ItemMeta meta = item.getItemMeta();

        if (meta == null) return;

        @SuppressWarnings("deprecation")
        final MenuExecutor executor = executors.get(RegistryKey.fromName(meta.getDisplayName(), CompatibleMaterial.getMaterial(item)));


        if (executor == null) return;

        executor.onClick(SkyBlock.getInstance(), clicker, e);
    }

    public interface MenuPopulator {

        void populate(Map<RegistryKey, MenuExecutor> executors);

    }

    public interface MenuExecutor {

        void onClick(SkyBlock plugin, Player clicker, ClickEvent e);

    }

    public static class RegistryKey {

        private static final File path = new File(SkyBlock.getInstance().getDataFolder(), "language.yml");

        private final String name;
        private final CompatibleMaterial type;

        private RegistryKey(String name, CompatibleMaterial type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RegistryKey)) return false;

            final RegistryKey other = (RegistryKey) obj;

            return Objects.equals(name, other.name) && type == other.type;
        }

        public static RegistryKey fromName(String name, CompatibleMaterial type) {
            return new RegistryKey(name, type);
        }

        public static RegistryKey fromLanguageFile(String namePath, CompatibleMaterial type) {
            return new RegistryKey(StringUtil.color(SkyBlock.getInstance().getFileManager().getConfig(path).getFileConfiguration().getString(namePath)), type);
        }
    }

}
