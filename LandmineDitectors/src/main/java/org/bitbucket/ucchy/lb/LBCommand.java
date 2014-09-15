/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * LandmineBustersのコマンドクラス
 * @author ucchy
 */
public class LBCommand implements TabExecutor {

    private static final String PERMISSION = "LandmineDetectors.";

    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if ( args.length == 0 ) {
            return false;
        }

        if ( args[0].equalsIgnoreCase("start") ) {
            return doStart(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("cancel") ) {
            return doCancel(sender, command, label, args);
        }

        return false;
    }

    /**
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }

    /**
     * startコマンド
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    private boolean doStart(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションチェック
        if ( !sender.hasPermission(PERMISSION + "start") ) {
            sender.sendMessage(ChatColor.RED + "パーミッションが無いため実行できません。");
            return true;
        }

        // ゲーム外から実行された場合はエラー終了する
        if ( !(sender instanceof Player) ) {
            sender.sendMessage(ChatColor.RED + "このコマンドはゲーム内から実行してください。");
            return true;
        }

        Player player = (Player)sender;
        GameSessionManager manager = LandmineBusters.getInstance().getGameSessionManager();

        // 既にセッション中ならエラー終了する
        if ( manager.isPlayerInGame(player) ) {
            sender.sendMessage(ChatColor.RED + "あなたは既にゲームを開始しています。");
            return true;
        }

        // セッションの作成
        String difficulty = "normal";
        if ( args.length >= 2 && args[1].equalsIgnoreCase("easy") ) {
            difficulty = "easy";
        } else if ( args.length >= 2 && args[1].equalsIgnoreCase("hard") ) {
            difficulty = "hard";
        }
        LBConfig config = LandmineBusters.getInstance().getLDConfig();
        LBDifficultySetting setting = config.getDifficulty().get(difficulty);
        manager.makeNewSession(player, setting.getSize(), setting.getMine());

        return true;
    }

    /**
     * cancelコマンド
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    private boolean doCancel(CommandSender sender, Command command, String label, String[] args) {

        // ゲーム外から実行された場合はエラー終了する
        if ( !(sender instanceof Player) ) {
            sender.sendMessage(ChatColor.RED + "このコマンドはゲーム内から実行してください。");
            return true;
        }

        Player player = (Player)sender;
        GameSessionManager manager = LandmineBusters.getInstance().getGameSessionManager();

        // セッション中じゃないならエラー終了する
        if ( !manager.isPlayerInGame(player) ) {
            sender.sendMessage(ChatColor.RED + "あなたはゲーム中ではありません。");
            return true;
        }

        // 準備中ならエラー終了する
        if ( manager.isPlayerPrepare(player) ) {
            sender.sendMessage(ChatColor.RED + "あなたはゲーム開始待機中のため、キャンセルできません。");
            return true;
        }

        // セッションのキャンセル
        manager.getSession(player).runCancel();
        manager.removeSession(player);

        return true;
    }
}
