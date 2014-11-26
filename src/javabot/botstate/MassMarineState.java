package javabot.botstate;

import java.awt.Point;

import javabot.datastructure.Base;
import javabot.datastructure.BuildingPlan;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class MassMarineState extends BotState {

	int barracksCount = 1;

	public MassMarineState(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		autoEconomy();
		autoSupplies();
		autoBuild();
		autoTrain();

		// Build marines
		if (buildManager.getToTrain() == null) {
			buildManager.addUnit(UnitTypes.Terran_Marine);
		}

		BuildingPlan bp = buildManager.getToBuild();
		if (game.getSelf().getMinerals() > 200
				&& !buildManager.buildQueueContains(UnitTypes.Terran_Barracks)) {
			// Add more barracks
			buildManager.addBuilding(UnitTypes.Terran_Barracks);
		}

		return this;
	}

	public BotState unitComplete(int unitID) {
		super.unitComplete(unitID);
		return this;
	}

}
