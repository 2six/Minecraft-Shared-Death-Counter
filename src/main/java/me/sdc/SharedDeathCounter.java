package me.sdc;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SharedDeathCounter extends JavaPlugin {

    public static SharedDeathCounter instance;

    public GameState state;
    public DeathStorage deathStorage;
    
    public boolean isResetting = false; 
    private boolean isHardResetting = false;

    private File stateFile;
    private final File crashLockFile = new File("reset_pending.lock");

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); 

        // [1] 데이터 폴더 확인
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // [2] 크래시 감지
        if (crashLockFile.exists()) {
            getLogger().warning("비정상 종료 감지됨. 강제 리셋을 수행합니다.");
            triggerReset(); 
            return;
        }

        // [3] 데이터 로드
        stateFile = new File(getDataFolder(), "state.yml");
        state = GameState.load(stateFile);
        deathStorage = new DeathStorage(new File(getDataFolder(), "death.yml"));

        // [4] 리스너 및 명령어 등록
        Bukkit.getPluginManager().registerEvents(new SDCListener(this), this);
        if (getCommand("sdc") != null) {
            getCommand("sdc").setExecutor(new SDCCommand(this));
        }

        // [5] 게임룰 적용 (1틱 후)
        Bukkit.getScheduler().runTaskLater(this, this::applyGameRules, 1L);

        // [6] HUD 태스크 시작
        new DisplayTask(this).runTaskTimer(this, 0L, 20L);

        getLogger().info("SharedDeathCounter enabled - Earth #" + state.earth);
    }

    @Override
    public void onDisable() {
        if (!isHardResetting) {
            if (state != null) state.save(stateFile);
            if (deathStorage != null) deathStorage.save();
        }
    }
    
    public void applyGameRules() {
        boolean regen = getConfig().getBoolean("gamerules.naturalRegeneration", false);
        boolean showMsg = getConfig().getBoolean("gamerules.showDeathMessages", true);
        
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.NATURAL_REGENERATION, regen);
            world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, showMsg);
        }
        getLogger().info("Gamerules applied to all worlds.");
    }

    public void triggerReset() {
        try {
            new File("reset.flag").createNewFile();
            if (crashLockFile.exists()) crashLockFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

        state.nextEarth();
        state.save(stateFile);
        deathStorage.save();

        Bukkit.shutdown();
    }
    
    public void performHardReset() {
        this.isHardResetting = true;

        try {
            new File("hard_reset.flag").createNewFile();
            
            File deathF = new File(getDataFolder(), "death.yml");
            if (stateFile.exists()) stateFile.delete();
            if (deathF.exists()) deathF.delete();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Bukkit.shutdown();
    }
    
    public void createCrashLock() {
        try {
            crashLockFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}