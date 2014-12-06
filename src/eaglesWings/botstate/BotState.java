package eaglesWings.botstate;

import eaglesWings.datastructure.BaseManager;
import eaglesWings.datastructure.BuildManager;
import eaglesWings.gamestructure.GameHandler;
import eaglesWings.pathfinder.PathingManager;

public abstract class BotState {
	protected GameHandler game;
	protected BaseManager baseManager;
	protected BuildManager buildManager;
	protected PathingManager pathingManager;

	// Constructor used for creating a new BotState
	protected BotState(GameHandler igame, BaseManager ibaseManager,
			BuildManager ibuildManager, PathingManager ipathingManager) {
		game = igame;
		baseManager = ibaseManager;
		buildManager = ibuildManager;
		pathingManager = ipathingManager;
	}

	// Constructor for moving from one state to another
	protected BotState(BotState oldState) {
		game = oldState.game;
		baseManager = oldState.baseManager;
		buildManager = oldState.buildManager;
		pathingManager = oldState.pathingManager;
	}

	public abstract BotState act();

	public BotState unitCreate(int unitID) {
		return this;
	}

	public BotState unitComplete(int unitID) {
		return this;
	}

	public BotState unitDestroy(int unitID) {
		return this;
	}

	public BotState unitDiscover(int unitID) {
		return this;
	}
}
