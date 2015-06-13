package net.eithon.plugin.rankup;

import java.util.Set;

import me.botsko.oracle.Oracle;
import net.eithon.library.extensions.EithonPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

public class Controller {

	private EithonPlugin _eithonPlugin = null;
	private ZPermissionsService _permissionService = null;
	private Plugin _oraclePlugin;

	public Controller(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		connectToPermissionService();
		connectToOracle(this._eithonPlugin);
	}

	private void connectToOracle(EithonPlugin eithonPlugin) {
		this._oraclePlugin = eithonPlugin.getServer().getPluginManager().getPlugin("Oracle");
		if (this._oraclePlugin != null && this._oraclePlugin.isEnabled()) {
			eithonPlugin.getEithonLogger().info("Succesfully hooked into the Oracle plugin!");
		} else {
			this._oraclePlugin = null;
			eithonPlugin.getEithonLogger().warning("RankUp doesn't work without the Oracle plugin");			
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

		int playTimeHours = 0;
		if (this._oraclePlugin == null) {
			player.sendMessage("RankUp doesn't work without the Oracle plugin");
		} else {
			playTimeHours = reportPlayTime(player);
		}
		int currentRank = currentRank(player, playTimeHours);
		int currentRankGroup = firstRankGroupPlayerIsMemberOfNow(player);

		if (currentRank > currentRankGroup) {
			removeAndAddGroups(player, playTimeHours);
			Config.M.rankedUpToGroup.sendMessage(player, Config.V.rankGroups[currentRank]);
		}
		reportNextRank(player, playTimeHours);
	}

	private int reportPlayTime(Player player) {
		Integer playTime = Oracle.playtimeHours.get(player);
		int playTimeHours = 0;
		if (playTime == null) {
			player.sendMessage(String.format("Could not find any playtime information for player %s.", player.getName()));
		} else {
			playTimeHours = playTime.intValue();
			Config.M.playTime.sendMessage(player, playTimeHours);
		}
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

	private void removeAndAddGroups(Player player, int playTimeHours) {
		int currentRank = currentRank(player, playTimeHours);
		int currentRankGroup = firstRankGroupPlayerIsMemberOfNow(player);
		while ((currentRankGroup >= 0) && (currentRankGroup != currentRank))
		{
			Config.C.removeGroupCommand.execute(player.getName(), Config.V.rankGroups[currentRankGroup]);
			currentRankGroup = firstRankGroupPlayerIsMemberOfNow(player);
		}
		if ((currentRankGroup == currentRank) || (currentRank < 0)) return;
		Config.C.addGroupCommand.execute(player.getName(), Config.V.rankGroups[currentRank]);
	}

	private void reportNextRank(Player player, int playTimeHours) {
		int nextRank = nextRank(player, playTimeHours);
		if (nextRank < 0) {
			Config.M.reachedHighestRank.sendMessage(player, Config.V.rankGroups[Config.V.rankGroups.length-1]);		
			return;			
		}
		String groupName = Config.V.rankGroups[nextRank];

		Config.M.timeToNextRank.sendMessage(player, Config.V.afterHours[nextRank] - playTimeHours, groupName);
	}

	private int nextRank(Player player, int playTimeHours) {
		for (int i = 0; i < Config.V.afterHours.length; i++) {
			int rankHour = Config.V.afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i;
			}
		}
		return -1;
	}

	private int currentRank(Player player, int playTimeHours) {
		for (int i = 0; i < Config.V.afterHours.length; i++) {
			int rankHour = Config.V.afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i-1;
			}
		}
		return Config.V.afterHours.length-1;
	}

	private int firstRankGroupPlayerIsMemberOfNow(Player player) {
		if (this._permissionService == null) return -1;
		Set<String> currentGroups = this._permissionService.getPlayerGroups(player.getUniqueId());
		if ((currentGroups == null) || (currentGroups.size() == 0)) {
			return -1;
		} else {
			for (int i = 0; i < Config.V.rankGroups.length; i++) {
				if (currentGroups.contains(Config.V.rankGroups[i])) return i;		
			}
		}
		return -1;
	}

}