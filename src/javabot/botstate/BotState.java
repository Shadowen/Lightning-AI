package javabot.botstate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javabot.datastructure.Base;
import javabot.datastructure.BaseManager;
import javabot.datastructure.BuildManager;
import javabot.gamestructure.DebugEngine;
import javabot.gamestructure.DebugModule;
import javabot.gamestructure.Debuggable;
import javabot.gamestructure.GameHandler;

public abstract class BotState implements Debuggable {
	public GameHandler game;
	public BaseManager baseManager;
	public BuildManager buildManager;

	// Constructor used for creating a new BotState
	protected BotState(GameHandler igame) {
		game = igame;
		baseManager = new BaseManager(game.getSelf().getID(), game);
		buildManager = new BuildManager(game);
	}

	// Constructor for moving from one state to another
	protected BotState(BotState oldState) {
		game = oldState.game;
		baseManager = oldState.baseManager;
		buildManager = oldState.buildManager;
	}

	public abstract BotState act();

	public BotState unitCreate(int unitID) {
		return this;
	}

	public BotState unitDestroy(int unitID) {
		return this;
	}

	public BotState unitDiscover(int unitID) {
		return this;
	}

	@Override
	public void registerDebugFunctions(GameHandler g) {
		// Which botstate am I in?
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				game.drawText(5, 5, this.getClass().toString(), true);
			}
		});

		buildManager.registerDebugFunctions(g);
		baseManager.registerDebugFunctions(g);
	}
}
