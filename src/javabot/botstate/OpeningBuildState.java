package javabot.botstate;

import java.awt.Point;
import java.util.Iterator;

import javabot.JavaBot;
import javabot.datastructure.Base;
import javabot.datastructure.GameHandler;
import javabot.datastructure.Resource;
import javabot.datastructure.Worker;
import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;

public class OpeningBuildState extends BotState {

	public OpeningBuildState(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		for (Base b : baseManager.getMyBases()) {
			b.gatherResources(game);

			// Train SCVS if necessary
			if (b.workers.size() < b.getMineralCount() * 2) {
				if (game.getSelf().getMinerals() >= 50
						&& b.commandCenter.getTrainingQueueSize() < 1) {
					game.train(b.commandCenter.getID(),
							UnitTypes.Terran_SCV.ordinal());
				}
			}
		}

		UnitTypes toBuild = buildManager.getToBuild();
		// Add supply depots if necessary
		if (game.getSelf().getSupplyUsed() > game.getSelf().getSupplyTotal() - 3) {
			game.sendText("Require additional Supply Depots.");
			// Check that it's not already in the queue
			if (buildManager.getToBuild() != UnitTypes.Terran_Supply_Depot) {
				buildManager.addBuilding(UnitTypes.Terran_Supply_Depot);
			}
		}
		// Attempt to build the next building
		if (game.getSelf().getMinerals() > 100) {
			baseManager.getBuilder().build();// game.build
		}

		return this;
	}

	public BotState unitCreate(int unitID) {
		Unit u = game.getUnit(unitID);
		if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
			// Add new workers to nearest base
			Base base = baseManager.getClosestBase(u.getX(), u.getY());
			base.workers.put(u.getID(), new Worker(game, u, base));
			game.sendText("Added worker to base!");
			game.sendText("Worker found at (" + u.getX() + "," + u.getY() + ")");
		} else if (u.getTypeID() == UnitTypes.Terran_Command_Center.ordinal()) {
			Base base = baseManager.getClosestBase(u.getX(), u.getY());
			base.commandCenter = u;
			game.sendText("Found new Command Center!");
		}
		return this;
	}

	public BotState unitDiscover(int unitID) {
		Unit u = game.getUnit(unitID);
		if (u.getTypeID() == UnitTypes.Resource_Mineral_Field.ordinal()) {
			Base base = baseManager.getClosestBase(u.getX(), u.getY());
			base.minerals.put(u.getID(), new Resource(u));
			game.sendText("Minerals found at (" + u.getX() + "," + u.getY()
					+ ")");
		} else if (u.getTypeID() == UnitTypes.Resource_Vespene_Geyser.ordinal()) {
			Base base = baseManager.getClosestBase(u.getX(), u.getY());
			base.gas.put(u.getID(), new Resource(u));
			game.sendText("Gas found at (" + u.getX() + "," + u.getY() + ")");
		}
		return this;
	}

	public BotState unitDestroy(int unitID) {
		for (Base b : baseManager) {
			if (b.workers.remove(unitID) != null) {
				break;
			}
		}
		return this;
	}
}
