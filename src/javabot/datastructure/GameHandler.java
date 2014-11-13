package javabot.datastructure;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javabot.BWAPIEventListener;
import javabot.JNIBWAPI;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class GameHandler extends JNIBWAPI {

	public GameHandler(BWAPIEventListener listener) {
		super(listener);
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

	public int getClosestEnemy(Unit toWho) {
		double closestDistance = Double.MAX_VALUE;
		Unit closestUnit = null;
		for (Unit u : getEnemyUnits()) {
			double distanceX = toWho.getX() - u.getX();
			double distanceY = toWho.getY() - u.getY();
			double distance = Math.sqrt(Math.pow(distanceX, 2)
					+ Math.pow(distanceY, 2));

			if (distance < closestDistance) {
				closestUnit = u;
				closestDistance = distance;
			}
		}
		if (closestUnit != null) {
			return closestUnit.getID();
		}
		return -1;
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
}
