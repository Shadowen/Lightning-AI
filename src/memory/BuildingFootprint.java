package memory;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import gamestructure.GameHandler;

public class BuildingFootprint {
	public final int id;
	public UnitType type;
	public final TilePosition tilePosition;

	public long lastSeen;

	public BuildingFootprint(Unit building) {
		id = building.getID();
		type = building.getType();
		tilePosition = building.getTilePosition();

		lastSeen = GameHandler.getFrameCount();
	}
}
