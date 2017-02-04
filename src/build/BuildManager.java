package build;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import base.Base;
import base.BaseManager;
import base.GasResource;
import bwapi.Color;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;

public final class BuildManager {
	private static Hashtable<UnitType, Integer> unitMinimums;
	public static Queue<BuildingPlan> buildingQueue;
	public static Queue<UnitType> unitQueue;
	// Only contains my units under construction
	public static Set<Unit> unitsUnderConstruction;

	public static void init() {
		System.out.print("Starting BuildManager... ");
		unitMinimums = new Hashtable<UnitType, Integer>();
		buildingQueue = new ArrayDeque<BuildingPlan>();
		unitQueue = new ArrayDeque<UnitType>();
		unitsUnderConstruction = new HashSet<Unit>();

		registerDebugFunctions();
		System.out.println("Success!");
	}

	/** This constructor should never be called */
	private BuildManager() {
	}

	/**
	 * Add a building or unit to the queue to be built. No control over the
	 * location.
	 * 
	 * @param unitType
	 */
	public static void addToQueue(UnitType unitType) {
		if (unitType == UnitType.Terran_Refinery) {
			// Refineries get special treatment!
			for (Base b : BaseManager.getMyBases()) {
				// Find a gas that isn't taken yet
				for (GasResource r : b.gas) {
					if (!r.gasTaken()) {
						addBuilding(r.getX() / 32 - 2, r.getY() / 32 - 1, UnitType.Terran_Refinery);
						return;
					}
				}
				GameHandler.sendText("Wanted to take another gas, but none left!");
			}
		} else if (unitType.isBuilding()) {
			// Otherwise, buildings
			if (BaseManager.main != null) {
				Point location = GameHandler.getBuildLocation(BaseManager.main.getX(), BaseManager.main.getY(),
						unitType);
				addBuilding(location.x, location.y, unitType);
			}
		} else {
			// Finally, units
			unitQueue.add(unitType);
		}
	}

	/**
	 * Add some buildings or units to the queue to be built. No contorl over the
	 * location.
	 * 
	 * @param unitType
	 * @param count
	 *            the amount of this unit to build
	 */
	public static void addToQueue(UnitType unitType, int count) {
		for (int i = 0; i < count; i++) {
			addToQueue(unitType);
		}
	}

	// Build a building at a specific location
	public static void addBuilding(TilePosition buildLocation, UnitType type) {
		addBuilding(buildLocation.getX(), buildLocation.getY(), type);
	}

	// Build a building at a specific location
	public static void addBuilding(int tx, int ty, UnitType type) {
		buildingQueue.add(new BuildingPlan(tx, ty, type));
	}

	public static void unitConstructed(Unit u) {
		UnitType type = u.getType();
		if (type.isBuilding()) {
			// Go through planned buildings
			for (BuildingPlan p : buildingQueue) {
				// If it's the right building according to the plan

				if (u.getType().equals(p.getType()) && u.getTilePosition().equals(p.getTilePosition())) {
					// It has been completed
					buildingQueue.remove(p);
					p.builder.setTaskMiningMinerals();
					break;
				}
			}
			if (u.getType().isRefinery()) {
				// If it's a refinery, the worker will automatically
				// become a gas miner!
				GasResource r = new GasResource(u);
				BaseManager.getClosestBase(u.getPosition()).ifPresent(b -> b.gas.add(r));
				u.gather(r.getUnit());
			}
		}
	}

	public static boolean isInQueue(UnitType unitType) {
		if (unitType.isBuilding()) {
			return buildingQueue.stream().anyMatch(bp -> bp.getType() == unitType);
		}
		return unitQueue.stream().anyMatch(u -> u == unitType)
				|| unitsUnderConstruction.stream().anyMatch(u -> u.getType() == unitType);
	}

