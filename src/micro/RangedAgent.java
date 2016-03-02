package micro;

import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import pathing.InvalidStartNodeException;
import pathing.NoPathFoundException;

public class RangedAgent extends GroundAgent {

	public RangedAgent(Unit u) {
		super(u);
	}

	@Override
	public void attackCycle(Unit target) {
		// TODO Auto-generated method stub
	}

	@Override
	public void act() {
		final Position predictedPosition = new Position((int) (unit.getX() + unit.getVelocityX()),
				(int) (unit.getY() + unit.getVelocityY()));

		switch (task) {
		case IDLE:
			task = UnitTask.SCOUTING;
			break;
		case SCOUTING:
			// Scout the base...
			scout();
			target = GameHandler.getEnemyUnits().stream()
					.sorted((u1,
							u2) -> (int) (u1.getPosition().getDistance(unit.getPosition())
									- u2.getPosition().getDistance(unit.getPosition())) * 1000)
					.findFirst().orElse(null);
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
					task = UnitTask.FIRING;
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
		case FIRING:
			unit.attack(target);
			// System.out.println(GameHandler.getFrameCount() + " :
			// attack");
			// System.out.println(GameHandler.getFrameCount() + ": Range=" +
			// u.getType().groundWeapon().maxRange());
			// System.out.println(
			// GameHandler.getFrameCount() + ": mySize=" +
			// u.getType().width() + ", " + u.getType().height());
			// System.out.println(GameHandler.getFrameCount() + ":
			// enemySize=" + target.getType().width() + ", "
			// + target.getType().height());
			// System.out.println(
			// GameHandler.getFrameCount() + ": real Distance=" +
			// u.getDistance(target.getPosition()));
			// System.out.println(GameHandler.getFrameCount() + ": predicted
			// Distance="
			// + predictedPosition.getDistance(target.getPosition()));
			// final Vector fv = Vector.fromAngle(unit.getAngle());
			// final Vector av = new Vector(unit.getPosition(),
			// target.getPosition()).normalize();
			// System.out.println(GameHandler.getFrameCount() + ": angle ("
			// + Vector.angleBetween(fv, av) + ")");
			task = UnitTask.RETREATING;
			timeout = 3;
			break;
		case RETREATING:
			target = GameHandler.getEnemyUnits().stream()
					.sorted((u1,
							u2) -> (int) (u1.getPosition().getDistance(unit.getPosition())
									- u2.getPosition().getDistance(unit.getPosition())) * 1000)
					.findFirst().orElse(null);
			// System.out.println(
			// GameHandler.getFrameCount() + ": Retreating (" +
			// unit.getGroundWeaponCooldown() + ")");
			// final int dx = (unit.getX() - target.getX());
			// final int dy = (unit.getY() - target.getY());
			// final Position destination = new Position(unit.getX() + dx,
			// unit.getY() + dy);
			// unit.move(destination);
			try {
				path = findPathAwayFrom(target.getPosition(), 10);
				followPath();
			} catch (NoPathFoundException e1) {
				System.err.println("No path found for RangedAgent!");
				e1.printStackTrace();
			} catch (InvalidStartNodeException e) {
				System.err.println("Invalid start node for RangedAgent.");
				e.printStackTrace();
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
