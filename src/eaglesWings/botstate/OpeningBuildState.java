package eaglesWings.botstate;

import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class OpeningBuildState extends BotState {

	public OpeningBuildState(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		// Add barracks at 11 supply
		if (game.getSelf().getSupplyUsed() / 2 == 11) {
			// Check that it's not already in the queue
			if (!buildManager.buildQueueContains(UnitTypes.Terran_Barracks)) {
				buildManager.addToQueue(UnitTypes.Terran_Barracks);
			}
		}

		return this;
	}

	public BotState unitComplete(int unitID) {
		Unit u = game.getUnit(unitID);
		int typeID = u.getTypeID();

		if (typeID == UnitTypes.Terran_Barracks.ordinal()) {
			return new MassMarineState(this);
		}

		return this;
	}
}
