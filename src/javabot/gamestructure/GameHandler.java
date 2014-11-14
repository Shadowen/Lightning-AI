package javabot.gamestructure;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javabot.BWAPIEventListener;
import javabot.JNIBWAPI;
import javabot.datastructure.BuildingPlan;
import javabot.datastructure.Resource;
import javabot.datastructure.Worker;
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

	// Finds a nearby valid build location
	public Point getBuildLocation(int x, int y, UnitTypes toBuild) {
		// Returns the Point object representing the suitable build tile
		// position
		// for a given building type near specified pixel position (or
		// Point(-1,-1) if not found)
		Point ret = new Point(-1, -1);
		int maxDist = 3;
		int stopDist = 40;
		int tileX = x / 32;
		int tileY = y / 32;

		while ((maxDist < stopDist) && (ret.x == -1)) {
			for (int i = tileX - maxDist; i <= tileX + maxDist; i++) {
				for (int j = tileY - maxDist; j <= tileY + maxDist; j++) {
					if (canBuildHere(i, j, toBuild)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : getAllUnits()) {
							if ((Math.abs(u.getTileX() - i) < 4)
									&& (Math.abs(u.getTileY() - j) < 4)) {
								unitsInWay = true;
							}
						}
						if (!unitsInWay) {
							ret.x = i;
							ret.y = j;
							return ret;
						}
					}
				}
			}
			maxDist += 2;
		}

		if (ret.x == -1) {
			throw new NullPointerException();
		}

		return ret;
	}

	private boolean canBuildHere(int left, int top, UnitTypes typeEnum) {
		UnitType type = getUnitType(typeEnum.ordinal());
		int width = type.getTileWidth();
		int height = type.getTileHeight();

		for (int i = left; i < left + width; i++) {
			for (int j = top; j < top + height; j++) {
				if (!(getMap().isBuildable(i, j))) {
					return false;
				}
			}
		}
		return true;
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
}
