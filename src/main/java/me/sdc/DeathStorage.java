package me.sdc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class DeathStorage {

    private final File file;
    private final YamlConfiguration yml;

    public DeathStorage(File file) {
        this.file = file;
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    public void recordDeath(Player p) {
        UUID id = p.getUniqueId();
        String base = id.toString();

        int count = yml.getInt(base + ".deaths", 0) + 1;
        yml.set(base + ".name", p.getName());
        yml.set(base + ".deaths", count);
    }

    public int getDeaths(Player p) {
        return yml.getInt(p.getUniqueId().toString() + ".deaths", 0);
    }

    // 마지막으로 방문한 지구 번호 가져오기 (없으면 0)
    public int getLastVisitedEarth(Player p) {
        return yml.getInt(p.getUniqueId().toString() + ".lastEarth", 0);
    }

    // 방문한 지구 번호 업데이트
    public void setLastVisitedEarth(Player p, int earthNum) {
        yml.set(p.getUniqueId().toString() + ".lastEarth", earthNum);
    }

    public void save() {
        try {
            yml.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}