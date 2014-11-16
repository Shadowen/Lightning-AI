package javabot.datastructure;

import javabot.model.Unit;

public class Resource {
	private Unit unit;
	private int gatherers;

	public Resource(Unit u) {
		unit = u;
		gatherers = 0;
	}

	public int getID() {
		return unit.getID();
	}

	public void addGatherer() {
		gatherers++;
	}

	public void removeGatherer() {
		if (gatherers <= 0) {
			throw new IndexOutOfBoundsException();
		}
		gatherers--;
	}

	public int getNumGatherers() {
		return gatherers;
	}

	public int getX() {
		return unit.getX();
	}

	public int getY() {
		return unit.getY();
	}
}