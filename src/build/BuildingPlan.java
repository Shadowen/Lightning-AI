package build;

import java.awt.Rectangle;

import base.Worker;
import bwapi.TilePosition;
import bwapi.UnitType;

public class BuildingPlan {
	private UnitType type;
	private TilePosition tilePosition;
	/** The bounding box of the proposed building in pixels */
	private Rectangle boundingBox;
	public Worker builder;

	public BuildingPlan(int itx, int ity, UnitType itype) {
		type = itype;
		tilePosition = new TilePosition(itx, ity);
		boundingBox = new Rectangle(tilePosition.getX() * 32, tilePosition.getY() * 32, type.tileWidth() * 32,
				type.tileHeight() * 32);
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

	/** Gets the bounding box of the building in pixels */
	public Rectangle getBoundingBox() {
		return boundingBox;
	}

	public boolean hasBuilder() {
		return builder != null;
	}

	public void setBuilder(Worker ibuilder) {
		builder = ibuilder;
	}

	@Override
	public String toString() {
		return "[" + type.toString() + "] @ (" + getTx() * 16 + ", " + getTy() * 16 + ")";
	}
}
