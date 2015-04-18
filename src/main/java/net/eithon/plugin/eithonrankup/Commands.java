package net.eithon.plugin.eithonrankup;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Commands {
	private static Commands singleton = null;

	private Commands() {
	}

	static Commands get()
	{
		if (singleton == null) {
			singleton = new Commands();
		}
		return singleton;
	}

	void enable(JavaPlugin plugin){
	}

	void disable() {
	}

	public boolean onCommand(CommandSender sender, String[] args) {
		if (args.length < 0) {
			sender.sendMessage("Incomplete command...");
			return false;
		}
		
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to use this command.");
			return false;
		}

		Commands.get().rankupCommand((Player) sender, args);
		return true;
	}

	public void rankupCommand(Player player, String[] args) {
		RankUp.get().rankup(player);
	}
}
