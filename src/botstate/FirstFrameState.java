package botstate;

import micromanager.MicroManager;
import pathfinder.PathingManager;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.BuildManager;
import bwta.BWTA;
import bwta.BaseLocation;
import gamestructure.DebugEngine;
import gamestructure.GameHandler;

public class FirstFrameState extends BotState {

	public FirstFrameState(GameHandler igame, BaseManager baseManager,
			BuildManager buildManager, MicroManager imicroManager,
			PathingManager pathingManager) {
		super(igame, baseManager, buildManager, imicroManager, pathingManager);

		// Create a list of bases corresponding to BWTA's analysis
		for (BaseLocation location : BWTA.getBaseLocations()) {
			Base b = new Base(game, location);
			baseManager.addBase(b);
		}

		// // Main base
		// Base main = baseManager.getClosestBase(commandCenter.getX(),
		// commandCenter.getY());
		// main.commandCenter = commandCenter;
		// baseManager.main = main;
		// game.sendText("Main set");
		//
		// // Sort SCVs and resources
		// for (Unit u : game.getAllUnits()) {
		// Base closestBase = baseManager.getClosestBase(u.getX(), u.getY());
		// if (closestBase != null) {
		// if (u.getType() == UnitType.Resource_Mineral_Field) {
		// closestBase.minerals.put(u.getID(), new MineralResource(u));
		// } else if (u.getType() == UnitType.Resource_Vespene_Geyser) {
		// closestBase.gas.put(u.getID(), new GasResource(u));
		// } else if (u.getType() == UnitType.Terran_SCV) {
		// game.sendText("Found SCV");
		// closestBase.addWorker(u);
		// }
		// }
		// }

		// Notify we're complete
		game.sendText("First frame initialization complete!");
	}

	@Override
	public BotState act() {
		return this;
		// Move to an opening build
		// return new StarportRush(this);
	}
}
