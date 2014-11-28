package javabot.datastructure;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Queue;

import javabot.gamestructure.DebugEngine;
import javabot.gamestructure.DebugModule;
import javabot.gamestructure.Debuggable;
import javabot.gamestructure.GameHandler;
import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class BuildManager implements Debuggable {
	private GameHandler game;
	private BaseManager baseManager;

	public Queue<BuildingPlan> buildingQueue;
	private Queue<UnitTypes> unitQueue;

	public BuildManager(GameHandler igame, BaseManager ibm) {
		game = igame;
		baseManager = ibm;
		buildingQueue = new ArrayDeque<BuildingPlan>();
		unitQueue = new ArrayDeque<UnitTypes>();
	}

	public void addToQueue(UnitTypes unitType) {
		if (game.getUnitType(unitType.ordinal()).isBuilding()) {
			Point location = getBuildLocation(
					baseManager.getMain().location.getX(),
					baseManager.getMain().location.getY(), unitType);
			addBuilding(location.x, location.y, unitType);
		} else {
			unitQueue.add(unitType);
		}
	}

	public void addBuilding(Point buildLocation, UnitTypes type) {
		addBuilding(buildLocation.x, buildLocation.y, type);
	}

	public void addBuilding(int tx, int ty, UnitTypes type) {
		buildingQueue.add(new BuildingPlan(game, tx, ty, type));
	}

	// Finds a nearby valid build location
	private Point getBuildLocation(int x, int y, UnitTypes toBuild) {
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
						for (Unit u : game.getAllUnits()) {
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
		UnitType type = game.getUnitType(typeEnum.ordinal());
		int width = type.getTileWidth();
		int height = type.getTileHeight();

		// Check if location is buildable
		for (int i = left; i < left + width; i++) {
			for (int j = top; j < top + height; j++) {
				if (!(game.isBuildable(i, j, true))) {
					return false;
				}
			}
		}
		// Check if another building is planned for this spot
		for (BuildingPlan bp : buildingQueue) {
			if (bp.getTx() <= left + width
					&& bp.getTx() + bp.getWidth() >= left) {
				if (bp.getTy() <= top + height
						&& bp.getTy() + bp.getHeight() >= top) {
					return false;
				}
			}
		}
		return true;
	}

	// Call this whenever a unit completes contruction
	public void doneBuilding(Unit u) {
		if (game.getUnitType(u.getTypeID()).isBuilding()) {
			// Go through planned buildings
			for (BuildingPlan p : buildingQueue) {
				// If it's the right building according to the plan
				if (u.getTypeID() == p.getTypeID() && u.getTileX() == p.getTx()
						&& u.getTileY() == p.getTy()) {
					// It has been completed
					buildingQueue.remove(p);
					break;
				}
			}
		} else {
			// Go through planned units

		}
	}

	public UnitTypes getToTrain() {
		return unitQueue.peek();
	}

	public boolean buildQueueContains(UnitTypes unitType) {
		for (BuildingPlan plan : buildingQueue) {
			if (plan.getTypeID() == unitType.ordinal()) {
				return true;
			}
		}
		return false;
	}

	public void removeUnitFromQueue(UnitTypes toTrain) {
		unitQueue.remove(toTrain);
	}

	@Override
	public void registerDebugFunctions(GameHandler g) {
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				String buildQueueString = "";
				for (BuildingPlan plan : buildingQueue) {
					int x = plan.getTx() * 32;
					int y = plan.getTy() * 32;
					int width = game.getUnitType(plan.getTypeID())
							.getTileWidth() * 32;
					int height = game.getUnitType(plan.getTypeID())
							.getTileHeight() * 32;
					engine.drawBox(x, y, x + width, y + height, BWColor.GREEN,
							false, false);
					if (plan.builder != null) {
						int bx = plan.builder.getX();
						int by = plan.builder.getY();
						engine.drawLine(bx, by, x + width / 2, y + width / 2,
								BWColor.GREEN, false);
					}

					buildQueueString += plan.getTypeName() + ", ";
				}
				engine.drawText(5, 20, "Building Queue: " + buildQueueString,
						true);
				String trainingQueueString = "";
				for (Unit u : unitQueue.toArray(new Unit[0])) {
					trainingQueueString += game.getUnitType(u.getTypeID())
							.toString() + ", ";
				}
				engine.drawText(5, 40,
						"Training Queue: " + trainingQueueString, true);
			}
		});
	}
}
