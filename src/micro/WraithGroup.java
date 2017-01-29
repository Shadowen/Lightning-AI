package micro;

import bwapi.Position;
import bwapi.UnitType;
import bwapi.WeaponType;
import gamestructure.GameHandler;

public class WraithGroup extends UnitGroup {
	public void act() {
		final Position centerPosition = getCenterPosition();

		switch (task) {
		case IDLE:
			task = UnitTask.SCOUTING;
			break;
		case SCOUTING:
			if (unitAgents.size() >= 2) {
				// Go aggressive
				task = UnitTask.ATTACK_RUN;
			} else {
				for (UnitAgent ua : unitAgents) {
					ua.setTaskScout();
				}
			}
			break;
		case ATTACK_RUN:
			// Prioritize units that can attack air
			target = null;
			GameHandler.getEnemyUnits().stream().filter(e -> e.getType().airWeapon() != null && e.isVisible())
					.sorted((u1,
							u2) -> (int) ((u1.getPosition().getDistance(centerPosition)
									- u2.getPosition().getDistance(centerPosition)) * 1000))
					.findFirst().ifPresent(e -> {
						target = e;
					});
			// Then workers
			if (target == null) {
				GameHandler.getEnemyUnits().stream().filter(e -> e.getType().isWorker())
						.sorted((u1,
								u2) -> (int) ((u1.getPosition().getDistance(centerPosition)
										- u2.getPosition().getDistance(centerPosition)) * 1000))
						.findFirst().ifPresent(e -> {
							target = e;
						});
			}
			// Otherwise target anything
			if (target == null) {
				GameHandler.getEnemyUnits().stream().filter(e -> e.isVisible())
						.sorted((u1,
								u2) -> (int) ((u1.getPosition().getDistance(centerPosition)
										- u2.getPosition().getDistance(centerPosition)) * 1000))
						.findFirst().ifPresent(e -> {
							target = e;
						});
			}
			if (target == null) {
				for (UnitAgent ua : unitAgents) {
					ua.setTaskScout();
				}
				return;
			}
			boolean cycleComplete = true;
			boolean canBeAttacked = false;
			for (UnitAgent ua : unitAgents) {
				ua.target = target;
				if (ua.getTask() == UnitTask.MOVE || ua.timeout > 0) {
					// Wraith has fired
					final int dx = ua.unit.getX() - target.getX();
					final int dy = ua.unit.getY() - target.getY();
					final Vector delta = new Vector(dx, dy).normalize().scalarMultiply(50);
					ua.setTaskMove(new Position(ua.unit.getX() + delta.getXInt(), ua.unit.getY() + delta.getYInt())
							.makeValid());
				}
				if (ua.timeout > 0
						|| Math.max(ua.unit.getGroundWeaponCooldown(), ua.unit.getAirWeaponCooldown()) > 10) {
					cycleComplete = false;
					canBeAttacked |= GameHandler.getUnitsInRadius(ua.unit.getPosition(), 300).stream()
							.filter(u -> u.getPlayer() != GameHandler.getSelfPlayer())
							.anyMatch(eu -> eu.getType().airWeapon() != WeaponType.None);
				}
			}

			if (cycleComplete && getPercentileDistance(0.2) > Math.sqrt(unitAgents.size()) * 20) {
				// Regather
				Position center = getCenterPosition();
				for (UnitAgent ua : unitAgents) {
					if (ua.unit.getPosition().getDistance(center) > Math.sqrt(unitAgents.size()) * 20) {
						ua.setTaskMove(centerPosition);
					}
				}
			} else if (cycleComplete || !canBeAttacked) {
				// Next cycle
				for (UnitAgent ua : unitAgents) {
					ua.task = UnitTask.ATTACK_RUN;
				}
			}

			break;
		default:
			System.err.println("Invalid WraithAgent state");
			break;

		}

	}

	public boolean tryAddUnitAgent(UnitAgent ua) {
		if (ua.unit.getType() == UnitType.Terran_Wraith && unitAgents.size() < 7) {
			unitAgents.add(ua);
			return true;
		}
		return false;
	}
}
