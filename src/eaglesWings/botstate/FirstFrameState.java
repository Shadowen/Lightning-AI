package eaglesWings.botstate;

import java.util.HashMap;
import java.util.Map;

import eaglesWings.datastructure.Base;
import eaglesWings.datastructure.BaseManager;
import eaglesWings.datastructure.BuildManager;
import eaglesWings.datastructure.GasResource;
import eaglesWings.datastructure.Resource;
import eaglesWings.datastructure.Worker;
import eaglesWings.gamestructure.DebugEngine;
import eaglesWings.gamestructure.Debuggable;
import eaglesWings.gamestructure.GameHandler;
import javabot.model.BaseLocation;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class FirstFrameState extends BotState {

	public FirstFrameState(GameHandler igame, BaseManager baseManager,
			BuildManager buildManager) {
		super(igame, baseManager, buildManager);
	}

	@Override
	public BotState act() {
		// Sort through the units and find my CC
		Unit commandCenter = null;
		for (Unit u : game.getMyUnits()) {
			if (u.getTypeID() == UnitTypes.Terran_Command_Center.ordinal()) {
				commandCenter = u;
				game.sendText("Command Center found.");
			}
		}
		if (commandCenter == null) {
			return this;
			// game.sendText("No Command Center found.");
			// throw new NullPointerException(); // TODO custom exceptions
		}

		// Create a list of bases corresponding to BWTA's analysis
		for (BaseLocation location : game.getMap().getBaseLocations()) {
			Base b = new Base(game, location);
			// Main base
			if (b.location.getX() == commandCenter.getX()
					&& location.getY() == commandCenter.getY()) {
				b.commandCenter = commandCenter;
				baseManager.main = b;
				game.sendText("Main set");
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
				closestBase.gas.put(u.getID(), new GasResource(u));
			} else if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
				closestBase.addWorker(u.getID(), u);
				game.sendText("SCV found");
			}
		}

		// Notify we're complete
		game.sendText("First frame initialization complete!");
		// Move to an opening build
		return new OpeningBuildState(this);
	}
}
