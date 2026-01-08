package me.sdc;

import org.bukkit.*;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SDCListener implements Listener {

    private final SharedDeathCounter plugin;

    public SDCListener(SharedDeathCounter plugin) {
        this.plugin = plugin;
    }

    //MOTD 변경
    @EventHandler
    public void onServerListPing(ServerListPingEvent e) {
        String motd = plugin.getConfig().getString("server-settings.motd", "Shared Death Counter");
        
        motd = motd.replace("{earth}", String.valueOf(plugin.state.earth))
                   .replace("{lives}", String.valueOf(plugin.state.lives));
        
        e.setMotd(motd);
    }

    // 새 지구 진입 시 상태 초기화
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        
        // 상태 초기화
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        p.setGameMode(GameMode.SURVIVAL);

        // 환영 메시지
        int currentEarth = plugin.state.earth;
        int lastEarth = plugin.deathStorage.getLastVisitedEarth(p);

        if (!plugin.state.isGameOver() && lastEarth < currentEarth) {
            String title = plugin.getConfig().getString("messages.new-earth-title", "NEW EARTH");
            String subtitle = plugin.getConfig().getString("messages.new-earth-subtitle", "Welcome to {earth} Earth")
                    .replace("{earth}", String.valueOf(currentEarth));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.sendTitle(title, subtitle, 10, 70, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1f);
                plugin.deathStorage.setLastVisitedEarth(p, currentEarth);
                plugin.deathStorage.save();
            }, 20L);
        } else if (plugin.state.isGameOver()) {
            applyGameOverEffects(p);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (plugin.isResetting) return;

        Player victim = e.getEntity();
        plugin.deathStorage.recordDeath(victim);
        plugin.state.onDeath();

        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10f, 1f)
        );

        if (plugin.state.isGameOver()) {
            plugin.isResetting = true;
            plugin.createCrashLock();
            new GameOverTask(plugin).start();
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (plugin.isResetting || plugin.state.isGameOver()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> applyGameOverEffects(e.getPlayer()), 5L);
        }
    }

    // 엔더드래곤 처치 시 통계 표시
    @EventHandler
    public void onDragonKill(EntityDeathEvent e) {
        if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            EnderDragon dragon = (EnderDragon) e.getEntity();
            Player killer = dragon.getKiller();

            // 연출
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                p.sendTitle("§d§lVICTORY!", "§f엔더 드래곤을 토벌했습니다!", 10, 100, 20);
            });

            // 6초 뒤 통계 출력
            Bukkit.getScheduler().runTaskLater(plugin, () -> announceStatistics(killer), 120L);
        }
    }

    private void announceStatistics(Player dragonSlayer) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;

        Bukkit.broadcastMessage("§d§m======================================");
        Bukkit.broadcastMessage("§f         §l[ §d전체 통계 §f§l]");

        Bukkit.broadcastMessage("§9 🌍 §e현재 지구: §f#" + plugin.state.earth + "§8 ☠ §e전체 누적 사망: §f" + plugin.state.totalDeaths + "회");

        // 1. 드래곤 슬레이어
        if (dragonSlayer != null) printStat("🐲 드래곤 슬레이어", dragonSlayer.getName(), "막타");

        // 2. 학살자 (몬스터 처치 수)
        Player slayer = getMVP(Statistic.MOB_KILLS);
        if (slayer != null) printStat("⚔ 학살자", slayer.getName(), slayer.getStatistic(Statistic.MOB_KILLS) + "마리 처치");

        // 3. 딜링 머신 (/10 하여 하트 단위로 표시)
        Player dealer = getMVP(Statistic.DAMAGE_DEALT);
        if (dealer != null) printStat("💥 딜링 머신", dealer.getName(), (dealer.getStatistic(Statistic.DAMAGE_DEALT) / 10) + "하트 피해 입힘");

        Player shielder = getMVP(Statistic.DAMAGE_BLOCKED_BY_SHIELD);
        if (shielder != null) printStat("🛡 방패 용사", shielder.getName(), (shielder.getStatistic(Statistic.DAMAGE_BLOCKED_BY_SHIELD) / 10) + "하트 방어");

        // 4. 불사신 (최소 사망)
        Player immortal = players.stream()
                .min(Comparator.comparingInt(p -> plugin.deathStorage.getDeaths(p)))
                .orElse(null);
        if (immortal != null) printStat("✨ 불사신", immortal.getName(), plugin.deathStorage.getDeaths(immortal) + "회 사망");

        // 5. 개복치 (최다 사망)
        Player sunfish = players.stream()
                .max(Comparator.comparingInt(p -> plugin.deathStorage.getDeaths(p)))
                .orElse(null);
        if (sunfish != null) printStat("🐟 개복치", sunfish.getName(), plugin.deathStorage.getDeaths(sunfish) + "회 사망");

        // 6. 점프킹
        Player jumper = getMVP(Statistic.JUMP);
        if (jumper != null) printStat("🐇 점프킹", jumper.getName(), jumper.getStatistic(Statistic.JUMP) + "회 점프");

        // 7. 마라토너
        Player runner = players.stream()
                .max(Comparator.comparingInt(p -> (p.getStatistic(Statistic.WALK_ONE_CM) + p.getStatistic(Statistic.SPRINT_ONE_CM))))
                .orElse(null);
        if (runner != null) {
            long dist = (runner.getStatistic(Statistic.WALK_ONE_CM) + runner.getStatistic(Statistic.SPRINT_ONE_CM)) / 100;
            printStat("🏃 마라토너", runner.getName(), dist + "m 이동");
        }

        // 8. 두더지 (모든 블럭 채굴 합산)
        Player mole = players.stream()
                .max(Comparator.comparingInt(this::getAllBlocksMined))
                .orElse(null);
        if (mole != null) printStat("⛏ 두더지", mole.getName(), getAllBlocksMined(mole) + "블럭 채굴");

        // 9. 광부 (주요 광석 합산)
        Player miner = players.stream()
                .max(Comparator.comparingInt(this::getOresMined))
                .orElse(null);
        if (miner != null) {
            String detail = String.format("다이아%d 금%d 철%d 석탄%d",
                    getOreCount(miner, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
                    getOreCount(miner, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE),
                    getOreCount(miner, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
                    getOreCount(miner, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE));
            printStat("💎 광부", miner.getName(), detail);
        }

        // 10. 먹보 (모든 음식 섭취 합산)
        Player glutton = players.stream()
                .max(Comparator.comparingInt(this::getAllFoodEaten))
                .orElse(null);
        if (glutton != null) printStat("🍖 먹보", glutton.getName(), getAllFoodEaten(glutton) + "개 섭취");

        // [개별 통계]
        for (Player p : players) {
            p.sendMessage("§b --------- [ 📝 개인 모험 기록 ] ---------");
            p.sendMessage("§7 ☠ 누적 사망 수 : §f" + plugin.deathStorage.getDeaths(p) + "회");
            p.sendMessage("§7 ⚔ 몬스터 처치 수 : §f" + p.getStatistic(Statistic.MOB_KILLS) + "마리");
            
            String killedMost = getTopHostileEntityStat(p, Statistic.KILL_ENTITY);
            String killedByMost = getTopHostileEntityStat(p, Statistic.ENTITY_KILLED_BY);

            p.sendMessage("§7 🗡 내가 가장 많이 죽인 몹 : §f" + killedMost);
            p.sendMessage("§7 🤕 나를 가장 많이 죽인 몹 : §f" + killedByMost);
            
            long timePlayed = p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
            p.sendMessage("§7 ⏱ 플레이 시간 : §f" + timePlayed + "분");
        }
        Bukkit.broadcastMessage("§d§m======================================");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§e[!] §f자신의 상세 기록을 보려면 통계 메뉴(ESC -> 통계)를 확인하세요.");
            Bukkit.broadcastMessage("§e[!] §f다른 플레이어의 사망 횟수 확인: §7/sdc deaths <닉네임>");
        }, 200L);
    }

    // --- 헬퍼 메소드 ---
    private void printStat(String title, String name, String value) {
        Bukkit.broadcastMessage("§e " + title + ": §f" + name + " §7(" + value + ")");
    }

    private Player getMVP(Statistic stat) {
        return Bukkit.getOnlinePlayers().stream()
                .max(Comparator.comparingInt(p -> p.getStatistic(stat)))
                .orElse(null);
    }

    // 모든 블럭 채굴량 합산
    private int getAllBlocksMined(Player p) {
        int sum = 0;
        for (Material m : Material.values()) {
            if (m.isBlock() && !m.isAir()) {
                sum += p.getStatistic(Statistic.MINE_BLOCK, m);
            }
        }
        return sum;
    }

    // 모든 음식 섭취량 합산
    private int getAllFoodEaten(Player p) {
        int sum = 0;
        for (Material m : Material.values()) {
            if (m.isEdible()) {
                sum += p.getStatistic(Statistic.USE_ITEM, m);
            }
        }
        return sum;
    }

    private int getOresMined(Player p) {
        return getOreCount(p, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE) +
               getOreCount(p, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE) +
               getOreCount(p, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE) +
               getOreCount(p, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE);
    }

    private int getOreCount(Player p, Material... mats) {
        int sum = 0;
        for (Material m : mats) sum += p.getStatistic(Statistic.MINE_BLOCK, m);
        return sum;
    }

    // 적대적 몹만 필터링하여 통계 산출
    private String getTopHostileEntityStat(Player p, Statistic statType) {
        EntityType topEntity = null;
        int max = 0;

        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && type.getEntityClass() != null && Monster.class.isAssignableFrom(type.getEntityClass())) {
                try {
                    int val = p.getStatistic(statType, type);
                    if (val > max) {
                        max = val;
                        topEntity = type;
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (topEntity == null || max == 0) return "없음";
        return topEntity.name() + " (" + max + "회)";
    }

    private void applyGameOverEffects(Player p) {
        //p.setGameMode(GameMode.SPECTATOR);        
        p.setGameMode(GameMode.ADVENTURE); // 블럭 파괴 방지
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 255));
        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 99999, 255));
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 99999, 255));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 99999, 255));
    }
}