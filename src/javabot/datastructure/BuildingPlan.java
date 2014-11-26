package javabot.datastructure;

import javabot.gamestructure.GameHandler;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;

public class BuildingPlan {
	private UnitTypes type;
	private int tx;
	private int ty;
	private int theight;
	private int twidth;
	public Worker builder;

	public BuildingPlan(GameHandler game, int itx, int ity, UnitTypes itype) {
		type = itype;
		tx = itx;
		ty = ity;
		theight = game.getUnitType(type.ordinal()).getTileHeight();
		twidth = game.getUnitType(type.ordinal()).getTileWidth();
	}

	public String getTypeName() {
		return type.toString();
	}

	public int getTypeID() {
		return type.ordinal();
	}

	public int getTx() {
		return tx;
	}

	public int getTy() {
		return ty;
	}

	public String toString() {
		return "[" + type.toString() + "] @ (" + tx * 16 + ", " + ty * 16 + ")";
	}

	public boolean hasBuilder() {
		return builder != null;
	}

	public void setBuilder(Worker ibuilder) {
		builder = ibuilder;
	}

	public int getWidth() {
		return twidth;
	}

	public int getHeight() {
		return theight;
	}
}
