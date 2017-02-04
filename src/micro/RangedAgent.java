package micro;

import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import pathing.NoPathFoundException;

public class RangedAgent extends GroundAgent {

	public RangedAgent(Unit u) {
		super(u);
	}

	@Override
	public void act() {
		final Position predictedPosition = new Position((int) (unit.getX() + unit.getVelocityX()),
				(int) (unit.getY() + unit.getVelocityY()));

		switch (task) {
		case IDLE:
			if (GameHandler.getUnitsInRadius(unit.getPosition(), 1000).stream()
					.anyMatch(u -> u.getPlayer().isEnemy(GameHandler.getSelfPlayer()))) {
				setTaskAttackRun();
			}
			break;
		case SCOUTING:
			// Scout the base...
			scout();
			target = GameHandler.getEnemyUnits().stream()
					.sorted((u1,
							u2) -> (int) (u1.getPosition().getDistance(unit.getPosition())
									- u2.getPosition().getDistance(unit.getPosition())) * 1000)
					.findFirst().orElse(null);
			// Switch to aggressive if enemy is nearby
			if (target != null && unit.getPosition().getDistance(target) < 100) {
				task = UnitTask.ATTACK_RUN;
			}
			break;
		case ATTACK_RUN:
			target = GameHandler.getEnemyUnits().stream()
					.sorted((u1,
							u2) -> (int) (u1.getPosition().getDistance(unit.getPosition())
									- u2.getPosition().getDistance(unit.getPosition())) * 1000)
					.findFirst().orElse(null);
			if (target == null) {
				task = UnitTask.SCOUTING;
				break;
			} else {
				final int unitSize = Math.min(unit.getType().width(), unit.getType().height());
				final int range = unit.getType().groundWeapon().maxRange();
				final int enemySize = Math.max(target.getType().width(), target.getType().height());
				final Vector fv = Vector.fromAngle(unit.getAngle());
				final Vector av = new Vector(unit.getPosition(), target.getPosition()).normalize();
				// Firing angle of 2.5 rad seems to work for vultures
				if (predictedPosition.getDistance(target.getPosition()) <= range + enemySize
						&& Vector.angleBetween(fv, av) < 2.5) {
					// TODO remember to check weapon cooldown here too! may
					// need to switch back to retreating state?
					unit.attack(target);
					timeout = 3;
					task = UnitTask.MOVE;
				} else {
					try {
						findPath(target.getPosition(), 64);
						followPath();
					} catch (NoPathFoundException e) {
						System.err.println("No path found for escape!");
					}
					break;
				}
			}
			break;
		case MOVE:
			target = GameHandler.getUnitsInRadius(unit.getPosition(), 300).stream()
					.filter(u -> u.getPlayer().isEnemy(GameHandler.getSelfPlayer()))
					.sorted((u1,
							u2) -> (int) (u1.getPosition().getDistance(unit.getPosition())
									- u2.getPosition().getDistance(unit.getPosition())) * 1000)
					.findFirst().orElse(null);
			if (target != null) {
				final int dx = unit.getX() - target.getX();
				final int dy = unit.getY() - target.getY();
				final Vector delta = new Vector(dx, dy).normalize().scalarMultiply(50);
				unit.move(new Position(unit.getX() + delta.getXInt(), unit.getY() + delta.getYInt()).makeValid());
			}
			timeout--;
			// Go safe when threshold is reached
			if (timeout <= 0 && unit.getGroundWeaponCooldown() <= 1) {
				task = UnitTask.ATTACK_RUN;
			}
			break;
		default:
			break;
		}
	}
}
