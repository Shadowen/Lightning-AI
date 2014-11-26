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
		autoEconomy();
		autoSupplies();
		autoBuild();
		autoTrain();

		// Add barracks at 11 supply
		if (game.getSelf().getSupplyUsed() / 2 == 11) {
			// Check that it's not already in the queue
			if (!buildManager.buildQueueContains(UnitTypes.Terran_Barracks)) {
				buildManager.addBuilding(UnitTypes.Terran_Barracks);
			}
		}

		return this;
	}

	public BotState unitCreate(int unitID) {
		Unit u = game.getUnit(unitID);

		return this;
	}

	public BotState unitDiscover(int unitID) {
		Unit u = game.getUnit(unitID);

		return this;
	}

	public BotState unitComplete(int unitID) {
		super.unitComplete(unitID);
		Unit u = game.getUnit(unitID);

		int typeID = u.getTypeID();

		if (typeID == UnitTypes.Terran_SCV.ordinal()) {
			// Add new workers to nearest base
			Base base = baseManager.getClosestBase(u.getX(), u.getY());
			base.addWorker(unitID, u);
		} else if (typeID == UnitTypes.Terran_Barracks.ordinal()) {
			return new MassMarineState(this);
		}

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
