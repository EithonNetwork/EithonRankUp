package net.eithon.plugin.rankup;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.permissions.PermissionGroupLadder;
import net.eithon.library.permissions.PermissionMisc;
import net.eithon.library.plugin.Logger.DebugPrintLevel;
import net.eithon.plugin.stats.EithonStatsApi;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Controller {

	private EithonPlugin _eithonPlugin = null;
	private Plugin _statsPlugin;
	private PermissionGroupLadder _rankGroupLadder;

	public Controller(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		this._rankGroupLadder = new PermissionGroupLadder(eithonPlugin, false, Config.V.rankGroups);
		connectToStats(this._eithonPlugin);
	}

	private void connectToStats(EithonPlugin eithonPlugin) {
		this._statsPlugin = eithonPlugin.getServer().getPluginManager().getPlugin("EithonStats");
		if (this._statsPlugin != null && this._statsPlugin.isEnabled()) {
			eithonPlugin.getEithonLogger().info("Succesfully hooked into the EithonStats plugin!");
		} else {
			this._statsPlugin = null;
			eithonPlugin.getEithonLogger().warning("EithonRankUp doesn't work without the EithonStats plugin");			
		}
	}

	void disable() {
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
		int currentRank = expectedRank(player, playTimeHours);
		int currentRankGroup = this._rankGroupLadder.currentLevel(player);
		if (currentRank > currentRankGroup) {
			removeAndAddGroups(player, playTimeHours);
			Config.M.rankedUpToGroup.sendMessage(player, this._rankGroupLadder.getPermissionGroup(currentRank));
		}
		reportNextRank(player, playTimeHours);
	}

	private long reportPlayTime(Player player) {
		long playTimeHours = EithonStatsApi.getPlaytimeHours(player);
		Config.M.playTime.sendMessage(player, playTimeHours);
		return playTimeHours;
	}

	private void reportCurrentGroup(Player player) {
		int rankGroup = this._rankGroupLadder.currentLevel(player);
		if (rankGroup < 0) {
			player.sendMessage("You are not member of any groups");
			return;
		}
		player.sendMessage(String.format("You are currently in the rank group %s.", 
				this._rankGroupLadder.getPermissionGroup(rankGroup)));
	}

	private void removeAndAddGroups(Player player, long playTimeHours) {
		verbose("removeAndAddGroups", "Enter");
		int expectedRank = expectedRank(player, playTimeHours);
		this._rankGroupLadder.updatePermissionGroups(player, expectedRank);
		verbose("removeAndAddGroups", "Leave");
	}

	private void reportNextRank(Player player, long playTimeHours) {
		int nextRank = nextRank(player, playTimeHours);
		if (nextRank < 0) {
			Config.M.reachedHighestRank.sendMessage(player, Config.V.rankGroups[Config.V.rankGroups.length-1]);		
			return;			
		}
		String groupName = this._rankGroupLadder.getPermissionGroup(nextRank);

		Config.M.timeToNextRank.sendMessage(player, Config.V.afterHours[nextRank] - playTimeHours, groupName);
	}

	private int nextRank(Player player, long playTimeHours) {
		for (int i = 0; i < Config.V.afterHours.length; i++) {
			long rankHour = Config.V.afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i;
			}
		}
		return -1;
	}

	private int expectedRank(Player player, long playTimeHours) {
		for (int i = 0; i < Config.V.afterHours.length; i++) {
			int rankHour = Config.V.afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i-1;
			}
		}
		return Config.V.afterHours.length-1;
	}

	private void verbose(String method, String format, Object... args) {
		String message = String.format(format, args);
		this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.VERBOSE, "%s: %s", method, message);
	}

}