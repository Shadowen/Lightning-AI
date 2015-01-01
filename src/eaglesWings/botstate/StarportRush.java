package eaglesWings.botstate;

import eaglesWings.datastructure.Worker;
import eaglesWings.datastructure.WorkerTask;
import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;

public class StarportRush extends BotState {

	int previousSupply = 0;

	protected StarportRush(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		// TODO
		if (!microManager.isScouting()) {
			Worker w = baseManager.getBuilder();
			if (w == null) {
				game.sendText("Can't scout since no workers available!");
			} else {
				w.setTask(WorkerTask.Scouting, null);
				microManager.setScoutingUnit(w.getUnit());
			}
		}

		// Check the build order
		int supply = game.getSelf().getSupplyUsed() / 2;
		if (previousSupply < supply) {
			switch (supply) {
			case 9:
				buildManager.setMinimum(UnitTypes.Terran_Supply_Depot, 1);
				break;
			case 11:
				buildManager.setMinimum(UnitTypes.Terran_Barracks, 1);
				if (!microManager.isScouting()) {
					Worker w = baseManager.getBuilder();
					if (w == null) {
						game.sendText("Can't scout since no workers available!");
					} else {
						w.setTask(WorkerTask.Scouting, null);
						microManager.setScoutingUnit(w.getUnit());
					}
				}
				break;
			case 12:
				buildManager.setMinimum(UnitTypes.Terran_Refinery, 1);
				break;
			case 13:
				buildManager.setMinimum(UnitTypes.Terran_Supply_Depot, 2);
				break;
			case 16:
				buildManager.setMinimum(UnitTypes.Terran_Factory, 1);
				buildManager.addToQueue(UnitTypes.Terran_Vulture, 2);
				break;
			case 22:
				buildManager.setMinimum(UnitTypes.Terran_Starport, 2);
				buildManager.setMinimum(UnitTypes.Terran_Supply_Depot, 3);
				buildManager.setMinimum(UnitTypes.Terran_Wraith, 50);
				break;
			case 30:
				buildManager.setMinimum(UnitTypes.Terran_Supply_Depot, 4);
				break;
			}
		}
		previousSupply = supply;

		if (supply > 30) {
			int numSupplyDepots = buildManager
					.getMyUnitCount(UnitTypes.Terran_Supply_Depot);
			buildManager
					.setMinimum(UnitTypes.Terran_Supply_Depot,
							numSupplyDepots
									+ (supply - game.getSelf().getSupplyTotal()
											/ 2 + 4) > 0 ? 1 : 0);
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
