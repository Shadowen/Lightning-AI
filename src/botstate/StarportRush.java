package botstate;

import datastructure.Worker;
import datastructure.WorkerTask;
import bwapi.Unit;
import bwapi.UnitType;

public class StarportRush extends BotState {

	int previousSupply = 0;

	protected StarportRush(BotState oldState) {
		super(oldState);
		Worker w = baseManager.getBuilder();
		if (w == null) {
			game.sendText("Can't scout since no workers available!");
		} else {
			// w.setTask(WorkerTask.SCOUTING, null);
			// microManager.setScoutingUnit(w.getUnit());
		}
	}

	@Override
	public BotState act() {
		// Check the build order
		int supply = game.self().supplyUsed() / 2;
		if (previousSupply < supply) {
			switch (supply) {
			case 9:
				buildManager.setMinimum(UnitType.Terran_Supply_Depot, 1);
				break;
			case 11:
				buildManager.setMinimum(UnitType.Terran_Barracks, 1);
				if (!microManager.isScouting()) {
					Worker w = baseManager.getBuilder();
					if (w == null) {
						game.sendText("Can't scout since no workers available!");
					} else {
						// w.setTask(WorkerTask.SCOUTING, null);
						// microManager.setScoutingUnit(w.getUnit());
					}
				}
				break;
			case 12:
				buildManager.setMinimum(UnitType.Terran_Refinery, 1);
				break;
			case 13:
				buildManager.setMinimum(UnitType.Terran_Supply_Depot, 2);
				break;
			case 16:
				buildManager.setMinimum(UnitType.Terran_Factory, 1);
				buildManager.addToQueue(UnitType.Terran_Vulture, 2);
				break;
			case 22:
				buildManager.setMinimum(UnitType.Terran_Starport, 2);
				buildManager.setMinimum(UnitType.Terran_Supply_Depot, 3);
				buildManager.setMinimum(UnitType.Terran_Wraith, 50);
				break;
			case 30:
				buildManager.setMinimum(UnitType.Terran_Supply_Depot, 4);
				break;
			}
		}
		previousSupply = supply;

		if (supply > 30) {
			int numSupplyDepots = buildManager
					.getMyUnitCount(UnitType.Terran_Supply_Depot);
			buildManager
					.setMinimum(
							UnitType.Terran_Supply_Depot,
							numSupplyDepots
									+ (supply - game.self().supplyTotal() / 2 + 4) > 0 ? 1
									: 0);
		}
		return this;
	}

	@Override
	public BotState unitComplete(Unit unit) {
		UnitType unitType = unit.getType();
		if (unitType.isRefinery()) {
			for (int i = 0; i < 2; i++) {
				baseManager.getBuilder().gather(baseManager.getResource(unit));
			}
		}
		return this;
	}

}
