package botstate;

import datastructure.BuildManager;
import gamestructure.GameHandler;
import bwapi.Unit;
import bwapi.UnitType;

public class OpeningBuildState extends BotState {

	public OpeningBuildState(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		// Add barracks at 11 supply
		int supply = GameHandler.getSelfPlayer().supplyUsed() / 2;
		if (supply >= 9) {
			BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 1);
		} else if (supply >= 11) {
			BuildManager.setMinimum(UnitType.Terran_Barracks, 1);
		}

		return this;
	}

	@Override
	public BotState unitComplete(Unit unit) {
		if (unit.getType() == UnitType.Terran_Barracks) {
			return new MassMarineState(this);
		}

		return this;
	}

	public BotState unitShown(Unit unit) {
		if (unit.getType() == UnitType.Protoss_Gateway) {
			return new MassMarineState(this);
		}
		return this;
	}
}
