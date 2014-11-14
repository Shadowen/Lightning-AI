package javabot.botstate;

import java.util.HashMap;
import java.util.Map;

import javabot.datastructure.Base;
import javabot.datastructure.BaseManager;
import javabot.datastructure.BuildManager;
import javabot.datastructure.Resource;
import javabot.datastructure.Worker;
import javabot.gamestructure.DebugEngine;
import javabot.gamestructure.GameHandler;
import javabot.model.BaseLocation;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class FirstFrameState extends BotState {

	public FirstFrameState(GameHandler igame) {
		super(igame);
	}

	@Override
	public BotState act() {
		// Sort through the units and find my CC
		Map<Integer, Unit> workers = new HashMap<Integer, Unit>();
		Unit commandCenter = null;
		for (Unit u : game.getMyUnits()) {
			if (u.getTypeID() == UnitTypes.Terran_Command_Center.ordinal()) {
				commandCenter = u;
				game.sendText("Command Center found.");
			}
		}
		if (commandCenter == null) {
			game.sendText("No Command Center found.");
			throw new NullPointerException(); // TODO custom exceptions
		}

		// Create a list of bases corresponding to BWTA's analysis
		for (BaseLocation location : game.getMap().getBaseLocations()) {
			Base b = new Base(game, location);
			// Main base
			if (b.location.getX() == commandCenter.getX()
					&& location.getY() == commandCenter.getY()) {
				b.commandCenter = commandCenter;
				game.sendText("Main base location found.");
			}
			baseManager.addBase(b);
		}

		// Sort SCVs and resources
		for (Unit u : game.getAllUnits()) {
			Base closestBase = baseManager.getClosestBase(u.getX(), u.getY());
			if (u.getTypeID() == UnitTypes.Resource_Mineral_Field.ordinal()) {
				game.sendText("Minerals found at (" + u.getX() + "," + u.getY()
						+ ")");
				closestBase.minerals.put(u.getID(), new Resource(u));
			} else if (u.getTypeID() == UnitTypes.Resource_Vespene_Geyser
					.ordinal()) {
				game.sendText("Gas found at (" + u.getX() + "," + u.getY()
						+ ")");
				closestBase.gas.put(u.getID(), new Resource(u));
			} else if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
				closestBase.workers.put(u.getID(), new Worker(game, u,
						closestBase));
				game.sendText("SCV found");
			}
		}

		// Notify we're complete
		game.sendText("First frame initialization complete!");
		// Move to an opening build
		return new OpeningBuildState(this);
	}
}
