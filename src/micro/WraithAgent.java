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

	@Override
	public void act() {
		timeout--;
		switch (task) {
		case IDLE:
			task = UnitTask.SCOUTING;
			break;
		case SCOUTING:
			// Scout the base...
			scout();
			break;
		case ATTACK_RUN:
			if (target != null) {
				final Position predictedPosition = new Position((int) (unit.getX() + unit.getVelocityX()),
						(int) (unit.getY() + unit.getVelocityY()));

				unit.move(target.getPosition());
				final int unitSize = Math.min(unit.getType().width(), unit.getType().height());
				final int range = unit.getType().groundWeapon().maxRange();
				final int enemySize = Math.min(target.getType().width(), target.getType().height());
				// Fire
				// 205 distance seems good for Wraith
				if (predictedPosition.getDistance(target.getPosition()) <= unitSize + range + enemySize / 2) {
					unit.attack(new PositionOrUnit(target));
					timeout = 3;
				}
			}
			break;
		case MOVE:
			unit.move(pathTarget);
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
	public Deque<Position> findPathAwayFrom(Position fromWhere, int length) {
		return null;
	}

}
