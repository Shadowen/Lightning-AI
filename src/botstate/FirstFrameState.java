package botstate;

import micromanager.MicroManager;
import pathfinder.PathingManager;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.BuildManager;
import bwta.BWTA;
import bwta.BaseLocation;
import gamestructure.GameHandler;

public class FirstFrameState extends BotState {

	public FirstFrameState(GameHandler igame, BaseManager baseManager,
			BuildManager buildManager, MicroManager imicroManager,
			PathingManager pathingManager) {
		super(igame, baseManager, buildManager, imicroManager, pathingManager);
	}

	@Override
	public BotState act() {
		// First base is main
		baseManager.main = baseManager.getMyBases().iterator().next();
		return new StarportRush(this);
	}
}
