package me.sdc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class DisplayTask extends BukkitRunnable {

    private final SharedDeathCounter plugin;

    public DisplayTask(SharedDeathCounter plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.state.lives <= 0) return;

        // config에서 표시 여부 가져오기
        boolean showLives = plugin.getConfig().getBoolean("display.lives", true);
        boolean showDeaths = plugin.getConfig().getBoolean("display.individual-deaths", true);
        boolean showEarth = plugin.getConfig().getBoolean("display.earth", true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            StringBuilder sb = new StringBuilder();

            if (showLives) {
                sb.append("§c❤ §f").append(plugin.state.lives);
            }

            // 개별 플레이어 사망 횟수 표시
            if (showDeaths) {
                if (sb.length() > 0) sb.append("  §7|  ");
                int myDeaths = plugin.deathStorage.getDeaths(p);
                sb.append("§8☠ §f").append(myDeaths);
            }

            if (showEarth) {
                if (sb.length() > 0) sb.append("  §7|  ");
                sb.append("§9🌍 §f#").append(plugin.state.earth);
            }

            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(sb.toString()));
        }
    }
}