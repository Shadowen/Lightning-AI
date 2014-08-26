package javabot;

import java.util.ArrayList;
import java.util.List;

import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.UnitType.UnitTypes;

public class Base {

	public List<Unit> workers;
	public Unit commandCenter;
	public BaseLocation location;

	public Base(BaseLocation l) {
		workers = new ArrayList<Unit>();
		location = l;
	}

	public Base() {
		workers = new ArrayList<Unit>();
	}

	public Unit getBuilder() {
		for (Unit u : workers) {
			if (!u.isConstructing()) {
				return u;
			}
		}
		return null;
	}

}
