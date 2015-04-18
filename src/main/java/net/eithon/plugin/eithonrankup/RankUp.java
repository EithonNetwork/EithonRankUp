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
	private static ConfigurableCommand setGroupCommand;
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
		setGroupCommand = eithonPlugin.getConfigurableCommand("commands.SetGroup", 2,
				"perm player %s addgroup %s");
		playTimeMessage = eithonPlugin.getConfigurableMessage("PlayTime", 1,
				"You have played %d hours.");
		timeToNextRankMessage = eithonPlugin.getConfigurableMessage("TimeToNextRank", 2,
				"You have %d hours left to rank %s.");
		rankedUpToGroupMessage = eithonPlugin.getConfigurableMessage("RankedUpToGroup", 1,
				"You have been ranked up to group %s!");
		reachedHighestRankMessage = eithonPlugin.getConfigurableMessage("ReachedHighestRank", 1,
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
		String currentGroup = null;
		if (this._permissionService == null) {
			player.sendMessage("RankUp doesn't work without the zPermissions plugin");			
		} else {
			currentGroup = reportCurrentGroup(player);
		}
		
		int playTimeHours = 0;
		if (this._oraclePlugin == null) {
			player.sendMessage("RankUp doesn't work without the Oracle plugin");
		} else {
			playTimeHours = reportPlayTime(player, playTimeHours);
		}

		addRelevantGroups(player, currentGroup, playTimeHours);
		reportNextGroup(player, playTimeHours);
	}

	private int reportPlayTime(Player player, int playTimeHours) {
		Integer playTime = Oracle.playtimeHours.get(player);
		if (playTime == null) {
			player.sendMessage(String.format("Could not find any playtime information for player %s.", player.getName()));
		} else {
			playTimeHours = playTime.intValue();
			RankUp.playTimeMessage.sendMessage(player, playTimeHours);
		}
		return playTimeHours;
	}

	private String reportCurrentGroup(Player player) {
		String lastGroup = null;
		Set<String> groups = this._permissionService.getPlayerGroups(player.getUniqueId());
		if ((groups == null) || (groups.size() == 0)) {
			player.sendMessage("You are not member of any groups");
		} else {
			for (String s : this._rankGroups) {
				if (groups.contains(s)) lastGroup = s;
			}
			if (lastGroup != null) {
				player.sendMessage(String.format("You are currently ranked as %s.", lastGroup));
			}
		}
		return lastGroup;
	}
	private void addRelevantGroups(Player player, String currentGroup, int playTimeHours) {
		for (int i = 0; i < this._afterHours.length; i++) {
			int rankHour = this._afterHours[i].intValue();
			String groupName = this._rankGroups[i];
			if (rankHour > playTimeHours) {
				break;
			}
			// Increase rank?
			if (currentGroup == null) {
				RankUp.setGroupCommand.execute(player.getName(), groupName);
				RankUp.rankedUpToGroupMessage.sendMessage(player, groupName);			
			} else {
				if (groupName.equalsIgnoreCase(currentGroup)) currentGroup = null;
			}
		}
	}

	private String reportNextGroup(Player player, int playTimeHours) {
		String groupName = null;
		for (int i = 0; i < this._afterHours.length; i++) {
			int rankHour = this._afterHours[i].intValue();
			groupName = this._rankGroups[i];
			if (rankHour > playTimeHours) {
				RankUp.timeToNextRankMessage.sendMessage(player, rankHour - playTimeHours, groupName);			
				return groupName;
			}
		}
		RankUp.reachedHighestRankMessage.sendMessage(player, groupName);		
		return null;
	}

}