package eaglesWings.botstate;

import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;

public class StarportRush extends BotState {

	protected StarportRush(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		// Check the build order
		int supply = game.getSelf().getSupplyUsed() / 2;
		switch (supply) {
		case 11:
			buildManager.setMinimum(UnitTypes.Terran_Barracks, 1);
			break;
		case 12:
			buildManager.setMinimum(UnitTypes.Terran_Refinery, 1);
			break;
		case 13:
			buildManager.setMinimum(UnitTypes.Terran_Supply_Depot, 2);
			break;
		case 16:
			buildManager.setMinimum(UnitTypes.Terran_Factory, 1);
			buildManager.setMinimum(UnitTypes.Terran_Vulture, 2);
			break;
		case 22:
			buildManager.setMinimum(UnitTypes.Terran_Starport, 2);
			buildManager.setMinimum(UnitTypes.Terran_Supply_Depot, 3);
			break;
		case 30:
			buildManager.setMinimum(UnitTypes.Terran_Supply_Depot, 4);
			break;
		}
		return this;
	}

	@Override
	public BotState unitComplete(int unitID) {
		Unit unit = game.getUnit(unitID);
		UnitType unitType = game.getUnitType(unit.getTypeID());
		if (unitType.isRefinery()) {
			for (int i = 0; i < 2; i++) {
				baseManager.getBuilder().gather(baseManager.getResource(unit));
			}
		}
		return this;
	}

}
