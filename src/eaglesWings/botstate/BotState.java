package eaglesWings.botstate;

import eaglesWings.datastructure.BaseManager;
import eaglesWings.datastructure.BuildManager;
import eaglesWings.gamestructure.GameHandler;

public abstract class BotState {
	public GameHandler game;
	public BaseManager baseManager;
	public BuildManager buildManager;

	// Constructor used for creating a new BotState
	protected BotState(GameHandler igame, BaseManager ibaseManager,
			BuildManager ibuildManager) {
		game = igame;
		baseManager = ibaseManager;
		buildManager = ibuildManager;
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
