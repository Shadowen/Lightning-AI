package memory;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import gamestructure.GameHandler;

public class BuildingFootprint {
	private final int id;
	private final UnitType type;
	private final TilePosition tilePosition;
	private long lastSeen;

	public BuildingFootprint(Unit building) {
		id = building.getID();
		type = building.getType();
		tilePosition = building.getTilePosition();

		lastSeen = GameHandler.getFrameCount();
	}

	public int getId() {
		return id;
	}

	public UnitType getType() {
		return type;
	}

	public TilePosition getTilePosition() {
		return tilePosition;
	}

	public long getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
	}
}
