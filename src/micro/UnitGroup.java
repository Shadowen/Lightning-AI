package micro;

import java.util.List;

import bwapi.Unit;

public abstract class UnitGroup {
	public List<UnitAgent> unitAgents;
	public UnitTask task;

	public Unit target;

	public UnitGroup() {
		task = UnitTask.IDLE;
	}

	public abstract void act();

	public void addUnitAgent(UnitAgent ua) {
		unitAgents.add(ua);
	}

	public void removeUnit(UnitAgent ua) {
		unitAgents.remove(ua);
	}
}
