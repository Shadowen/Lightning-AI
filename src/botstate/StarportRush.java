package botstate;

import gamestructure.GameHandler;

import java.util.Optional;

import micromanager.MicroManager;
import bwapi.Unit;
import bwapi.UnitType;
import datastructure.BaseManager;
import datastructure.BuildManager;
import datastructure.Worker;
import datastructure.WorkerTask;

public class StarportRush extends BotState {

	int previousSupply = 0;

	protected StarportRush(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		// Check the build order
		int supply = GameHandler.getSelfPlayer().supplyUsed() / 2;
		if (supply > 30) {
			if (GameHandler.getSelfPlayer().supplyTotal() - GameHandler.getSelfPlayer().supplyUsed() < 4) {
				if (!BuildManager.isInQueue(UnitType.Terran_Supply_Depot)) {
					BuildManager.addToQueue(UnitType.Terran_Supply_Depot);
				}
			}
		} else if (previousSupply < supply) {
			if (supply > 22) {
				BuildManager.setMinimum(UnitType.Terran_Starport, 2);
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 3);
				BuildManager.setMinimum(UnitType.Terran_Wraith, 24);
			} else if (supply > 16) {
				BuildManager.setMinimum(UnitType.Terran_Factory, 1);
				BuildManager.addToQueue(UnitType.Terran_Vulture, 2);
			} else if (supply > 14) {
				BuildManager.setMinimum(UnitType.Terran_Marine, 2);
				BaseManager.getFreeWorker().ifPresent(w -> MicroManager.setScoutingUnit(w.getUnit()));
			} else if (supply > 13) {
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 2);
			} else if (supply > 12) {
				BuildManager.setMinimum(UnitType.Terran_Refinery, 1);
			} else if (supply > 11) {
				BuildManager.setMinimum(UnitType.Terran_Barracks, 1);
				if (!MicroManager.isScouting()) {
					Optional<Worker> w = BaseManager.getFreeWorker();
					if (!w.isPresent()) {
						GameHandler.sendText("Can't scout since no workers available!");
					} else {
						w.get().setTask(WorkerTask.SCOUTING);
						MicroManager.setScoutingUnit(w.get().getUnit());
					}
				}
			} else if (supply > 9) {
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 1);
			}
		}
		previousSupply = supply;

		return this;
	}

	@Override
	public BotState unitComplete(Unit unit) {
		UnitType unitType = unit.getType();
		if (unit.getPlayer() == GameHandler.getSelfPlayer()) {
			if (unitType.isRefinery()) {
				for (int i = 0; i < 3; i++) {
					BaseManager.getFreeWorker()
							.ifPresent(w -> BaseManager.getResource(unit).ifPresent(r -> w.gather(r)));
				}
			}
		}
		return this;
	}
}
