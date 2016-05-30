package memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;

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
			for (int tx = bf.tilePosition.getX(); tx < bf.tilePosition.getX() + bf.type.tileWidth(); tx++) {
				for (int ty = bf.tilePosition.getY(); ty < bf.tilePosition.getY() + bf.type.tileHeight(); ty++) {
					if (GameHandler.isVisible(tx, ty)) {
						// Update the last seen time
						bf.lastSeen = GameHandler.getFrameCount();
						// If the building is not where it's supposed to be
						if (!GameHandler.getUnitsOnTile(tx, ty).stream().anyMatch(u -> u.getID() == bf.id)) {
							itt.remove();
							continue buildingLoop;
						}
					}
				}
			}
		}
	}

	public static void onUnitShow(Unit unit) {
		if (unit.getType().isBuilding() && !unit.isFlying()) {
			buildings.put(unit.getID(), new BuildingFootprint(unit));
		}
	}

	public static void onUnitMorph(Unit unit) {
		if (unit.getType().isBuilding()) {
			// Vespene Geysers and Zerg buildings
			buildings.put(unit.getID(), new BuildingFootprint(unit));
		}
	}

	public static void onUnitDestroy(Unit unit) {
		if (unit.getType().isBuilding() && !unit.isFlying()) {
			buildings.remove(unit.getID());
		}
	}

	private static void registerDebugFunctions() {
		DebugManager.createDebugModule("footprints").setDraw(() -> {
			for (BuildingFootprint bf : buildings.values()) {
				Position pos = bf.tilePosition.toPosition();
				DrawEngine.drawBoxMap(pos.getX(), pos.getY(), pos.getX() + bf.type.tileWidth() * 32,
						pos.getY() + bf.type.tileHeight() * 32, Color.Yellow, false);
				DrawEngine.drawTextMap(pos.getX(), pos.getY(), bf.type.toString() + " @ " + bf.lastSeen);
			}
		}).setActive(true);
	}
}
