package botstate;

import micromanager.MicroManager;
import pathfinder.PathingManager;
import datastructure.BaseManager;
import datastructure.BuildManager;
import bwapi.Unit;
import gamestructure.DebugEngine;
import gamestructure.GameHandler;

public abstract class BotState {
	protected GameHandler game;
	protected BaseManager baseManager;
	protected BuildManager buildManager;
	protected MicroManager microManager;
	protected PathingManager pathingManager;

	// Constructor used for creating a new BotState
	protected BotState(GameHandler igame, BaseManager ibaseManager,
			BuildManager ibuildManager, MicroManager imicroManager,
			PathingManager ipathingManager) {
		game = igame;
		baseManager = ibaseManager;
		buildManager = ibuildManager;
		microManager = imicroManager;
		pathingManager = ipathingManager;
	}

	// Constructor for moving from one state to another
	protected BotState(BotState oldState) {
		game = oldState.game;
		baseManager = oldState.baseManager;
		buildManager = oldState.buildManager;
		microManager = oldState.microManager;
		pathingManager = oldState.pathingManager;
	}

	public abstract BotState act();

	public BotState unitCreate(Unit unit) {
		return this;
	}

	public BotState unitComplete(Unit unit) {
		return this;
	}

	public BotState unitDestroy(Unit unit) {
		return this;
	}

	public BotState unitDiscover(Unit unit) {
		return this;
	}
}
