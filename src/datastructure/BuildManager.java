package datastructure;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Queue;

import bwapi.Color;
import bwapi.Unit;
import bwapi.UnitType;
import gamestructure.DebugEngine;
import gamestructure.DebugModule;
import gamestructure.Debuggable;
import gamestructure.GameHandler;
import gamestructure.ShapeOverflowException;

public class BuildManager implements Debuggable {
	private GameHandler game;
	private BaseManager baseManager;

	public Hashtable<UnitType, Integer> unitMinimums;

	public Queue<BuildingPlan> buildingQueue;
	public Queue<UnitType> unitQueue;

	public BuildManager(GameHandler igame, BaseManager ibm,
			DebugEngine debugEngine) {
		game = igame;
		baseManager = ibm;

		unitMinimums = new Hashtable<UnitType, Integer>();
		// TODO fix this
		/*
		 * for (UnitType type : UnitType.AllUnits) { unitMinimums.put(type, 0);
		 * }
		 */

		buildingQueue = new ArrayDeque<BuildingPlan>();
		unitQueue = new ArrayDeque<UnitType>();

		registerDebugFunctions(debugEngine);
	}

	public void addToQueue(UnitType unitType) {
		if (unitType == UnitType.Terran_Refinery) {
			// Refineries get special treatment!
			for (Base b : baseManager.getMyBases()) {
				for (GasResource r : b.gas) {
					if (!r.gasTaken()) {
						addBuilding(r.getX() / 32 - 2, r.getY() / 32 - 1,
								UnitType.Terran_Refinery);
						break;
					}
				}
			}
		} else if (unitType.isBuilding()) {
			// Otherwise, buildings
			if (baseManager.main != null) {
				Point location = getBuildLocation(baseManager.main.getX(),
						baseManager.main.getY(), unitType);
				addBuilding(location.x, location.y, unitType);
			}
		} else {
			// Finally, units
			unitQueue.add(unitType);
		}
	}

	// Add multiple units at once
	public void addToQueue(UnitType unitType, int count) {
		for (int i = 0; i < count; i++) {
			addToQueue(unitType);
		}
	}

	// Build a building at a specific location
	public void addBuilding(Point buildLocation, UnitType type) {
		addBuilding(buildLocation.x, buildLocation.y, type);
	}

	// Build a building at a specific location
	public void addBuilding(int tx, int ty, UnitType type) {
		buildingQueue.add(new BuildingPlan(game, tx, ty, type));
	}

	// Finds a nearby valid build location for the building of specified type
	// Returns the Point object representing the suitable build tile
	// position
	// for a given building type near specified pixel position (or
	// Point(-1,-1) if not found)
	private Point getBuildLocation(int x, int y, UnitType toBuild) {
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
							if ((Math.abs(u.getX() / 32 - i) < 4)
									&& (Math.abs(u.getY() / 32 - j) < 4)) {
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
			maxDist++;
		}

		if (ret.x == -1) {
			throw new NullPointerException();
		}

		return ret;
	}

	// Checks if the building type specified can be built at the coordinates
	// given
	private boolean canBuildHere(int left, int top, UnitType type) {
		int width = type.tileWidth();
		int height = type.tileHeight();

		// Check if location is buildable
		for (int i = left; i < left + width - 1; i++) {
			for (int j = top; j < top + height - 1; j++) {
				if (!(game.isBuildable(i, j, true))) {
					return false;
				}
			}
		}
		// Check if another building is planned for this spot
		for (BuildingPlan bp : buildingQueue) {
			if (bp.getTx() <= left + width
					&& bp.getTx() + bp.getType().tileWidth() >= left) {
				if (bp.getTy() <= top + height
						&& bp.getTy() + bp.getType().tileHeight() >= top) {
					return false;
				}
			}
		}
		return true;
	}

