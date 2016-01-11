package micro;

import java.util.ArrayDeque;
import java.util.Deque;
import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import pathing.InvalidStartNodeException;
import pathing.NoPathFoundException;
import pathing.PathFinder;

public class UnitAgent {
	public Unit unit;
	protected Deque<Position> path;
	/** The destination of the current path */
	protected Position pathTarget;
	/** The length of the originally planned path */
	protected int pathOriginalSize;
	/** Number of frames after which we try a better path */
	protected static final int PATHING_TIMEOUT_FRAMES = 250;
	/**
	 * Frame on which we started using this path.
	 */
	protected int pathStartFrame;
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
		if (pathTarget != null && pathTarget.equals(toWhere)
				&& (path.size() >= 1.0 / 3 * length || pathOriginalSize <= 1.0 / 3 * length)) {
			return;
		}
		// Every 500 frames make the pathfinder work harder
		length *= ((GameHandler.getFrameCount() - pathStartFrame) / PATHING_TIMEOUT_FRAMES + 1);
		// Otherwise make a new path
		try {
			path = PathFinder.findGroundPath(unit, toWhere, length);
			pathTarget = toWhere;
			pathOriginalSize = path.size();
			pathStartFrame = GameHandler.getFrameCount() + 500;
		} catch (InvalidStartNodeException e) {
			e.printStackTrace();
		}
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
