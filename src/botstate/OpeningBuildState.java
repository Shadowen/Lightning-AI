package botstate;

import static datastructure.BuildManager.buildManager;
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
		if (GameHandler.getSelfPlayer().supplyUsed() / 2 == 11) {
			// Check that it's not already in the queue
			if (!buildManager().isInQueue(UnitType.Terran_Barracks)) {
				buildManager().addToQueue(UnitType.Terran_Barracks);
			}
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
}
