package micro;

import bwapi.Position;
import gamestructure.GameHandler;

public class WraithGroup extends UnitGroup {
	public void act() {
		double cx = 0;
		double cy = 0;
		for (UnitAgent ua : unitAgents) {
			cx += ua.unit.getX();
			cy += ua.unit.getY();
		}
		final Position centerPosition = new Position((int) (cx / unitAgents.size()), (int) (cy / unitAgents.size()));

		switch (task) {
		case IDLE:
			if (unitAgents.size() > 2) {
				task = UnitTask.SCOUTING;
			}
			break;
		case SCOUTING:
			for (UnitAgent ua : unitAgents) {
				ua.setTaskScouting();
			}
			if (unitAgents.size() > 5) {
				task = UnitTask.ATTACK_RUN;
			}
			break;
		case ATTACK_RUN:
			// Prioritize units that can attack
			GameHandler.getEnemyUnits()
					.stream().filter(
							e -> e.getType().canAttack())
					.sorted((u1, u2) -> (int) (u1.getPosition().getDistance(centerPosition)
							- u2.getPosition().getDistance(centerPosition)) * 1000)
					.findFirst().ifPresent(e -> {
						target = e;
					});
			// Otherwise target anything
			if (target == null) {
				GameHandler.getEnemyUnits().stream().filter(e -> !e.getType().canAttack())
						.sorted((u1,
								u2) -> (int) (u1.getPosition().getDistance(centerPosition)
										- u2.getPosition().getDistance(centerPosition)) * 1000)
						.findFirst().ifPresent(e -> {
							target = e;
						});
			}
			boolean cycleComplete = true;
			for (UnitAgent ua : unitAgents) {
				ua.attackCycle(target);
				if (ua.task != UnitTask.RETREATING || ua.unit.getGroundWeaponCooldown() > 0) {
					cycleComplete = false;
				}
			}
			if (cycleComplete) {
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
}
