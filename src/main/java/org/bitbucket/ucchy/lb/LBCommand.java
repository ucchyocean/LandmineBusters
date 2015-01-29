/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.lb.game.GameSessionManager;
import org.bitbucket.ucchy.lb.ranking.RankingDataManager;
import org.bitbucket.ucchy.lb.ranking.RankingScoreData;
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

    private static final String PERMISSION = "LandmineBusters.";

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
        } else if ( args[0].equalsIgnoreCase("rank") ) {
            return doRank(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("reload") ) {
            return doReload(sender, command, label, args);
        }

        return false;
    }

    /**
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // 1番目の引数の補完
        if ( args.length == 1 ) {
            String pre = args[0].toLowerCase();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( String com : new String[]{"start", "cancel", "rank"} ) {
                if ( com.startsWith(pre) ) {
                    candidates.add(com);
                }
            }
            return candidates;
        }

        // 2番目の引数の補完
        if ( args.length == 2 ) {
            String pre = args[1].toLowerCase();
            ArrayList<String> candidates = new ArrayList<String>();

            if ( args[0].equalsIgnoreCase("start") ||
                    args[0].equalsIgnoreCase("rank") ) {
                for ( Difficulty dif : Difficulty.values() ) {
                    if ( dif.getName().startsWith(pre) ) {
                        candidates.add(dif.getName());
                    }
                }
            }
            return candidates;
        }

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
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // ゲーム外から実行された場合はエラー終了する
        if ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("ErrorInGameCommand"));
            return true;
        }

        Player player = (Player)sender;
        GameSessionManager manager = LandmineBusters.getInstance().getGameSessionManager();

        // 既にセッション中ならエラー終了する
        if ( manager.isPlayerInGame(player) ) {
            sender.sendMessage(Messages.get("ErrorAlreadyStartGame"));
            return true;
        }

        // セッションの作成
        Difficulty difficulty = Difficulty.NORMAL;
        if ( args.length >= 2 ) {
            difficulty = Difficulty.getFromString(args[1], Difficulty.NORMAL);
        }
        LBConfig config = LandmineBusters.getInstance().getLBConfig();
        LBDifficultySetting setting = config.getDifficulty().get(difficulty);
        manager.makeNewSession(player, setting.getSize(), setting.getMine(), difficulty);

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
            sender.sendMessage(Messages.get("ErrorInGameCommand"));
            return true;
        }

        Player player = (Player)sender;
        GameSessionManager manager = LandmineBusters.getInstance().getGameSessionManager();

        // セッション中じゃないならエラー終了する
        if ( !manager.isPlayerInGame(player) ) {
            sender.sendMessage(Messages.get("ErrorNotInGame"));
            return true;
        }

        // 準備中ならエラー終了する
        if ( manager.isPlayerPrepare(player) ) {
            sender.sendMessage(Messages.get("ErrorNowPreparing"));
            return true;
        }

        // セッションのキャンセル
        manager.getSession(player).runCancel();
        manager.removeSession(player);

        return true;
    }


    /**
     * rankコマンド
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    private boolean doRank(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションチェック
        if ( !sender.hasPermission(PERMISSION + "rank") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // 表示対象
        Difficulty difficulty = null;
        if ( args.length >= 2 ) {
            difficulty = Difficulty.getFromString(args[1], null);
        }

        // 表示個数
        int max = 10;
        if ( args.length >= 3 && isDigit(args[2]) ) {
            max = Integer.parseInt(args[2]);
            if ( max == 0 ) {
                max = 1;
            }
        }

        // ランキングデータ表示
        RankingDataManager manager =
                LandmineBusters.getInstance().getRankingManager();

        for ( Difficulty dif : Difficulty.values() ) {

            if ( difficulty == null || difficulty == dif ) {

                ArrayList<RankingScoreData> ranking =
                        manager.getData(dif).getRanking();
                sender.sendMessage(ChatColor.LIGHT_PURPLE +
                        "===== " + dif.getName() + " ranking ====");

                for ( int index = 0; index < max ; index ++ ) {

                    if ( ranking.size() <= index ) continue;

                    RankingScoreData score = ranking.get(index);
                    String message = String.format(
                            ChatColor.RED + "%d. " + ChatColor.GOLD + "%s " +
                                    ChatColor.WHITE + "- " + ChatColor.GOLD + "%dP",
                            (index + 1), score.getName(), score.getScore() );
                    sender.sendMessage(message);
                }
            }
        }

        return true;
    }

    /**
     * reloadコマンド
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    private boolean doReload(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションチェック
        if ( !sender.hasPermission(PERMISSION + "reload") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        LandmineBusters.getInstance().getLBConfig().reloadConfig();
        sender.sendMessage(Messages.get("InformationReloaded"));
        return true;
    }

    /**
     * 文字列が整数値に変換可能かどうかを判定する
     * @param source 変換対象の文字列
     * @return 整数に変換可能かどうか
     */
    private static boolean isDigit(String source) {
        return source.matches("^[0-9]{1,9}$");
    }
}
