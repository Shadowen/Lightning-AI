package micro;

import java.awt.Rectangle;

import base.Base;
import base.BaseManager;
import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import pathing.InvalidStartNodeException;
import pathing.NoPathFoundException;
import pathing.PathFinder;

public abstract class GroundAgent extends UnitAgent {
	public GroundAgent(Unit u) {
		super(u);
	}

	public void findPath(Position toWhere, int length) throws NoPathFoundException {
		pathTargetBox = null;
		// If we already have a decent path
		if (pathTarget != null && pathTarget.equals(toWhere)
				&& (path.size() >= 1.0 / 3 * length || pathOriginalSize <= 1.0 / 3 * length)) {
			return;
		}
		// Every 500 frames make the pathfinder work harder
		length *= ((GameHandler.getFrameCount() - pathStartFrame) / PATHING_TIMEOUT_FRAMES + 1);
		// Otherwise make a new path
		try {
			// TODO this still needs work
			path = PathFinder.findGroundPath(unit, toWhere, length);
			pathTarget = toWhere;
			pathOriginalSize = path.size();
			pathStartFrame = GameHandler.getFrameCount();
		} catch (InvalidStartNodeException e) {
			e.printStackTrace();
		}
	}

	public void findPath(Rectangle toWhere, int length) throws NoPathFoundException {
		pathTarget = null;
		// If we already have a decent path
		if (pathTargetBox != null && pathTargetBox.equals(toWhere)
				&& (path.size() >= 1.0 / 3 * length || pathOriginalSize <= 1.0 / 3 * length)) {
			return;
		}
		// Every 500 frames make the pathfinder work harder
		length *= ((GameHandler.getFrameCount() - pathStartFrame) / PATHING_TIMEOUT_FRAMES + 1);
		// Otherwise make a new path
		try {
			path = PathFinder.findGroundPath(unit, toWhere, length);
			pathTargetBox = toWhere;
			pathOriginalSize = path.size();
			pathStartFrame = GameHandler.getFrameCount();
		} catch (InvalidStartNodeException e) {
			e.printStackTrace();
		}
	}

	public void scout() throws NoPathFoundException {
		// Acquire target
		if (pathTarget == null || (GameHandler.isVisible(pathTarget.getX() / 32, pathTarget.getY() / 32)
				&& BaseManager.getClosestBase(pathTarget).get().getPlayer().isNeutral())) {
			System.out.println("Acquiring new scouting target");
			Base targetBase = null;
			for (Base b : BaseManager.getBases()) {
				if (b.getLocation().isStartLocation()
						&& (b.getPlayer() == GameHandler.getNeutralPlayer() || b.getLastScouted() <= 100)
						&& (targetBase == null || b.getLastScouted() < targetBase.getLastScouted())) {
					targetBase = b;
				}
			}
			if (targetBase == null) {
				for (Base b : BaseManager.getBases()) {
					if (targetBase == null || b.getLastScouted() < targetBase.getLastScouted()) {
						targetBase = b;
					}
				}
			}
			if (targetBase != null) {
				pathTarget = targetBase.getLocation().getPosition();
			}
		}
		// Path planned is short
		if (pathTarget != null) {
			findPath(pathTarget, 256);
			followPath();
		} else {
			System.err.println("Attempted to scout with no target");
		}
	}
}
