package com.songoda.skyblock.permission;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.utils.TextUtils;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.config.FileManager;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandRole;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class BasicPermission {

    private final String name;
    private final CompatibleMaterial icon;
    private final PermissionType type;

    protected BasicPermission(@Nonnull String name, @Nonnull CompatibleMaterial icon, @Nonnull PermissionType type) {
        this.name = name;
        this.icon = icon;
        this.type = type;
    }

    public ItemStack getItem(Island island, IslandRole role) {
        return getItem(island.hasPermission(role, this), role);
    }

    public ItemStack getItem(boolean permissionEnabled, IslandRole role) {
        ItemStack is = icon.getItem();
        FileConfiguration configLoad = SkyBlock.getInstance().getLanguage();

        List<String> itemLore = new ArrayList<>();

        ItemMeta im = is.getItemMeta();

        String roleName = role.name();

        if (role == IslandRole.Visitor
                || role == IslandRole.Member
                || role == IslandRole.Coop)
            roleName = "Default";

        String nameFinal = TextUtils.formatText(configLoad.getString("Menu.Settings." + roleName + ".Item.Setting." + name + ".Displayname", name));

        if(im != null){
            im.setDisplayName(nameFinal);
            for (String itemLoreList : configLoad
                    .getStringList("Menu.Settings." + roleName + ".Item.Setting.Status."
                            + (permissionEnabled ? "Enabled" : "Disabled") + ".Lore"))
                itemLore.add(TextUtils.formatText(itemLoreList));

            im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            im.setLore(itemLore);
            is.setItemMeta(im);
        }

        return is;
    }

    public String getName() {
        return name;
    }

    public CompatibleMaterial getIcon() {
        return icon;
    }

    public PermissionType getType() {
        return type;
    }
}
