package javabot.botstate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javabot.datastructure.Base;
import javabot.datastructure.BaseManager;
import javabot.datastructure.BuildManager;
import javabot.datastructure.GameHandler;

public abstract class BotState {
	public GameHandler game;
	public BaseManager baseManager;
	public BuildManager buildManager;

	// Constructor used for creating a new BotState
	protected BotState() {
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
}
