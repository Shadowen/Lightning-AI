package micro;

import java.util.ArrayDeque;
import java.util.Deque;
import bwapi.Position;
import bwapi.Unit;
import pathing.NoPathFoundException;
import pathing.PathFinder;

public class UnitAgent {
	public Unit unit;
	protected Deque<Position> path;
	/** The destination of the current path */
	protected Position pathTarget;
	public UnitTask task;
	public Unit target;
	public int timeout;

	public UnitAgent(Unit u) {
		unit = u;
		path = new ArrayDeque<>();
		task = UnitTask.IDLE;
		target = null;
		timeout = 0;
	}

	public void findPath(Position toWhere, int length) throws NoPathFoundException {
		// If we already have a decent path
		if (pathTarget != null && pathTarget.equals(toWhere) && path.size() >= 1D / 3 * length) {
			return;
		}
		// Otherwise make a new path
		pathTarget = toWhere;
		path = PathFinder.findGroundPath(unit, toWhere, length);
	}

	public void followPath() {
		Position moveTarget = null;
		double distanceToCheckPoint;
		while (!path.isEmpty()) {
			moveTarget = path.element();
			distanceToCheckPoint = unit.getPosition()
					.getApproxDistance(new Position(moveTarget.getX(), moveTarget.getY()));

			if (distanceToCheckPoint > 64) {
				// Keep following existing path
				break;
			} else {
				// Checkpoint
				path.remove();
			}
		}

		// Issue a movement command
		if (moveTarget != null) {
			unit.move(moveTarget);
		}
	}

	public Deque<Position> getPath() {
		return path;
	}

	public String toString() {
		return unit.getType() + " @ (" + unit.getX() + ", " + unit.getY() + ")";
	}
}
