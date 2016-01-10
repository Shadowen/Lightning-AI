package state;

import java.util.Optional;

import base.BaseManager;
import base.Worker;
import build.BuildManager;
import bwapi.Unit;
import bwapi.UnitType;
import gamestructure.GameHandler;
import micro.UnitTask;

public class StarportRush extends BotState {

	int previousSupply = 0;

	protected StarportRush(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState onFrame() {
		if (GameHandler.getSelfPlayer().minerals() >= 400 && !BuildManager.isInQueue(UnitType.Terran_Command_Center)) {
			BaseManager.expand();
		}
		// Check the build order
		int supply = GameHandler.getSelfPlayer().supplyUsed() / 2;
		if (supply > 30) {
			if (GameHandler.getSelfPlayer().supplyTotal() - GameHandler.getSelfPlayer().supplyUsed() < 4) {
				if (!BuildManager.isInQueue(UnitType.Terran_Supply_Depot)) {
					BuildManager.addToQueue(UnitType.Terran_Supply_Depot);
				}
			}
		} else {
			if (previousSupply < 22 && supply >= 22) {
				BuildManager.setMinimum(UnitType.Terran_Starport, 2);
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 3);
				BuildManager.setMinimum(UnitType.Terran_Wraith, 24);
			} else if (previousSupply < 16 && supply >= 16) {
				BuildManager.setMinimum(UnitType.Terran_Factory, 1);
				BuildManager.addToQueue(UnitType.Terran_Vulture, 2);
			} else if (previousSupply < 14 && supply >= 14) {
				BuildManager.setMinimum(UnitType.Terran_Marine, 2);
			} else if (previousSupply < 13 && supply >= 13) {
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 2);
			} else if (previousSupply < 12 && supply >= 12) {
				BuildManager.setMinimum(UnitType.Terran_Refinery, 1);
			} else if (previousSupply < 11 && supply >= 11) {
				BuildManager.setMinimum(UnitType.Terran_Barracks, 1);
				Optional<Worker> w = BaseManager.getFreeWorker();
				if (!w.isPresent()) {
					GameHandler.sendText("Can't scout since no workers available!");
				} else {
					w.get().setTask(UnitTask.SCOUTING);
				}
			} else if (previousSupply < 9 && supply >= 9) {
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 1);
			}
		}
		previousSupply = supply;

		return this;

	}

	@Override
	public BotState unitConstructed(Unit unit) {
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
