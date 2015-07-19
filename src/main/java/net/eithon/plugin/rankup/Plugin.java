package net.eithon.plugin.rankup;

import net.eithon.library.extensions.EithonPlugin;

public final class Plugin extends EithonPlugin {
	private Controller _controller;
	private EventListener _eventListener;

	@Override
	public void onEnable() {
		super.onEnable();
		Config.load(this);
		this._controller = new Controller(this);
		CommandHandler commandHandler = new CommandHandler(this, this._controller);
		this._eventListener = new EventListener(this, this._controller);
		super.activate(commandHandler, null);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		this._controller = null;
	}
}
