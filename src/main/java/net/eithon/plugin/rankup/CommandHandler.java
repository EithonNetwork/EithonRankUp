package net.eithon.plugin.rankup;

import net.eithon.library.extensions.EithonPlayer;
import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.CommandParser;
import net.eithon.library.plugin.ICommandHandler;

import org.bukkit.command.CommandSender;

public class CommandHandler implements ICommandHandler{

	private net.eithon.plugin.rankup.Controller _controller;

	public CommandHandler(EithonPlugin eithonPlugin, Controller controller) {
		this._controller = controller;
	}

	@Override
	public boolean onCommand(CommandParser commandParser) {
		String argument = commandParser.getArgumentStringAsLowercase();
		if ((argument != null) && argument.equalsIgnoreCase("test")) {
			Config.M.rememberToRankUp.sendMessage(commandParser.getSender(), "TESTING_STUFF");
			return true;
		}
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(0,0)) return true;
		EithonPlayer player = commandParser.getEithonPlayerOrInformSender();
		if (player == null) return true;
		if (player.hasPermission("rankup.noteligible")) {
			Config.M.notEligibleForRankUp.sendMessage(player.getPlayer());
			return true;
		}

		rankupCommand(commandParser);
		return true;
	}

	public void rankupCommand(CommandParser commandParser) {
		this._controller.rankup(commandParser.getPlayer());
	}

	@Override
	public void showCommandSyntax(CommandSender sender, String command) {	
		sender.sendMessage("/rankup");
	}
}
