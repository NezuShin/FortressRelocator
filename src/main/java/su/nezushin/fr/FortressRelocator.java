package su.nezushin.fr;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class FortressRelocator extends JavaPlugin {
    private static volatile int yOffset;

    static int yOffset() {
        return yOffset;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        yOffset = getConfig().getInt("y-offset");
        FortressShift.setOffset(() -> yOffset);
        getServer().getPluginManager().registerEvents(new ResourceReloadListener(), this);
        FortressInstaller.install(getLogger());
    }

    private final class ResourceReloadListener implements Listener {
        @EventHandler
        public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
            FortressInstaller.install(getLogger());
        }
    }
}
