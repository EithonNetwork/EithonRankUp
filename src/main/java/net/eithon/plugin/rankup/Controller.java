package net.eithon.plugin.rankup;

import java.util.HashSet;
import java.util.Set;

import net.eithon.plugin.stats.EithonStatsApi;
import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.Logger.DebugPrintLevel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

public class Controller {

	private EithonPlugin _eithonPlugin = null;
	private ZPermissionsService _permissionService = null;
	private Plugin _statsPlugin;

	public Controller(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		connectToPermissionService();
		connectToStats(this._eithonPlugin);
	}

	private void connectToStats(EithonPlugin eithonPlugin) {
		this._statsPlugin = eithonPlugin.getServer().getPluginManager().getPlugin("EithonStats");
		if (this._statsPlugin != null && this._statsPlugin.isEnabled()) {
			eithonPlugin.getEithonLogger().info("Succesfully hooked into the EithonStats plugin!");
		} else {
			this._statsPlugin = null;
			eithonPlugin.getEithonLogger().warning("RankUp doesn't work without the EithonStats plugin");			
		}
	}

	private void connectToPermissionService() {
		try {
			this._permissionService = Bukkit.getServicesManager().load(ZPermissionsService.class);
			// this.oracleService = Bukkit.getServicesManager().load(Oracle.class); 
		}
		catch (NoClassDefFoundError e) {
			// Eh...
		}
		if (this._permissionService == null) {
			this._eithonPlugin.getEithonLogger().warning("RankUp doesn't work without the zPermissions plugin");
		}
	}

	void disable() {
	}

	public void rankup(Player player) {
		if (this._permissionService == null) {
			player.sendMessage("RankUp doesn't work without the zPermissions plugin");			
		} else {
			reportCurrentGroup(player);			
		}

		long playTimeHours = 0;
		if (this._statsPlugin == null) {
			player.sendMessage("RankUp doesn't work without the EithonStats plugin");
		} else {
			playTimeHours = reportPlayTime(player);
		}
		int currentRank = expectedRank(player, playTimeHours);
		int currentRankGroup = firstRankGroupPlayerIsMemberOfNow(player);

		if (currentRank > currentRankGroup) {
			removeAndAddGroups(player, playTimeHours);
			Config.M.rankedUpToGroup.sendMessage(player, Config.V.rankGroups[currentRank]);
		}
		reportNextRank(player, playTimeHours);
	}

	private long reportPlayTime(Player player) {
		long playTimeHours = EithonStatsApi.getPlaytimeHours(player);
		Config.M.playTime.sendMessage(player, playTimeHours);
		return playTimeHours;
	}

	private void reportCurrentGroup(Player player) {
		int rankGroup = firstRankGroupPlayerIsMemberOfNow(player);
		if (rankGroup < 0) {
			player.sendMessage("You are not member of any groups");
			return;
		}
		player.sendMessage(String.format("You are currently in the rank group %s.", Config.V.rankGroups[rankGroup]));
	}

	private void removeAndAddGroups(Player player, long playTimeHours) {
		verbose("removeAndAddGroups", "Enter");
		int expectedRank = expectedRank(player, playTimeHours);
		verbose("removeAndAddGroups", "expectedRank = %d", expectedRank);
		Set<String> playerGroups = getPlayerGroupsInLowercase(player);
		verbose("removeAndAddGroups", "playerGroups: %s", String.join(", ", playerGroups));
		maybeAddGroup(player, expectedRank, playerGroups);
		removeGroups(player, expectedRank, playerGroups);
		verbose("removeAndAddGroups", "Leave");
	}

	private void maybeAddGroup(Player player, int expectedRank,
			Set<String> playerGroups) {
		verbose("maybeAddGroup", "Enter");
		String expectedRankGroup = Config.V.rankGroups[expectedRank].toLowerCase();
		verbose("maybeAddGroup", "expectedRankGroup: %s", expectedRankGroup);
		if (!playerGroups.contains(expectedRankGroup)) {
			verbose("maybeAddGroup", "Not found, so we will add the group %s for player %s", expectedRankGroup, player.getName());
			Config.C.addGroupCommand.execute(player.getName(), Config.V.rankGroups[expectedRank]);
		}
		verbose("maybeAddGroup", "Leave");
	}

	private void removeGroups(Player player, int expectedRank,
			Set<String> playerGroups) {
		verbose("removeGroups", "Enter");
		for (String playerGroup : playerGroups) {
			int i = getGroupRankNumber(playerGroup);	
			if (i < 0) {
				verbose("removeGroups", "Group %s is not a rank group, do not remove it.", playerGroup);
				continue;
			}
			if (i == expectedRank) {
				verbose("removeGroups", "Group %s is the expected rank group, do not remove it.", playerGroup);
				continue;
			}
			verbose("removeGroups", "Group %s should be removed for player %s", playerGroup, player.getName());
			Config.C.removeGroupCommand.execute(player.getName(), Config.V.rankGroups[i]);			
		}
		verbose("removeGroups", "Enter");
	}

	private int getGroupRankNumber(String groupName) {
		for (int i = 0; i < Config.V.rankGroups.length; i++) {
			if (Config.V.rankGroups[i].equalsIgnoreCase(groupName)) return i;
		}
		return -1;
	}

	private void reportNextRank(Player player, long playTimeHours) {
		int nextRank = nextRank(player, playTimeHours);
		if (nextRank < 0) {
			Config.M.reachedHighestRank.sendMessage(player, Config.V.rankGroups[Config.V.rankGroups.length-1]);		
			return;			
		}
		String groupName = Config.V.rankGroups[nextRank];

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

	private int firstRankGroupPlayerIsMemberOfNow(Player player) {
		verbose("firstRankGroupPlayerIsMemberOfNow", "Enter");
		if (this._permissionService == null) return -1;
		Set<String> currentGroups = getPlayerGroupsInLowercase(player);
		if ((currentGroups != null) && (currentGroups.size() > 0)) {
			verbose("firstRankGroupPlayerIsMemberOfNow", "Current groups: %s", String.join(", ", currentGroups));
			for (int i = 0; i < Config.V.rankGroups.length; i++) {
				String groupName = Config.V.rankGroups[i].toLowerCase();
				verbose("firstRankGroupPlayerIsMemberOfNow", "Check group: %s", groupName);
				if (currentGroups.contains(groupName)) {
					verbose("firstRankGroupPlayerIsMemberOfNow", "Matches %s", groupName);
					verbose("firstRankGroupPlayerIsMemberOfNow", "Leave %d", i);
					return i;		
				}
			}
		}
		verbose("firstRankGroupPlayerIsMemberOfNow", "No group found");
		verbose("firstRankGroupPlayerIsMemberOfNow", "Leave -1");
		return -1;
	}

	private Set<String> getPlayerGroupsInLowercase(Player player) {
		Set<String> currentGroups = this._permissionService.getPlayerGroups(player.getUniqueId());
		Set<String> lowerCaseGroups = new HashSet<String>();
		for (String groupName : currentGroups) {
			lowerCaseGroups.add(groupName.toLowerCase());
		}
		return lowerCaseGroups;
	}

	private void verbose(String method, String format, Object... args) {
		String message = String.format(format, args);
		this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.VERBOSE, "%s: %s", message);
	}

}