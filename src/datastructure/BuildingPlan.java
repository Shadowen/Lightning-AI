package datastructure;

import bwapi.TilePosition;
import bwapi.UnitType;
import gamestructure.GameHandler;

public class BuildingPlan {
	private UnitType type;
	private TilePosition tilePosition;
	public Worker builder;

	public BuildingPlan(GameHandler game, int itx, int ity, UnitType itype) {
		type = itype;
		tilePosition = new TilePosition(itx, ity);
	}

	public UnitType getType() {
		return type;
	}

	public int getTx() {
		return tilePosition.getX();
	}

	public int getTy() {
		return tilePosition.getY();
	}

	public TilePosition getTilePosition() {
		return tilePosition;
	}

	public boolean hasBuilder() {
		return builder != null;
	}

	public void setBuilder(Worker ibuilder) {
		builder = ibuilder;
	}

	public String toString() {
		return "[" + type.toString() + "] @ (" + getTx() * 16 + ", " + getTy()
				* 16 + ")";
	}
}
