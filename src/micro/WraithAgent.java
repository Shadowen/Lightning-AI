package micro;

import java.awt.Rectangle;
import java.util.Deque;

import bwapi.Position;
import bwapi.PositionOrUnit;
import bwapi.Unit;
import gamestructure.GameHandler;
import pathing.NoPathFoundException;

public class WraithAgent extends UnitAgent {

	public WraithAgent(Unit unit) {
		super(unit);
	}

	public void attackCycle(Unit target) {
		this.target = target;
		final Position predictedPosition = new Position((int) (unit.getX() + unit.getVelocityX()),
				(int) (unit.getY() + unit.getVelocityY()));

		switch (task) {
		case ATTACK_RUN:
			unit.move(target.getPosition());
			final int unitSize = Math.min(unit.getType().width(), unit.getType().height());
			final int range = unit.getType().groundWeapon().maxRange();
			final int enemySize = Math.min(target.getType().width(), target.getType().height());
			// 205 distance seems good for Wraith
			if (predictedPosition.getDistance(target.getPosition()) <= unitSize + range + enemySize / 2) {
				task = UnitTask.FIRING;
			}
			break;
		case FIRING:
			unit.attack(new PositionOrUnit(target));
			task = UnitTask.RETREATING;
			timeout = 3;
			break;
		case RETREATING:
			final int dx = 10 * (unit.getX() - target.getX());
			final int dy = 10 * (unit.getY() - target.getY());
			unit.move(new Position(unit.getX() + dx, unit.getY() + dy));
			timeout--;
			break;
		default:
			task = UnitTask.ATTACK_RUN;
			attackCycle(target);
		}
	}

	@Override
	public void act() {
		switch (task) {
		case IDLE:
			task = UnitTask.SCOUTING;
			break;
		case SCOUTING:
			// Scout the base...
			scout();
			break;
		case ATTACK_RUN:
		case FIRING:
			attackCycle(target);
			break;
		case RETREATING:
			final int dx = 10 * (unit.getX() - target.getX());
			final int dy = 10 * (unit.getY() - target.getY());
			unit.move(new Position(unit.getX() + dx, unit.getY() + dy));
			timeout--;
			// Go safe when threshold is reached
			if (timeout <= 0
					&& unit.getGroundWeaponCooldown() <= unit.getType().groundWeapon().damageCooldown() * 1 / 3) {
				task = UnitTask.ATTACK_RUN;
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void findPath(Position toWhere, int length) throws NoPathFoundException {
		path.clear();
		path.add(toWhere);
	}

	@Override
	public void findPath(Rectangle toWhere, int length) throws NoPathFoundException {
		path.clear();
		path.addLast(new Position(toWhere.x, toWhere.y));
	}

	@Override
	public void scout() {
		// TODO Auto-generated method stub
	}

	@Override
	public Deque<Position> findPathAwayFrom(Position fromWhere, int length) {
		// TODO Auto-generated method stub
		return null;
	}

}
