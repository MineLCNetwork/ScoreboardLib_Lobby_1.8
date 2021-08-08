package me.tigerhix.lib.scoreboard.type;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.minelc.CORE.Controller.Jugador;
import me.tigerhix.lib.scoreboard.ScoreboardLib;
import me.tigerhix.lib.scoreboard.common.Strings;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleScoreboard1_8 implements Scoreboard {

    private static final String TEAM_PREFIX = "S_";
    private static int TEAM_COUNTER = 0;

    private final org.bukkit.scoreboard.Scoreboard scoreboard;
    private final Objective objective;

    protected Player holder;
    protected long updateInterval = 10L;

    private boolean activated;
    private ScoreboardHandler handler;
    private Map<FakePlayer, Integer> entryCache = new ConcurrentHashMap<>();
    private Table<String, Integer, FakePlayer> playerCache = HashBasedTable.create();
    private Table<Team, String, String> teamCache = HashBasedTable.create();
    private BukkitRunnable updateTask;

    public SimpleScoreboard1_8(Player holder) {
        this.holder = holder;
        // Initiate the Bukkit scoreboard
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective("board", "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
        objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        Objective objHealth = scoreboard.getObjective("ShowHealth");
        if (objHealth == null) {
            objHealth = scoreboard.registerNewObjective("ShowHealth", "health");
            objHealth.setDisplaySlot(DisplaySlot.BELOW_NAME);
            objHealth.setDisplayName(ChatColor.DARK_RED + "❤");
        }
        // pruebas
        Team tm = scoreboard.registerNewTeam(holder.getName());
        Jugador j = Jugador.getJugador(holder);
        if (j.isHideRank()) {
            if(j.isOnlineMode()){
                tm.setPrefix(ChatColor.YELLOW.toString());
            } else {
                tm.setPrefix(ChatColor.GRAY.toString());
            }
        } else {
            tm.setPrefix(ChatColor.translateAlternateColorCodes('&', j.getRank().getTab_prefix()) + "" + j.getNameTagColor());
            if(!j.getRank().getTab_suffix().isEmpty())
                tm.setSuffix(" " + ChatColor.translateAlternateColorCodes('&', j.getRank().getTab_suffix()));


        }
        tm.addEntry(holder.getName());
    }

    @Override
    public void activate() {
        if (activated) return;
        if (handler == null) throw new IllegalArgumentException("Scoreboard handler not set");
        activated = true;
        // Set to the custom scoreboard
        holder.setScoreboard(scoreboard);
        // And start updating on a desired interval
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(ScoreboardLib.getPluginInstance(), 0, updateInterval);
    }

    @Override
    public void deactivate() {
        if (!activated) return;
        activated = false;
        // Set to the main scoreboard
        if (holder.isOnline()) {
            synchronized (this) {
                holder.setScoreboard((Bukkit.getScoreboardManager().getMainScoreboard()));
            }
        }
        // Unregister teams that are created for this scoreboard
        for (Team team : teamCache.rowKeySet()) {
            team.unregister();
        }
        // Stop updating
        updateTask.cancel();
    }

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public ScoreboardHandler getHandler() {
        return handler;
    }

    @Override
    public Scoreboard setHandler(ScoreboardHandler handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public long getUpdateInterval() {
        return updateInterval;
    }

    @Override
    public SimpleScoreboard1_8 setUpdateInterval(long updateInterval) {
        if (activated) throw new IllegalStateException("Scoreboard is already activated");
        this.updateInterval = updateInterval;
        return this;
    }

    @Override
    public Player getHolder() {
        return holder;
    }

    @Override
    public void updatePublic() {
        update();
    }

    @SuppressWarnings("deprecation")
    private void update() {
        if (!holder.isOnline()) {
            deactivate();
            return;
        }
        // Title
        String handlerTitle = handler.getTitle(holder);
        String finalTitle = Strings.format(handlerTitle != null ? handlerTitle : ChatColor.BOLD.toString());
        if (!objective.getDisplayName().equals(finalTitle)) objective.setDisplayName(Strings.format(finalTitle));
        // Entries
        List<Entry> passed = handler.getEntries(holder);
        Map<String, Integer> appeared = new HashMap<>();
        Map<FakePlayer, Integer> current = new HashMap<>();
        if (passed == null) return;
        for (Entry entry : passed) {
            // Handle the entry
            String key = entry.getName();
            Integer score = entry.getPosition();
            if (key.length() > 48) key = key.substring(0, 47);
            String appearance;
            if (key.length() > 16) {
                appearance = key.substring(16);
            } else {
                appearance = key;
            }
            if (!appeared.containsKey(appearance)) appeared.put(appearance, -1);
            appeared.put(appearance, appeared.get(appearance) + 1);
            // Get fake player
            FakePlayer faker = getFakePlayer(key, appeared.get(appearance));
            // Set score
            objective.getScore(faker).setScore(score);
            // Update references
            entryCache.put(faker, score);
            current.put(faker, score);
        }
        appeared.clear();
        for(Player tmOnline : Bukkit.getOnlinePlayers()){
            Jugador jugTM = Jugador.getJugador(tmOnline);

            try {
                Team tm = scoreboard.getTeam(jugTM.getBukkitPlayer().getName());

                if(tm != null){
                    // continue;
                    tm.unregister();
                }

                tm = scoreboard.registerNewTeam(jugTM.getBukkitPlayer().getName());

                if (jugTM.isHideRank()) {
                    if(jugTM.isOnlineMode()){
                        tm.setPrefix(ChatColor.YELLOW.toString());

                    } else {
                        tm.setPrefix(ChatColor.GRAY.toString());

                    }
                } else {
                    if(jugTM.getRank().getName().equalsIgnoreCase("DEFAULT")){
                        tm.setPrefix(ChatColor.GRAY.toString());
                    }else if(jugTM.getRank().getName().equalsIgnoreCase("PREMIUM")){
                        tm.setPrefix(ChatColor.YELLOW.toString());
                    }
                    else{
                        tm.setPrefix(ChatColor.translateAlternateColorCodes('&', jugTM.getRank().getTab_prefix()) +" "+ jugTM.getNameTagColor());
                        if(!jugTM.getRank().getTab_suffix().isEmpty())
                            tm.setSuffix(ChatColor.translateAlternateColorCodes('&', jugTM.getRank().getTab_suffix()));
                    }
                }
                tm.addPlayer(tmOnline);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        // Remove duplicated or non-existent entries
        for (FakePlayer fakePlayer : entryCache.keySet()) {
            if (!current.containsKey(fakePlayer)) {
                entryCache.remove(fakePlayer);
                scoreboard.resetScores(fakePlayer.getName());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private FakePlayer getFakePlayer(String text, int offset) {
        Team team = null;
        String name;
        // If the text has a length less than 16, teams need not to be be created
        if (text.length() <= 16) {
            name = text + Strings.repeat(" ", offset);
        } else {
            String prefix;
            String suffix = "";
            offset++;
            // Otherwise, iterate through the string and cut off prefix and suffix
            prefix = text.substring(0, 16 - offset);
            name = text.substring(16 - offset);
            if (name.length() > 16) name = name.substring(0, 16);
            if (text.length() > 32) suffix = text.substring(32 - offset);
            // If teams already exist, use them
            for (Team other : teamCache.rowKeySet()) {
                if (other.getPrefix().equals(prefix) && other.getSuffix().equals(suffix)) {
                    team = other;
                }
            }
            // Otherwise create them
            if (team == null) {
                team = scoreboard.registerNewTeam(TEAM_PREFIX + TEAM_COUNTER++);
                team.setPrefix(prefix);
                team.setSuffix(suffix);
                teamCache.put(team, prefix, suffix);
            }
        }
        FakePlayer faker;
        if (!playerCache.contains(name, offset)) {
            faker = new FakePlayer(name, team, offset);
            playerCache.put(name, offset, faker);
            if (faker.getTeam() != null) {
                faker.getTeam().addPlayer(faker);
            }
        } else {
            faker = playerCache.get(name, offset);
            if (team != null && faker.getTeam() != null) {
                faker.getTeam().removePlayer(faker);
            }
            faker.setTeam(team);
            if (faker.getTeam() != null) {
                faker.getTeam().addPlayer(faker);
            }
        }
        return faker;
    }

    public Objective getObjective() {
        return objective;
    }


    public org.bukkit.scoreboard.Scoreboard getScoreboard() {
        return scoreboard;
    }

    private static class FakePlayer implements OfflinePlayer {

        private final String name;

        private Team team;
        private int offset;

        FakePlayer(String name, Team team, int offset) {
            this.name = name;
            this.team = team;
            this.offset = offset;
        }

        public Team getTeam() {
            return team;
        }

        public void setTeam(Team team) {
            this.team = team;
        }

        public int getOffset() {
            return offset;
        }

        public String getFullName() {
            if (team == null) return name;
            if (team.getSuffix() == null) return team.getPrefix() + name;
            return team.getPrefix() + name + team.getSuffix();
        }

        @Override
        public boolean isOnline() {
            return true;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public UUID getUniqueId() {
            return UUID.randomUUID();
        }

        @Override
        public boolean isBanned() {
            return false;
        }


        public void setBanned(boolean b) {

        }


        @Override
        public boolean isWhitelisted() {
            return false;
        }

        @Override
        public void setWhitelisted(boolean whitelisted) {
        }

        @Override
        public Player getPlayer() {
            return null;
        }

        @Override
        public long getFirstPlayed() {
            return 0;
        }

        @Override
        public long getLastPlayed() {
            return 0;
        }

        @Override
        public boolean hasPlayedBefore() {
            return false;
        }

        @Override
        public Location getBedSpawnLocation() {
            return null;
        }

        public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {

        }

        public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {

        }

        public void incrementStatistic(Statistic statistic, int i) throws IllegalArgumentException {

        }

        public void decrementStatistic(Statistic statistic, int i) throws IllegalArgumentException {

        }

        public void setStatistic(Statistic statistic, int i) throws IllegalArgumentException {

        }

        public int getStatistic(Statistic statistic) throws IllegalArgumentException {
            return 0;
        }

        public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {

        }

        public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {

        }

        public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
            return 0;
        }

        public void incrementStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException {

        }

        public void decrementStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException {

        }

        public void setStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException {

        }

        public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {

        }

        public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {

        }

        public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
            return 0;
        }

        public void incrementStatistic(Statistic statistic, EntityType entityType, int i) throws IllegalArgumentException {

        }

        public void decrementStatistic(Statistic statistic, EntityType entityType, int i) {

        }

        public void setStatistic(Statistic statistic, EntityType entityType, int i) {

        }

        @Override
        public Map<String, Object> serialize() {
            return null;
        }

        @Override
        public boolean isOp() {
            return false;
        }

        @Override
        public void setOp(boolean op) {
        }

        @Override
        public String toString() {
            return "FakePlayer{" +
                    "name='" + name + '\'' +
                    ", team=" + team
                    + '}';
        }

    }

}