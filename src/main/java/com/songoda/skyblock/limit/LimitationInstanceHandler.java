package com.songoda.skyblock.limit;

import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.limit.impl.BlockLimitation;
import com.songoda.skyblock.limit.impl.EntityLimitation;
import org.bukkit.configuration.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class LimitationInstanceHandler {

    private final Map<Class<? extends Limitation>, Limitation> instances;
    
    // If true, load all chunks on player's island to count entities.
    // If false, do not load all chunks and only count entites on loaded chunks on player's island.
    private boolean loadChunks;
    
    
    public LimitationInstanceHandler() {
        this.instances = new HashMap<>();
        registerInstance(new EntityLimitation(this));
        registerInstance(new BlockLimitation());
        reloadAll();
    }
    
    public boolean isLoadChunks() {
        return loadChunks;
    }
    
    public <T extends Limitation> T getInstance(Class<T> type) {
        return type.cast(instances.get(type));
    }

    public void registerInstance(Limitation instance) {
        instances.put(instance.getClass(), instance);
    }

    public void reloadAll() {
        final SkyBlock plugin = SkyBlock.getInstance();
        final Configuration config = plugin.getLimits();
    
        loadChunks = plugin.getConfiguration().getBoolean("Island.Limits.LoadChunks");
    
        for (Limitation limit : instances.values()) {
            limit.reload(config.getConfigurationSection(limit.getSectionName()));
        }

    }

}
