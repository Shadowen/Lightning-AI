package javabot.datastructure;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.UnitType.UnitTypes;

public class Base {

	private static final int MAX_MINERAL_DISTANCE = 10;
	private static final int MAX_GAS_DISTANCE = 10;

	private List<Unit> minerals;
	private List<Unit> gas;
	public List<Unit> workers;
	public Unit commandCenter;
	public BaseLocation location;

	public Base(GameHandler game, BaseLocation l) {

		workers = new ArrayList<Unit>();
		location = l;

		minerals = new ArrayList<Unit>();
		for (Unit u : game.getAllUnits()) {
			if (u.getTypeID() == UnitTypes.Resource_Mineral_Field.ordinal()) {
				if (Point.distance(l.getX(), l.getY(), u.getX(), u.getY()) < MAX_MINERAL_DISTANCE) {
					minerals.add(u);
				}
			}
		}
		gas = new ArrayList<Unit>();
		for (Unit u : game.getAllUnits()) {
			if (u.getTypeID() == UnitTypes.Resource_Mineral_Field.ordinal()
					|| u.getTypeID() == UnitTypes.Terran_Refinery.ordinal()
					|| u.getTypeID() == UnitTypes.Protoss_Assimilator.ordinal()
					|| u.getTypeID() == UnitTypes.Zerg_Extractor.ordinal()) {
				if (Point.distance(l.getX(), l.getY(), u.getX(), u.getY()) < MAX_GAS_DISTANCE) {
					gas.add(u);
				}
			}
		}
	}

	public Unit getBuilder() {
		for (Unit u : workers) {
			if (!u.isConstructing() && !u.isCarryingMinerals()
					&& !u.isCarryingGas()) {
				return u;
			}
		}
		return null;
	}

	public int getMineralCount() {
		return minerals.size();
	}

}
