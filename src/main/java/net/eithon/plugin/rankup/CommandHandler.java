package net.eithon.plugin.rankup;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.CommandParser;
import net.eithon.library.plugin.ICommandHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements ICommandHandler{

	private net.eithon.plugin.rankup.Controller _controller;


	public CommandHandler(EithonPlugin eithonPlugin, Controller controller) {
		this._controller = controller;
	}

	@Override
	public boolean onCommand(CommandParser commandParser) {
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(0,0)) return true;
		Player player = commandParser.getPlayerOrInformSender();
		if (player == null) return true;

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
