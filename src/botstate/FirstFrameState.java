package botstate;

import datastructure.BaseManager;

public class FirstFrameState extends BotState {

	public FirstFrameState() {
		super();
	}

	@Override
	public BotState act() {
		// First base is main
		BaseManager.main = BaseManager.getMyBases().iterator().next();
		return new StarportRush(this);
	}
}
