package micro;

import bwapi.Position;
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
			// Prioritize units that can attack
			target = null;
			GameHandler.getEnemyUnits().stream().filter(e -> e.getType().canAttack() && e.isVisible())
					.sorted((u1,
							u2) -> (int) ((u1.getPosition().getDistance(centerPosition)
									- u2.getPosition().getDistance(centerPosition)) * 1000))
					.findFirst().ifPresent(e -> {
						target = e;
					});
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
				}
			}
			if (cycleComplete) {
				// Regather
				if (getPercentileDistance(0.2) > Math.sqrt(unitAgents.size()) * 20) {
					Position center = getCenterPosition();
					for (UnitAgent ua : unitAgents) {
						if (ua.unit.getPosition().getDistance(center) > Math.sqrt(unitAgents.size()) * 20) {
							ua.setTaskMove(centerPosition);
						}
					}
				} else {
					// Next cycle
					for (UnitAgent ua : unitAgents) {
						ua.task = UnitTask.ATTACK_RUN;
					}
				}
			}
			break;
		default:
			System.err.println("Invalid WraithAgent state");
			break;

		}
	}
}
