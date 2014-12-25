package eaglesWings.botstate;

import javabot.model.BaseLocation;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;
import eaglesWings.datastructure.Base;
import eaglesWings.datastructure.BaseManager;
import eaglesWings.datastructure.BaseStatus;
import eaglesWings.datastructure.BuildManager;
import eaglesWings.datastructure.GasResource;
import eaglesWings.datastructure.MineralResource;
import eaglesWings.datastructure.Resource;
import eaglesWings.gamestructure.GameHandler;
import eaglesWings.micromanager.MicroManager;
import eaglesWings.pathfinder.PathingManager;

public class FirstFrameState extends BotState {

	public FirstFrameState(GameHandler igame, BaseManager baseManager,
			BuildManager buildManager, MicroManager imicroManager,
			PathingManager pathingManager) {
		super(igame, baseManager, buildManager, imicroManager, pathingManager);
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
			game.sendText("No Command Center found.");
			return this;
		}

		// Create a list of bases corresponding to BWTA's analysis
		for (BaseLocation location : game.getMap().getBaseLocations()) {
			Base b = new Base(game, location);
			// Main base
			if (location.getX() == commandCenter.getX()
					&& location.getY() == commandCenter.getY()) {
				b.commandCenter = commandCenter;
				b.setStatus(BaseStatus.OCCUPIED_SELF);
				baseManager.main = b;
				game.sendText("Main set");
			}
			baseManager.addBase(b);
		}

		// Sort SCVs and resources
		for (Unit u : game.getAllUnits()) {
			Base closestBase = baseManager.getClosestBase(u.getX(), u.getY());
			if (u.getTypeID() == UnitTypes.Resource_Mineral_Field.ordinal()) {
				closestBase.minerals.put(u.getID(), new MineralResource(u));
			} else if (u.getTypeID() == UnitTypes.Resource_Vespene_Geyser
					.ordinal()) {
				closestBase.gas.put(u.getID(), new GasResource(u));
			} else if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
				closestBase.addWorker(u.getID(), u);
			}
		}

		// Some other init
		buildManager.setMinimum(UnitTypes.Terran_Command_Center, 1);

		// Notify we're complete
		game.sendText("First frame initialization complete!");
		// Move to an opening build
		return new StarportRush(this);
	}
}
