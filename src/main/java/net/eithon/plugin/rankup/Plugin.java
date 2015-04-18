package net.eithon.plugin.rankup;

import net.eithon.library.extensions.EithonPlugin;

public final class Plugin extends EithonPlugin {
	private Controller _controller;

	@Override
	public void onEnable() {
		this._controller = new Controller(this);
		CommandHandler commandHandler = new CommandHandler(this, this._controller);
		super.enable(commandHandler, null);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		this._controller = null;
	}
}
