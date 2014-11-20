package javabot.botstate;

import javabot.datastructure.Base;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class MassMarineState extends BotState {

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

		return this;
	}

	public BotState unitComplete(int unitID) {
		Unit u = game.getUnit(unitID);
		buildManager.doneBuilding(u);
		return this;
	}

}
