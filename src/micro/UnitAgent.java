package micro;

import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Deque;

import base.BaseManager;
import bwapi.Position;
import bwapi.Unit;
import bwta.BWTA;
import gamestructure.GameHandler;
import pathing.InvalidStartNodeException;
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

	public abstract Deque<Position> findPathAwayFrom(Position fromWhere, int length)
			throws InvalidStartNodeException, NoPathFoundException;

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

	protected void beforeTaskChange() {
		path.clear();
		pathTarget = null;
		pathTargetBox = null;
		pathOriginalSize = Integer.MAX_VALUE;
		target = null;
	}

	public void setTaskIdle() {
		beforeTaskChange();
		task = UnitTask.IDLE;
	}

	public void setTaskMove(Position toWhere) {
		beforeTaskChange();

		task = UnitTask.MOVE;
		pathTarget = toWhere;
		if (!pathTarget.isValid()) {
			System.err.println("UnitAgent tried to move to invalid position!");
		}
	}

	public void setTaskScout() {
		beforeTaskChange();

		task = UnitTask.SCOUTING;
	}

	public void setTaskScout(Position target) {
		beforeTaskChange();

		this.task = UnitTask.SCOUTING;
		pathTarget = target;
		if (!pathTarget.isValid()) {
			System.err.println("UnitAgent tried to scout to invalid position!");
		}
	}

	public UnitTask getTask() {
		return task;
	}

	protected void act() {
		switch (task) {
		case SCOUTING:
			scout();
			break;
		case MOVE:
			unit.move(pathTarget);
			break;
		default:
			break;
		}
	}

	protected void scout() {
		// Acquire target
		if (pathTarget == null
				|| GameHandler.isVisible(new Position(pathTarget.getX(), pathTarget.getY()).toTilePosition())) {
			System.out.println("Acquiring new scouting target");
			pathTarget = MicroManager.getScoutingTarget(unit);
		}
		// Find a path to there
		if (pathTarget != null) {
			try {
				findPath(pathTarget, 256);
				followPath();
			} catch (NoPathFoundException e) {
				System.err.println("No path to scout " + unit.getType());
				setTaskIdle();
			}
		}

		// Give up on scouting because there's nothing left to see
		if (pathTarget == null) {
			System.out.println("Gave up on scouting.");
			setTaskIdle();
		}
	}

	public String toString() {
		return unit.getType() + " @ (" + unit.getX() + ", " + unit.getY() + ")";
	}
}
