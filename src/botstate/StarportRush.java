package botstate;

import gamestructure.GameHandler;

import java.util.Optional;

import micromanager.MicroManager;
import bwapi.Unit;
import bwapi.UnitType;
import datastructure.BaseManager;
import datastructure.BuildManager;
import datastructure.Worker;

public class StarportRush extends BotState {

	int previousSupply = 0;

	protected StarportRush(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		// Check the build order
		int supply = GameHandler.getSelfPlayer().supplyUsed() / 2;
		if (previousSupply < supply) {
			switch (supply) {
			case 9:
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 1);
				break;
			case 11:
				BuildManager.setMinimum(UnitType.Terran_Barracks, 1);
				if (!MicroManager.isScouting()) {
					Optional<Worker> w = BaseManager.getBuilder();
					if (!w.isPresent()) {
						GameHandler
								.sendText("Can't scout since no workers available!");
					} else {
						// w.get().setTask(WorkerTask.SCOUTING, null);
						// microManager.setScoutingUnit(w.get().getUnit());
					}
				}
				break;
			case 12:
				BuildManager.setMinimum(UnitType.Terran_Refinery, 1);
				break;
			case 13:
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 2);
				break;
			case 16:
				BuildManager.setMinimum(UnitType.Terran_Factory, 1);
				BuildManager.addToQueue(UnitType.Terran_Vulture, 2);
				break;
			case 22:
				BuildManager.setMinimum(UnitType.Terran_Starport, 2);
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 3);
				BuildManager.setMinimum(UnitType.Terran_Wraith, 50);
				break;
			case 30:
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 4);
				break;
			}
		}
		previousSupply = supply;

		if (supply > 30) {
			int numSupplyDepots = BuildManager
					.getMyUnitCount(UnitType.Terran_Supply_Depot);
			BuildManager.setMinimum(UnitType.Terran_Supply_Depot,
					numSupplyDepots
							+ (supply
									- GameHandler.getSelfPlayer().supplyTotal()
									/ 2 + 4) > 0 ? 1 : 0);
		}
		return this;
	}

	@Override
	public BotState unitComplete(Unit unit) {
		UnitType unitType = unit.getType();
		if (unit.getPlayer() == GameHandler.getSelfPlayer()) {
			if (unitType.isRefinery()) {
				// TODO put just enough workers on gas immediately according to
				// strategy
				for (int i = 0; i < 2; i++) {
					BaseManager.getBuilder().ifPresent(
							w -> BaseManager.getResource(unit).ifPresent(
									r -> w.gather(r)));
				}
			}
		}
		return this;
	}
}
