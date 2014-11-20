package javabot.botstate;

import java.awt.Point;
import java.util.Iterator;

import javabot.datastructure.Base;
import javabot.datastructure.BuildingPlan;
import javabot.datastructure.Resource;
import javabot.datastructure.Worker;
import javabot.gamestructure.DebugEngine;
import javabot.gamestructure.GameHandler;
import javabot.gamestructure.JavaBot;
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
			b.gatherResources();

			// Train SCVS if necessary
			if (b.getWorkerCount() < b.getMineralCount() * 2) {
				if (game.getSelf().getMinerals() >= 50
						&& b.commandCenter.getTrainingQueueSize() == 0) {
					game.train(b.commandCenter.getID(),
							UnitTypes.Terran_SCV.ordinal());
				}
			}
		}

		BuildingPlan toBuild = buildManager.getToBuild();
		// Add supply depots if necessary
		if (game.getSelf().getSupplyUsed() > game.getSelf().getSupplyTotal() - 4) {
			// Check that it's not already in the queue
			if (buildManager.getToBuild() == null
					|| !buildManager
							.buildQueueContains(UnitTypes.Terran_Supply_Depot)) {
				Point location = game.getBuildLocation(
						baseManager.getMain().location.getX(),
						baseManager.getMain().location.getY(),
						UnitTypes.Terran_Supply_Depot);
				buildManager.addBuilding(location,
						UnitTypes.Terran_Supply_Depot);
			}
		}

		// Add barracks at 11 supply
		if (game.getSelf().getSupplyUsed() / 2 == 11) {
			// Check that it's not already in the queue
			if (buildManager.getToBuild() == null
					|| !buildManager
							.buildQueueContains(UnitTypes.Terran_Barracks)) {
				Point location = game.getBuildLocation(
						baseManager.getMain().location.getX(),
						baseManager.getMain().location.getY(),
						UnitTypes.Terran_Barracks);
				buildManager.addBuilding(location, UnitTypes.Terran_Barracks);
			}
		} else if (game.getSelf().getSupplyUsed() == 13) {
		}

		// Attempt to build the next building
		if (toBuild != null) {
			// If we have the minerals and gas
			if (game.getSelf().getMinerals() > game.getUnitType(
					toBuild.getTypeID()).getMineralPrice()
					&& game.getSelf().getGas() >= game.getUnitType(
							toBuild.getTypeID()).getGasPrice()) {
				// If it has a builder, tell them to hurry up!
				if (toBuild.hasBuilder()) {
					toBuild.builder.build(toBuild);
				} else {
					// If it isn't being built yet
					baseManager.getBuilder().build(toBuild);
				}
			}
		}

		return this;
	}

	public BotState unitCreate(int unitID) {
		Unit u = game.getUnit(unitID);
		int typeID = u.getTypeID();
		UnitType type = game.getUnitType(typeID);

		buildManager.doneBuilding(u);

		if (typeID == UnitTypes.Terran_SCV.ordinal()) {
			// Add new workers to nearest base
			Base base = baseManager.getClosestBase(u.getX(), u.getY());
			base.addWorker(unitID, u);
			game.sendText("Added worker to base!");
			game.sendText("Worker found at (" + u.getX() + "," + u.getY() + ")");
		} else if (typeID == UnitTypes.Terran_Command_Center.ordinal()) {
			baseManager.getClosestBase(u.getX(), u.getY()).commandCenter = u;
			game.sendText("Found new Command Center!");
		} else if (typeID == UnitTypes.Terran_Supply_Depot.ordinal()) {
			game.sendText("Found new Supply Depot!");
		}

		return this;
	}

	public BotState unitDiscover(int unitID) {
		Unit u = game.getUnit(unitID);

		return this;
	}

	public BotState unitDestroy(int unitID) {
		for (Base b : baseManager) {
			if (b.removeWorker(unitID)) {
				break;
			}
		}
		return this;
	}
}