	// Call this whenever a unit completes construction
	public void doneBuilding(Unit u) {
		UnitType type = u.getType();
		// Go through planned units
		unitQueue.remove(type);
		// Go through planned buildings
		for (BuildingPlan p : buildingQueue) {
			// If it's the right building according to the plan
			if (u.getType() == p.getType() && u.getX() / 32 == p.getTx()
					&& u.getY() / 32 == p.getTy()) {
				// It has been completed
				buildingQueue.remove(p);

				if (u.getType() == UnitType.Terran_Refinery) {
					// If it's a refinery, the worker will automatically become
					// a gas miner!
					p.builder.gather(baseManager.getResource(u));
				} else {
					// Otherwise, back to work!
					p.builder.gather(p.builder.getCurrentResource());
				}

				break;
			}
		}
	}

	public boolean isInQueue(UnitType unitType) {
		for (BuildingPlan plan : buildingQueue) {
			if (plan.getType() == unitType) {
				return true;
			}
		}
		for (UnitType unitInQueue : unitQueue) {
			if (unitInQueue == unitType) {
				return true;
			}
		}
		return false;
	}

	public int getCountInQueue(UnitType unitType) {
		int count = 0;
		for (BuildingPlan plan : buildingQueue) {
			if (plan.getType() == unitType) {
				count++;
			}
		}
		for (UnitType type : unitQueue) {
			if (type == unitType) {
				count++;
			}
		}
		return count;
	}

	public int getMyUnitCount(UnitType type) {
		int count = 0;
		for (Unit u : game.getAllUnits()) {
			if (!u.isBeingConstructed() && u.getType() == type) {
				count++;
			}
		}
		return count;
	}

	public void setMinimum(UnitType unitType, int min) {
		unitMinimums.put(unitType, min);
	}

	public void checkMinimums() {
		for (Entry<UnitType, Integer> entry : unitMinimums.entrySet()) {
			UnitType unitType = entry.getKey();
			int currentCount = getMyUnitCount(unitType);
			int inQueueCount = getCountInQueue(unitType);
			int requiredCount = entry.getValue();
			if (currentCount + inQueueCount < requiredCount) {
				game.sendText("Queuing up another " + unitType.toString());
				addToQueue(unitType);
			}
		}
	}

	@Override
	public void registerDebugFunctions(DebugEngine debugEngine) {
		debugEngine.registerDebugFunction(new DebugModule("buildingqueue") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				String buildQueueString = "";
				for (BuildingPlan plan : buildingQueue) {
					int x = plan.getTx() * 32;
					int y = plan.getTy() * 32;
					int width = plan.getType().tileWidth() * 32;
					int height = plan.getType().tileHeight() * 32;
					engine.drawBoxMap(x, y, x + width, y + height, Color.Green,
							false);
					engine.drawTextMap(x, y, plan.getType().toString());
					if (plan.builder != null) {
						int bx = plan.builder.getX();
						int by = plan.builder.getY();
						engine.drawLineMap(bx, by, x + width / 2,
								y + width / 2, Color.Green);
					}

					buildQueueString += plan.toString() + ", ";
				}
				engine.drawTextScreen(5, 20, "Building Queue: "
						+ buildQueueString);
			}
		});
		debugEngine.registerDebugFunction(new DebugModule("trainingqueue") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				String trainingQueueString = "";
				for (UnitType type : unitQueue.toArray(new UnitType[0])) {
					trainingQueueString += type.toString() + ", ";
				}
				engine.drawTextScreen(5, 40, "Training Queue: "
						+ trainingQueueString);
			}
		});
		debugEngine.registerDebugFunction(new DebugModule("unitminimums") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawTextScreen(5, 80,
						"Unit Minimums: current(queued)/required");
				int y = 90;
				for (Entry<UnitType, Integer> entry : unitMinimums.entrySet()) {
					UnitType unitType = entry.getKey();
					int inQueueCount = getCountInQueue(unitType);
					int currentCount = getMyUnitCount(unitType);
					int requiredCount = entry.getValue();

					if (inQueueCount != 0 || currentCount != 0
							|| requiredCount != 0) {
						engine.drawTextScreen(5, y, unitType.toString() + ": "
								+ currentCount + "(" + inQueueCount + ")/"
								+ requiredCount);
						y += 10;
					}
				}
			}
		});
	}
}
