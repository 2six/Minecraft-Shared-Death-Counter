package me.sdc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SDCCommand implements CommandExecutor {

    private final SharedDeathCounter plugin;
    private final Map<CommandSender, Long> confirmMap = new HashMap<>();

    public SDCCommand(SharedDeathCounter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // 1. /sdc deaths 명령어 처리 (권한 체크 및 설정 확인)
        if (args.length >= 1 && args[0].equalsIgnoreCase("deaths")) {
            if (!plugin.getConfig().getBoolean("settings.allow-deaths-command", true)) {
                sender.sendMessage("§c이 명령어는 비활성화되어 있습니다.");
                return true;
            }
            handleDeathsCommand(sender, args);
            return true;
        }

        // 2. 관리자 명령어 처리 (OP 전용)
        if (!sender.isOp()) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /sdc <status|reset|hardreset|setlife|addlife|deaths>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                sender.sendMessage("§6=== SDC Status ===");
                sender.sendMessage("§eLives: §f" + plugin.state.lives);
                sender.sendMessage("§eEarth: §f#" + plugin.state.earth);
                sender.sendMessage("§eResetting: §f" + plugin.isResetting);
                break;

            case "reset":
                sender.sendMessage("§4[Warning] 강제 리셋을 시작합니다.");
                plugin.triggerReset();
                break;

            case "setlife":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /sdc setlife <amount>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount < 0) {
                        sender.sendMessage("§c0 이상의 숫자만 입력 가능합니다.");
                        return true;
                    }
                    plugin.state.lives = amount;
                    sender.sendMessage("§a목숨이 " + amount + "개로 설정되었습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자만 입력하세요.");
                }
                break;

            case "addlife":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /sdc addlife <amount>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    plugin.state.lives += amount;
                    sender.sendMessage("§a목숨 " + amount + "개 추가됨. 현재: " + plugin.state.lives);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자만 입력하세요.");
                }
                break;

            case "hardreset":
                if (confirmMap.containsKey(sender) && (System.currentTimeMillis() - confirmMap.get(sender) < 10000)) {
                    sender.sendMessage("§4§l[초기화] §c하드 리셋을 수행합니다...");
                    plugin.performHardReset();
                } else {
                    sender.sendMessage("§4§l[경고!] §c모든 데이터가 영구 삭제됩니다.");
                    sender.sendMessage("§c실행하려면 10초 내에 §7/sdc hardreset§c 을 다시 입력하세요.");
                    confirmMap.put(sender, System.currentTimeMillis());
                }
                break;
        }
        return true;
    }

    private void handleDeathsCommand(CommandSender sender, String[] args) {
        // 1. 인수가 없을 때 (/sdc deaths) -> 접속 중인 모든 플레이어 목록 표시
        if (args.length == 1) {
            sender.sendMessage("§6§l[ ☠ 접속자 사망 현황 ]");
            boolean found = false;
            for (Player onlineP : Bukkit.getOnlinePlayers()) {
                int d = plugin.deathStorage.getDeaths(onlineP);
                sender.sendMessage(" §7- §f" + onlineP.getName() + ": §c" + d + "회");
                found = true;
            }
            if (!found) {
                sender.sendMessage(" §7(접속 중인 플레이어가 없습니다)");
            }
            sender.sendMessage("§6§m-----------------------------");
            return;
        }

        // 2. 특정 플레이어 조회 (/sdc deaths <닉네임>)
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target != null) {
            int deaths = plugin.deathStorage.getDeaths(target);
            sender.sendMessage("§e[기록] §f" + target.getName() + "님의 누적 사망 횟수: §c" + deaths + "회");
        } else {
            sender.sendMessage("§c해당 플레이어를 찾을 수 없거나 오프라인입니다.");
        }
    }
}