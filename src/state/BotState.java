package state;

import bwapi.Unit;

public abstract class BotState {
	public BotState() {
	}

	// Constructor for moving from one state to another
	protected BotState(BotState oldState) {
	}

	public abstract BotState onFrame();

	public BotState unitDiscover(Unit unit){
		return this;
	}
	
	public BotState unitConstructed(Unit unit) {
		return this;
	}

	public BotState unitDestroyed(Unit unit) {
		return this;
	}

	public BotState unitShown(Unit unit) {
		return this;
	}
}