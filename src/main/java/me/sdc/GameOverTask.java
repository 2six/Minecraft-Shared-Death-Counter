package me.sdc;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class GameOverTask extends BukkitRunnable {

    private final SharedDeathCounter plugin;
    private int phase = 0; 
    private int countdown = 10;
    
    // Phase 0: 10초 카운트다운
    // Phase 1: 세계 붕괴 타이틀 후 5초 대기
    // Phase 2: 플레이어 킥 후 5초 대기 (서버 종료)

    public GameOverTask(SharedDeathCounter plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.state.lives = 0; // 목숨 0 고정
        World world = Bukkit.getWorlds().get(0);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

        Bukkit.getOnlinePlayers().forEach(this::applyEffects);
        
        this.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run() {
        
        // [PHASE 0] 10초 카운트다운
        if (phase == 0) {
            if (countdown > 0) {
                String color = getColor(countdown);
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.sendTitle("§4§lGAME OVER", color + "세계 붕괴 임박: " + countdown, 0, 25, 0);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
                });
                countdown--;
            } else {
                // 카운트다운 종료 -> Phase 1 진입
                phase = 1;
                countdown = 5; // 5초 대기 시간 설정

                Bukkit.getOnlinePlayers().forEach(p -> {
                    // 타이틀 표시
                    p.sendTitle("", "§4§l세계가 소멸했습니다", 0, 100, 20);
                    p.sendMessage("§4§l[SYSTEM] §c세계가 붕괴됩니다...");
                    
                    // 암전 효과
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1, true, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 1, true, false));
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
                });
            }
        }
        
        // [PHASE 1] 타이틀 후 5초 대기
        else if (phase == 1) {
            if (countdown > 0) {
                countdown--;
            } else {
                // 5초 대기 후 킥
                String kickFormat = plugin.getConfig().getString("messages.kick-reason", "World Collapsed");
                
                // {earth} -> 현재 숫자, {earth_next} -> 다음 숫자
                String kickMsg = kickFormat
                        .replace("{earth}", String.valueOf(plugin.state.earth))
                        .replace("{earth_next}", String.valueOf(plugin.state.earth + 1));
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.kickPlayer(kickMsg);
                }
                
                phase = 2;
                countdown = 5;
                Bukkit.getLogger().info("All players kicked. Shutting down in 5 seconds...");
            }
        }
        
        // [PHASE 2] 빈 서버 5초 대기 후 리셋
        else if (phase == 2) {
            if (countdown > 0) {
                countdown--;
            } else {
                // 최종 종료
                plugin.triggerReset();
                this.cancel();
            }
        }
    }

    private void applyEffects(Player p) {
        //p.setGameMode(GameMode.SPECTATOR);
        p.setGameMode(GameMode.ADVENTURE); // 블럭 파괴 방지용
        // 이동/공격 봉인 이펙트
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 255));
        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 99999, 255));
    }

    private String getColor(int t) {
        if (t > 5) return "§e§l";
        if (t > 2) return "§6§l";
        return "§4§l";
    }
}