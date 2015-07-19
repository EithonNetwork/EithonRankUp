package net.eithon.plugin.rankup;

import java.util.List;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.ConfigurableMessage;
import net.eithon.library.plugin.Configuration;

public class Config {
	public static void load(EithonPlugin plugin)
	{
		Configuration config = plugin.getConfiguration();
		V.load(config);
		C.load(config);
		M.load(config);

	}
	public static class V {
		public static Integer[] afterHours;
		public static String[] rankGroups;
		public static int remindAfterMinutes;

		static void load(Configuration config) {
			List<String> stringList = config.getStringList("RankGroups");
			if (stringList == null) rankGroups = new String[0];
			else rankGroups = stringList.toArray(new String[0]);
			List<Integer> integerList = config.getIntegerList("AfterHours");
			if (integerList == null) afterHours = new Integer[0];
			else afterHours = integerList.toArray(new Integer[0]);
			remindAfterMinutes = config.getInt("RemindAfterMinutes", 10);
		}

	}
	public static class C {
		static void load(Configuration config) {
		}

	}
	public static class M {
		public static ConfigurableMessage playTime;
		public static ConfigurableMessage timeToNextRank;
		public static ConfigurableMessage rankedUpToGroup;
		public static ConfigurableMessage reachedHighestRank;
		public static ConfigurableMessage notEligibleForRankUp;
		public static ConfigurableMessage rememberToRankUp;

		static void load(Configuration config) {
			playTime = config.getConfigurableMessage("PlayTime_1", 1,
					"You have played %d hours.");
			timeToNextRank = config.getConfigurableMessage("messages.TimeToNextRank_2", 2,
					"You have %d hours left to rank %s.");
			rankedUpToGroup = config.getConfigurableMessage("messages.RankedUpToGroup_1", 1,
					"You have been ranked up to group %s!");
			reachedHighestRank = config.getConfigurableMessage("messages.ReachedHighestRank_1", 1,
					"You have reached the highest rank, %s!");
			notEligibleForRankUp = config.getConfigurableMessage("messages.NotEligibleForRankUp_0", 0,
					"You are not eligible for rank up.");
			rememberToRankUp = config.getConfigurableMessage("messages.RememberToRankUp_1", 1,
					"You are now eligible for rank up to %s.");
		}		
	}

}
