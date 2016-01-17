package base;

import java.util.ArrayList;
import java.util.List;

import bwapi.Unit;

public abstract class Resource {
	protected Unit unit;
	private List<Worker> gatherers;

	public Resource(Unit u) {
		unit = u;
		gatherers = new ArrayList<>();
	}

	public Unit getUnit() {
		return unit;
	}

	public void addGatherer(Worker w) {
		gatherers.add(w);
	}

	public void removeGatherer(Worker w) {
		gatherers.remove(w);
	}

	public int getNumGatherers() {
		return gatherers.size();
	}

	public int getX() {
		return unit.getX();
	}

	public int getY() {
		return unit.getY();
	}
}
