package eaglesWings.gamestructure;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
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
	// A set containing the resource depot types
	public static final Set<UnitTypes> resourceDepotTypes;
	static {
		resourceDepotTypes = new HashSet<UnitTypes>();
		resourceDepotTypes.add(UnitTypes.Terran_Command_Center);
		resourceDepotTypes.add(UnitTypes.Protoss_Nexus);
		resourceDepotTypes.add(UnitTypes.Zerg_Hatchery);
		resourceDepotTypes.add(UnitTypes.Zerg_Lair);
		resourceDepotTypes.add(UnitTypes.Zerg_Hive);
	}

	private DebugEngine debugEngine;

	public GameHandler(BWAPIEventListener listener) {
		super(listener);
		debugEngine = new DebugEngine(this);
	}

	public Unit getClosestUnitOfType(int x, int y, UnitTypes type) {
		Unit closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Unit u : getAllUnits()) {
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

	public void registerDebugFunction(DebugModule m) {
		debugEngine.debugModules.add(m);
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
		return closestUnit;
	}

	public Unit getClosestEnemy(Unit toWho) {
		return getClosestEnemy(toWho.getX(), toWho.getY());
	}
}
