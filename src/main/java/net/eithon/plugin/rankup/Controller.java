package net.eithon.plugin.rankup;

import net.eithon.library.core.PlayerCollection;
import net.eithon.library.extensions.EithonPlayer;
import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.permissions.PermissionGroupLadder;
import net.eithon.library.plugin.Logger.DebugPrintLevel;
import net.eithon.library.plugin.PluginMisc;
import net.eithon.library.time.AlarmTrigger;
import net.eithon.library.time.IRepeatable;
import net.eithon.plugin.stats.EithonStatsApi;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Controller {

	private EithonPlugin _eithonPlugin = null;
	private Plugin _statsPlugin;
	private PermissionGroupLadder _rankGroupLadder;
	private PlayerCollection<EithonPlayer> _knownPlayers;

	public Controller(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		this._rankGroupLadder = new PermissionGroupLadder(eithonPlugin, false, Config.V.rankGroups);
		connectToStats(this._eithonPlugin);
		repeatRemindToRankup();
	}

	private void connectToStats(EithonPlugin eithonPlugin) {
		this._statsPlugin = PluginMisc.getPlugin("EithonStats");
		if (this._statsPlugin != null && this._statsPlugin.isEnabled()) {
			eithonPlugin.getEithonLogger().info("Succesfully hooked into the EithonStats plugin!");
		} else {
			this._statsPlugin = null;
			eithonPlugin.getEithonLogger().warning("EithonRankUp doesn't work without the EithonStats plugin");			
		}
	}

	void disable() {
	}

	private void repeatRemindToRankup() {
		AlarmTrigger.get().repeat("EithonRankUp rank up reminders", Config.V.remindAfterMinutes*60, 
				new IRepeatable() {
			@Override
			public boolean repeat() {
				remindToRankUp();
				return true;
			}
		});
	}

	void remindToRankUp() {
		for (EithonPlayer eithonPlayer : this._knownPlayers.values()) {
			if (!eithonPlayer.isOnline()) continue;
			Player player = eithonPlayer.getPlayer();
			if (!EithonStatsApi.isActive(player)) continue;
			long playTimeHours = EithonStatsApi.getPlaytimeHours(player);
			int expectedRankStartAtOne = expectedRankStartAtOne(player, playTimeHours);
			int currentRankStartAtOne = this._rankGroupLadder.currentLevel(player);
			if (expectedRankStartAtOne > currentRankStartAtOne) {
				remindToRankUp(player, expectedRankStartAtOne);
			}
		}
	}

	private void remindToRankUp(Player player, int expectedRankStartAtOne) {
		String rankGroup = this._rankGroupLadder.getPermissionGroup(expectedRankStartAtOne);
		Config.M.rememberToRankUp.sendMessage(player, rankGroup);
	}

	public void rankup(Player player) {
		if (!this._rankGroupLadder.canUpdatePermissionGroups()) {
			player.sendMessage("EihonRankUp doesn't work without the zPermissions plugin");			
		} else {
			reportCurrentGroup(player);	
		}

		long playTimeHours = 0;
		if (this._statsPlugin == null) {
			player.sendMessage("EithonRankUp doesn't work without the EithonStats plugin");
		} else {
			playTimeHours = reportPlayTime(player);
		}
		int currentRankStartAtOne = expectedRankStartAtOne(player, playTimeHours);
		int currentRankGroupStartAtOne = this._rankGroupLadder.currentLevel(player);
		if (currentRankStartAtOne > currentRankGroupStartAtOne) {
			removeAndAddGroups(player, playTimeHours);
			Config.M.rankedUpToGroup.sendMessage(player, this._rankGroupLadder.getPermissionGroup(currentRankStartAtOne));
		}
		reportNextRank(player, playTimeHours);
	}

	public void playerJoined(Player player) {
		this._knownPlayers.put(player, new EithonPlayer(player));
	}

	public void playerQuitted(Player player) {
		this._knownPlayers.remove(player);
	}

	private long reportPlayTime(Player player) {
		long playTimeHours = EithonStatsApi.getPlaytimeHours(player);
		Config.M.playTime.sendMessage(player, playTimeHours);
		return playTimeHours;
	}

	private void reportCurrentGroup(Player player) {
		int rankGroupStartAtOne = this._rankGroupLadder.currentLevel(player);
		if (rankGroupStartAtOne < 1) {
			player.sendMessage("You are not member of any rank groups");
			return;
		}
		player.sendMessage(String.format("You are currently in the rank group %s.", 
				this._rankGroupLadder.getPermissionGroup(rankGroupStartAtOne)));
	}

	private void removeAndAddGroups(Player player, long playTimeHours) {
		verbose("removeAndAddGroups", "Enter");
		int expectedRankStartAtOne = expectedRankStartAtOne(player, playTimeHours);
		this._rankGroupLadder.updatePermissionGroups(player, expectedRankStartAtOne);
		verbose("removeAndAddGroups", "Leave");
	}

	private void reportNextRank(Player player, long playTimeHours) {
		int nextRankStartAtOne = nextRankStartAtOne(player, playTimeHours);
		if (nextRankStartAtOne < 1) {
			Config.M.reachedHighestRank.sendMessage(player, Config.V.rankGroups[Config.V.rankGroups.length-1]);		
			return;			
		}
		String groupName = this._rankGroupLadder.getPermissionGroup(nextRankStartAtOne);

		Config.M.timeToNextRank.sendMessage(player, Config.V.afterHours[nextRankStartAtOne-1] - playTimeHours, groupName);
	}

	private int nextRankStartAtOne(Player player, long playTimeHours) {
		for (int i = 0; i < Config.V.afterHours.length; i++) {
			long rankHour = Config.V.afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i+1;
			}
		}
		return 0;
	}

	private int expectedRankStartAtOne(Player player, long playTimeHours) {
		for (int i = 0; i < Config.V.afterHours.length; i++) {
			int rankHour = Config.V.afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i;
			}
		}
		return Config.V.afterHours.length;
	}

	private void verbose(String method, String format, Object... args) {
		String message = String.format(format, args);
		this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.VERBOSE, "%s: %s", method, message);
	}

}