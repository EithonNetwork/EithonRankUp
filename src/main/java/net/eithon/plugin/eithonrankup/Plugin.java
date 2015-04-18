package net.eithon.plugin.eithonrankup;

import net.eithon.library.extensions.EithonPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {
	@Override
	public void onEnable() {
		EithonPlugin eithonPlugin = EithonPlugin.get(this);
		eithonPlugin.enable();
		Commands.get().enable(this);
		RankUp.get().enable(eithonPlugin);
	}

	@Override
	public void onDisable() {
		EithonPlugin eithonPlugin = EithonPlugin.get(this);
		eithonPlugin.disable();
		Commands.get().disable();
		RankUp.get().disable();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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
}
