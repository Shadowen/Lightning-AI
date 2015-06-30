package botstate;

import bwapi.Unit;

public abstract class BotState {
	public BotState() {
	}

	// Constructor for moving from one state to another
	protected BotState(BotState oldState) {
	}

	public abstract BotState act();

	public BotState unitCreate(Unit unit) {
		return this;
	}

	public BotState unitComplete(Unit unit) {
		return this;
	}

	public BotState unitDestroyed(Unit unit) {
		return this;
	}

	public BotState unitDiscover(Unit unit) {
		return this;
	}
}
