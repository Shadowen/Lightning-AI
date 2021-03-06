package state;

import base.BaseManager;
import base.GasResource;
import base.Worker;
import build.BuildManager;
import bwapi.Unit;
import bwapi.UnitType;
import gamestructure.GameHandler;
import micro.MicroManager;
import micro.UnitAgent;
import micro.UnitTask;

public class StarportRush extends BotState {
	private int previousSupply = 0;
	private Worker gasStealer;

	protected StarportRush(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState onFrame() {
		// Anti-zerg rush
		if (BaseManager.main != null
				&& GameHandler.getUnitsInRadius(BaseManager.main.getLocation().getPosition(), 1000).stream()
						.filter(u -> u.getType() == UnitType.Zerg_Zergling).count() > 0
				&& MicroManager.getUnitsByType(UnitType.Terran_Vulture).size() > 2
				&& MicroManager.getUnitsByType(UnitType.Terran_Marine).size() > 5) {
			System.out.println("Zerg rush detected");
			MicroManager.getUnitsByType(UnitType.Terran_SCV).stream().forEach(u -> {
				// u.setTaskDefending(UnitTask.DEFENDING);
			});
		}

		// Expand
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
			} else if (previousSupply < 10 && supply >= 10) {
				gasStealer = BaseManager.getFreeWorker().get();
				try {
					gasStealer.setTaskScout(BaseManager.getEnemyBases().stream().findFirst().get().gas.stream()
							.findFirst().get().getUnit().getInitialPosition());
				} catch (Exception e) {
					System.err.println("Failed to steal gas.");
					gasStealer.setTaskScout();
				}
			} else if (previousSupply < 9 && supply >= 9) {
				BuildManager.setMinimum(UnitType.Terran_Supply_Depot, 1);
			}
		}
		previousSupply = supply;

		// Gas steal!
		BaseManager.getEnemyBases().stream().findFirst().ifPresent(
				base -> base.gas.stream().filter(gas -> gas.getUnit().isVisible()).findFirst().ifPresent(gas -> {
					if (gasStealer != null && gasStealer.getTask() == UnitTask.SCOUTING) {
						gasStealer.setTaskGasFreeze(gas);
					}
				}));

		return this;

	}

	@Override
	public BotState unitConstructed(Unit unit) {
		UnitType unitType = unit.getType();
		if (unit.getPlayer() == GameHandler.getSelfPlayer()) {
			if (unitType == UnitType.Terran_Vulture) {
				UnitAgent a = MicroManager.getAgentForUnit(unit);
				a.setTaskScout();
			} else if (unitType == UnitType.Terran_Wraith) {
				MicroManager.getAgentForUnit(unit).setTaskScout();
			}
		}
		return this;
	}
}