	public static long getCountInQueue(UnitType unitType) {
		if (unitType.isBuilding()) {
			return buildingQueue.stream().map(bp -> bp.getType()).filter(ut -> ut == unitType).count();
		}
		return unitQueue.stream().filter(ut -> ut == unitType).count();
	}

	public static long getTrainingCount(UnitType unitType) {
		return unitsUnderConstruction.stream().filter(u -> u.getType() == unitType).count();
	}

	public static int getMyUnitCount(UnitType type) {
		return (int) GameHandler.getMyUnits().stream().filter(u -> !u.isBeingConstructed())
				.filter(u -> u.getType() == type).count();
	}

	public static void setMinimum(UnitType unitType, int min) {
		unitMinimums.put(unitType, min);
	}

	/**
	 * Check if any buildings or units are below required minimums. If they are,
	 * put more of them into the building queue!
	 */
	public static void checkMinimums() {
		for (Entry<UnitType, Integer> entry : unitMinimums.entrySet()) {
			UnitType unitType = entry.getKey();
			int currentCount = getMyUnitCount(unitType);
			long inQueueCount = getCountInQueue(unitType);
			long inTrainingCount = getTrainingCount(unitType);
			int requiredCount = entry.getValue();
			if (currentCount + inQueueCount + inTrainingCount < requiredCount) {
				// GameHandler.sendText("Queuing up another " +
				// unitType.toString());
				addToQueue(unitType);
			}
		}
	}

	public static boolean buildingPlannedForLocation(TilePosition tilePosition) {
		return buildingQueue.stream()
				.anyMatch(b -> b.getBoundingBox().contains(tilePosition.getX() * 32, tilePosition.getY() * 32));
	}

	public static void registerDebugFunctions() {
		DebugManager.createDebugModule("buildingqueue").setDraw(() -> {
			String buildQueueString = "";
			for (BuildingPlan plan : buildingQueue) {
				Rectangle bb = plan.getBoundingBox();
				DrawEngine.drawBoxMap((int) bb.getMinX(), (int) bb.getMinY(), (int) bb.getMaxX(), (int) bb.getMaxY(),
						Color.Green, false);
				DrawEngine.drawTextMap((int) bb.getMinX(), (int) bb.getMinY(), plan.getType().toString());
				if (plan.builder != null) {
					int bx = plan.builder.unit.getX();
					int by = plan.builder.unit.getY();

					DrawEngine.drawLineMap(bx, by, (int) bb.getCenterX(), (int) bb.getCenterY(), Color.Green);
				}

				buildQueueString += plan.toString() + ", ";
			}
			DrawEngine.drawTextScreen(5, 20, "Building Queue: " + buildQueueString);
		}).setActive(true);
		DebugManager.createDebugModule("trainingqueue").setDraw(() -> {
			String trainingQueueString = "";
			for (UnitType type : unitQueue.toArray(new UnitType[0])) {
				trainingQueueString += type.toString() + ", ";
			}
			DrawEngine.drawTextScreen(5, 40, "Training Queue: " + trainingQueueString);
		}).setActive(true);
		DebugManager.createDebugModule("unitminimums").setDraw(() -> {
			// Unit minimums
			DrawEngine.drawTextScreen(5, 80, "Unit Minimums: current(queued, training)/required");
			int y = 90;
			for (Entry<UnitType, Integer> entry : unitMinimums.entrySet()) {
				UnitType unitType = entry.getKey();
				long inQueueCount = getCountInQueue(unitType);
				long inTrainingCount = getTrainingCount(unitType);
				int currentCount = getMyUnitCount(unitType);
				int requiredCount = entry.getValue();

				if (inQueueCount != 0 || currentCount != 0 || inTrainingCount != 0 || requiredCount != 0) {
					DrawEngine.drawTextScreen(5, y, unitType.toString() + ": " + currentCount + "(" + inQueueCount
							+ ", " + inTrainingCount + ")/" + requiredCount);
					y += 10;
				}
			}
		}).setActive(true);
	}
}
