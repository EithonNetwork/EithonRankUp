package net.eithon.plugin.eithonrankup;

import java.util.List;
import java.util.Set;

import me.botsko.oracle.Oracle;
import net.eithon.library.core.CoreMisc;
import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.ConfigurableCommand;
import net.eithon.library.plugin.ConfigurableMessage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

public class RankUp {
	private static RankUp singleton = null;
	private static ConfigurableCommand addGroupCommand;
	private static ConfigurableCommand removeGroupCommand;
	private static ConfigurableMessage playTimeMessage;
	private static ConfigurableMessage timeToNextRankMessage;
	private static ConfigurableMessage rankedUpToGroupMessage;
	private static ConfigurableMessage reachedHighestRankMessage;

	private EithonPlugin _eithonPlugin = null;
	private ZPermissionsService _permissionService = null;
	private Plugin _oraclePlugin;
	private String[] _rankGroups;
	private Integer[] _afterHours;

	private RankUp() {
	}

	static RankUp get()
	{
		if (singleton == null) {
			singleton = new RankUp();
		}
		return singleton;
	}

	void enable(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		addGroupCommand = eithonPlugin.getConfigurableCommand("commands.AddGroup", 2,
				"perm player %s addgroup %s");
		removeGroupCommand = eithonPlugin.getConfigurableCommand("commands.RemoveGroup", 2,
				"perm player %s removegroup %s");
		playTimeMessage = eithonPlugin.getConfigurableMessage("PlayTime", 1,
				"You have played %d hours.");
		timeToNextRankMessage = eithonPlugin.getConfigurableMessage("messages.TimeToNextRank", 2,
				"You have %d hours left to rank %s.");
		rankedUpToGroupMessage = eithonPlugin.getConfigurableMessage("messages.RankedUpToGroup", 1,
				"You have been ranked up to group %s!");
		reachedHighestRankMessage = eithonPlugin.getConfigurableMessage("messages.ReachedHighestRank", 1,
				"You have reached the highest rank, %s!");
		List<String> stringList = eithonPlugin.getConfiguration().getStringList("RankGroups");
		if (stringList == null) this._rankGroups = new String[0];
		else this._rankGroups = stringList.toArray(new String[0]);
		Bukkit.getLogger().info(String.format("RankGroups: %s", CoreMisc.arrayToString(this._rankGroups)));
		List<Integer> integerList = eithonPlugin.getConfiguration().getIntegerList("AfterHours");
		if (integerList == null) this._afterHours = new Integer[0];
		else this._afterHours = integerList.toArray(new Integer[0]);
		Bukkit.getLogger().info(String.format("RankHours: %s", CoreMisc.arrayToString(this._afterHours)));
		connectToPermissionService();
		connectToOracle(this._eithonPlugin);
	}

	private void connectToOracle(EithonPlugin eithonPlugin) {
		this._oraclePlugin = eithonPlugin.getJavaPlugin().getServer().getPluginManager().getPlugin("Oracle");
		if (this._oraclePlugin != null && this._oraclePlugin.isEnabled()) {
			eithonPlugin.getDebug().info("Succesfully hooked into the Oracle plugin!");
		} else {
			this._oraclePlugin = null;
			eithonPlugin.getDebug().warning("RankUp doesn't work without the Oracle plugin");			
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
			Bukkit.getLogger().warning("RankUp doesn't work without the zPermissions plugin");
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
			rankedUpToGroupMessage.sendMessage(player, this._rankGroups[currentRank]);
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
			RankUp.playTimeMessage.sendMessage(player, playTimeHours);
		}
		return playTimeHours;
	}

	private void reportCurrentGroup(Player player) {
		int rankGroup = firstRankGroupPlayerIsMemberOfNow(player);
		if (rankGroup < 0) {
			player.sendMessage("You are not member of any groups");
			return;
		}
		player.sendMessage(String.format("You are currently in the rank group %s.", this._rankGroups[rankGroup]));
	}

	private void removeAndAddGroups(Player player, int playTimeHours) {
		int currentRank = currentRank(player, playTimeHours);
		int currentRankGroup = firstRankGroupPlayerIsMemberOfNow(player);
		while ((currentRankGroup >= 0) && (currentRankGroup != currentRank))
		{
			RankUp.removeGroupCommand.execute(player.getName(), this._rankGroups[currentRankGroup]);
			currentRankGroup = firstRankGroupPlayerIsMemberOfNow(player);
		}
		if ((currentRankGroup == currentRank) || (currentRank < 0)) return;
		RankUp.addGroupCommand.execute(player.getName(), this._rankGroups[currentRank]);
	}

	private void reportNextRank(Player player, int playTimeHours) {
		int nextRank = nextRank(player, playTimeHours);
		if (nextRank < 0) {
			RankUp.reachedHighestRankMessage.sendMessage(player, this._rankGroups[this._rankGroups.length-1]);		
			return;			
		}
		String groupName = this._rankGroups[nextRank];

		RankUp.timeToNextRankMessage.sendMessage(player, this._afterHours[nextRank] - playTimeHours, groupName);
	}

	private int nextRank(Player player, int playTimeHours) {
		for (int i = 0; i < this._afterHours.length; i++) {
			int rankHour = this._afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i;
			}
		}
		return -1;
	}

	private int currentRank(Player player, int playTimeHours) {
		for (int i = 0; i < this._afterHours.length; i++) {
			int rankHour = this._afterHours[i].intValue();
			if (rankHour > playTimeHours) {
				return i-1;
			}
		}
		return this._afterHours.length-1;
	}

	private int firstRankGroupPlayerIsMemberOfNow(Player player) {
		if (this._permissionService == null) return -1;
		Set<String> currentGroups = this._permissionService.getPlayerGroups(player.getUniqueId());
		if ((currentGroups == null) || (currentGroups.size() == 0)) {
			return -1;
		} else {
			for (int i = 0; i < this._rankGroups.length; i++) {
				if (currentGroups.contains(this._rankGroups[i])) return i;		
			}
		}
		return -1;
	}

}