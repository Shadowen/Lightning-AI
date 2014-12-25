package eaglesWings.datastructure;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Queue;

import eaglesWings.gamestructure.DebugEngine;
import eaglesWings.gamestructure.DebugModule;
import eaglesWings.gamestructure.Debuggable;
import eaglesWings.gamestructure.GameHandler;
import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class BuildManager implements Debuggable {
	private GameHandler game;
	private BaseManager baseManager;

	public Hashtable<UnitTypes, Integer> unitMinimums;

	public Queue<BuildingPlan> buildingQueue;
	public Queue<UnitTypes> unitQueue;

	public BuildManager(GameHandler igame, BaseManager ibm) {
		game = igame;
		baseManager = ibm;

		unitMinimums = new Hashtable<UnitTypes, Integer>();
		for (UnitTypes type : UnitTypes.values()) {
			unitMinimums.put(type, 0);
		}

		buildingQueue = new ArrayDeque<BuildingPlan>();
		unitQueue = new ArrayDeque<UnitTypes>();
	}

	public void addToQueue(UnitTypes unitType) {
		if (unitType == UnitTypes.Terran_Refinery) {
			// Refineries get special treatment!
			for (Base b : baseManager.getMyBases()) {
				for (Entry<Integer, GasResource> r : b.gas.entrySet()) {
					if (!r.getValue().gasTaken()) {
						addBuilding(r.getValue().getX() / 32 - 2, r.getValue()
								.getY() / 32 - 1, UnitTypes.Terran_Refinery);
						break;
					}
				}
			}
		} else if (game.getUnitType(unitType.ordinal()).isBuilding()) {
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
	public void addToQueue(UnitTypes unitType, int count) {
		for (int i = 0; i < count; i++) {
			addToQueue(unitType);
		}
	}

	// Build a building at a specific location
	public void addBuilding(Point buildLocation, UnitTypes type) {
		addBuilding(buildLocation.x, buildLocation.y, type);
	}

	// Build a building at a specific location
	public void addBuilding(int tx, int ty, UnitTypes type) {
		buildingQueue.add(new BuildingPlan(game, tx, ty, type));
	}

	// Finds a nearby valid build location for the building of specified type
	// Returns the Point object representing the suitable build tile
	// position
	// for a given building type near specified pixel position (or
	// Point(-1,-1) if not found)
	private Point getBuildLocation(int x, int y, UnitTypes toBuild) {
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
			maxDist++;
		}

		if (ret.x == -1) {
			throw new NullPointerException();
		}

		return ret;
	}

	// Checks if the building type specified can be built at the coordinates
	// given
	private boolean canBuildHere(int left, int top, UnitTypes typeEnum) {
		UnitType type = game.getUnitType(typeEnum.ordinal());
		int width = type.getTileWidth();
		int height = type.getTileHeight();

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
					&& bp.getTx() + bp.getWidth() >= left) {
				if (bp.getTy() <= top + height
						&& bp.getTy() + bp.getHeight() >= top) {
					return false;
				}
			}
		}
		return true;
	}

	// Call this whenever a unit completes construction
	public void doneBuilding(Unit u) {
		UnitTypes type = UnitTypes.values()[u.getTypeID()];
		// Go through planned units
		unitQueue.remove(type);
		// Go through planned buildings
		for (BuildingPlan p : buildingQueue) {
			// If it's the right building according to the plan
			if (u.getTypeID() == p.getTypeID() && u.getTileX() == p.getTx()
					&& u.getTileY() == p.getTy()) {
				// It has been completed
				buildingQueue.remove(p);

				// If it's a refinery, the worker will automatically become
				// a gas miner!
				if (u.getTypeID() == UnitTypes.Terran_Refinery.ordinal()) {
					p.builder.gather(baseManager.getResource(u));
				}

				break;
			}
		}
	}

	public boolean isInQueue(UnitTypes unitType) {
		for (BuildingPlan plan : buildingQueue) {
			if (plan.getTypeID() == unitType.ordinal()) {
				return true;
			}
		}
		for (UnitTypes unitInQueue : unitQueue) {
			if (unitInQueue.ordinal() == unitType.ordinal()) {
				return true;
			}
		}
		return false;
	}

	public int getCountInQueue(UnitTypes unitType) {
		int count = 0;
		for (BuildingPlan plan : buildingQueue) {
			if (plan.getTypeID() == unitType.ordinal()) {
				count++;
			}
		}
		for (UnitTypes type : unitQueue) {
			if (type.ordinal() == unitType.ordinal()) {
				count++;
			}
		}
		return count;
	}

	public int getMyUnitCount(UnitTypes type) {
		int count = 0;
		for (Unit u : game.getMyUnits()) {
			if (!u.isBeingConstructed() && u.getTypeID() == type.ordinal()) {
				count++;
			}
		}
		return count;
	}

	public void setMinimum(UnitTypes unitType, int min) {
		unitMinimums.put(unitType, min);
	}

	public void checkMinimums() {
		for (Entry<UnitTypes, Integer> entry : unitMinimums.entrySet()) {
			UnitTypes unitType = entry.getKey();
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
					engine.drawText(x, y, plan.getTypeName(), false);
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
			}
		});
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				String trainingQueueString = "";
				for (UnitTypes type : unitQueue.toArray(new UnitTypes[0])) {
					trainingQueueString += type.toString() + ", ";
				}
				engine.drawText(5, 40,
						"Training Queue: " + trainingQueueString, true);
			}
		});
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				engine.drawText(5, 80,
						"Unit Minimums: current(queued)/required", true);
				int y = 90;
				for (Entry<UnitTypes, Integer> entry : unitMinimums.entrySet()) {
					UnitTypes unitType = entry.getKey();
					int inQueueCount = getCountInQueue(unitType);
					int currentCount = getMyUnitCount(unitType);
					int requiredCount = entry.getValue();

					if (inQueueCount != 0 || currentCount != 0
							|| requiredCount != 0) {
						engine.drawText(5, y, unitType.toString() + ": "
								+ currentCount + "(" + inQueueCount + ")/"
								+ requiredCount, true);
						y += 10;
					}
				}
			}
		});
	}
}
