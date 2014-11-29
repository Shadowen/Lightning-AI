package eaglesWings.gamestructure;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eaglesWings.datastructure.BuildingPlan;
import eaglesWings.datastructure.Resource;
import eaglesWings.datastructure.Worker;
import javabot.BWAPIEventListener;
import javabot.JNIBWAPI;
import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;

public class GameHandler extends JNIBWAPI {
	private DebugEngine debugEngine;

	public GameHandler(BWAPIEventListener listener) {
		super(listener);
		debugEngine = new DebugEngine(this);
	}

	public Unit getClosestUnitOfType(int x, int y, UnitTypes type) {
		Unit closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Unit u : getNeutralUnits()) {
			if (u.getTypeID() == type.ordinal()) {
				double distance = Point.distance(x, y, u.getX(), u.getY());
				if (distance < closestDistance) {
					closestDistance = distance;
					closest = u;
				}
			}
		}
		return closest;
	}

	// TODO this doesn't need to be here.
	public Point getUnitVector(Point start, Point dest) {
		double distance = 1;
		return new Point((int) ((dest.x - start.x) / distance * 1000),
				(int) ((dest.y - start.y) / distance * 1000));
	}

	public double distance(Worker w, Resource r) {
		return Point.distance(w.getX(), w.getY(), r.getX(), r.getY());
	}

	// Provides a public method for build
	public void build(int id, BuildingPlan toBuild) {
		build(id, toBuild.getTx(), toBuild.getTy(), toBuild.getTypeID());
	}

	public void registerDebugFunction(DebugModule m) {
		debugEngine.registerDebugFunction(m);
	}

	public void drawDebug() {
		debugEngine.draw();
	}

	public Unit getClosestEnemy(int x, int y) {
		double closestDistance = Double.MAX_VALUE;
		Unit closestUnit = null;
		for (Unit u : getEnemyUnits()) {
			double distanceX = x - u.getX();
			double distanceY = y - u.getY();
			double distance = Math.sqrt(Math.pow(distanceX, 2)
					+ Math.pow(distanceY, 2));

			if (distance < closestDistance) {
				closestUnit = u;
				closestDistance = distance;
			}
		}
		if (closestUnit != null) {
			return closestUnit;
		}
		return null;
	}

	public Unit getClosestEnemy(Unit toWho) {
		return getClosestEnemy(toWho.getX(), toWho.getY());
	}
}
