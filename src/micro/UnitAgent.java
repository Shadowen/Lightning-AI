package micro;

import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Deque;

import bwapi.Position;
import bwapi.Unit;
import pathing.NoPathFoundException;

public abstract class UnitAgent {
	public Unit unit;
	protected Deque<Position> path;
	/** The destination of the current path */
	protected Position pathTarget;
	/** The destination building of the current path */
	protected Rectangle pathTargetBox;
	/** The length of the originally planned path */
	protected int pathOriginalSize = Integer.MAX_VALUE;
	/** Number of frames after which we try a better path */
	protected static final int PATHING_TIMEOUT_FRAMES = 250;
	/**
	 * Frame on which we started using this path.
	 */
	protected int pathStartFrame;
	protected UnitTask task;
	public Unit target;
	protected int timeout;

	public UnitAgent(Unit u) {
		unit = u;
		path = new ArrayDeque<>();
		task = UnitTask.IDLE;
		target = null;
		timeout = 0;
	}

	public abstract void findPath(Position toWhere, int length) throws NoPathFoundException;

	public abstract void findPath(Rectangle toWhere, int length) throws NoPathFoundException;

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

	public UnitTask getTask() {
		return task;
	}

	public void setTaskScout(Position target) {
		this.task = UnitTask.SCOUTING;
		pathTarget = target;
	}

	public void setTask(UnitTask task) {
		// TODO remove this, replace with specific functions
		this.task = task;
	}

	public abstract void act();

	public abstract void scout() throws NoPathFoundException;

	public String toString() {
		return unit.getType() + " @ (" + unit.getX() + ", " + unit.getY() + ")";
	}
}
