package me.tigerhix.lib.scoreboard;

import me.tigerhix.lib.scoreboard.type.Scoreboard;
import me.tigerhix.lib.scoreboard.type.SimpleScoreboard;
import me.tigerhix.lib.scoreboard.type.SimpleScoreboard1_8;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ScoreboardLib extends JavaPlugin {

    private static Plugin instance;

    public static Plugin getPluginInstance() {
        return instance;
    }

    public static void setPluginInstance(Plugin instance) {
        if (ScoreboardLib.instance != null) return;
        ScoreboardLib.instance = instance;
    }

    public static Scoreboard createScoreboard(Player holder) {
        if(Bukkit.getServer().getVersion().contains("1.8")){
            return new SimpleScoreboard1_8(holder);
        } else {
            return new SimpleScoreboard(holder);
        }
    }

    @Override
    public void onEnable() {
        setPluginInstance(this);
    }

}