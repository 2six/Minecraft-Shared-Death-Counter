package me.sdc;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class GameState {

    public int lives;
    public int totalDeaths;
    public int earth;

    private static final int DEFAULT_LIVES = 3;

    public GameState() {
        lives = DEFAULT_LIVES;
        totalDeaths = 0;
        earth = 1;
    }

    public void onDeath() {
        lives--;
        totalDeaths++;
    }

    public boolean isGameOver() {
        return lives <= 0;
    }

    public void nextEarth() {
        earth++;
        lives = DEFAULT_LIVES;
    }

    public void save(File file) {
        try {
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("lives", lives);
            yml.set("totalDeaths", totalDeaths);
            yml.set("earth", earth);
            yml.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static GameState load(File file) {
        GameState state = new GameState();
        try {
            if (!file.exists()) return state;

            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            state.lives = yml.getInt("lives", state.lives);
            state.totalDeaths = yml.getInt("totalDeaths", 0);
            state.earth = yml.getInt("earth", 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return state;
    }
}
