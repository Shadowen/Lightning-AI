package memory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;
import pathing.PathFinder;

public class MemoryManager {
	private static Map<Integer, BuildingFootprint> buildings;

	public static void init() {
		System.out.print("Starting DebugManager... ");
		buildings = new HashMap<>();
		registerDebugFunctions();
		System.out.println("Success!");
	}

	public static void onFrame() {
		// Check if any remembered buildings disappeared
		Iterator<BuildingFootprint> itt = buildings.values().iterator();
		buildingLoop: while (itt.hasNext()) {
			BuildingFootprint bf = itt.next();
			for (int tx = bf.getTilePosition().getX(); tx < bf.getTilePosition().getX()
					+ bf.getType().tileWidth(); tx++) {
				for (int ty = bf.getTilePosition().getY(); ty < bf.getTilePosition().getY()
						+ bf.getType().tileHeight(); ty++) {
					if (GameHandler.isVisible(tx, ty)) {
						// Update the last seen time
						bf.lastSeen = GameHandler.getFrameCount();
						// If the building is not where it's supposed to be
						if (!GameHandler.getUnitsOnTile(tx, ty).stream().anyMatch(u -> u.getID() == bf.id)) {
							itt.remove();
							PathFinder.removeBuilding(bf);
							continue buildingLoop;
						}
					}
				}
			}
		}
		// Check if any flying buildings have landed
		GameHandler.getAllUnits().stream()
				.filter(u -> u.getType().isFlyingBuilding() && !u.isFlying() && !buildings.containsKey(u.getID()))
				.forEach(u -> addBuilding(u));
		// Alternate method
		// for (Unit unit : GameHandler.getAllUnits()) {
		// if (unit.getType().isFlyingBuilding() && !unit.isFlying() &&
		// !buildings.containsKey(unit.getID())) {
		// addBuilding(unit);
		// }
		// }
	}

	private static void addBuilding(Unit unit) {
		buildings.put(unit.getID(), new BuildingFootprint(unit));
		PathFinder.addBuilding(buildings.get(unit.getID()));
	}

	public static void onUnitShow(Unit unit) {
		if (unit.getType().isBuilding() && !unit.isFlying()) {
			addBuilding(unit);
		}
	}

	public static void onUnitMorph(Unit unit) {
		if (unit.getType().isBuilding()) {
			// Vespene Geysers and Zerg buildings
			// Because unit IDs are preserved by morphing, this effectively
			// updates the unit type only
			addBuilding(unit);
		}
	}

	public static void onUnitDestroy(Unit unit) {
		if (unit.getType().isBuilding() && !unit.isFlying()) {
			PathFinder.removeBuilding(buildings.get(unit.getID()));
			buildings.remove(unit.getID());
		}
	}

	private static void registerDebugFunctions() {
		DebugManager.createDebugModule("footprints").setDraw(() -> {
			for (BuildingFootprint bf : buildings.values()) {
				Position pos = bf.getTilePosition().toPosition();
				DrawEngine.drawBoxMap(pos.getX(), pos.getY(), pos.getX() + bf.getType().tileWidth() * 32,
						pos.getY() + bf.getType().tileHeight() * 32, Color.Yellow, false);
				DrawEngine.drawTextMap(pos.getX(), pos.getY(), bf.getType().toString() + " @ " + bf.lastSeen);
			}
		}).setActive(true);
	}
}
