package micro;

import java.util.ArrayList;
import java.util.List;

import bwapi.Position;
import bwapi.Unit;

public abstract class UnitGroup {
	public List<UnitAgent> unitAgents;
	public UnitTask task;

	public Unit target;

	public UnitGroup() {
		unitAgents = new ArrayList<UnitAgent>();
		task = UnitTask.IDLE;
	}

	public abstract void act();

	public Position getCenterPosition() {
		double cx = 0;
		double cy = 0;
		for (UnitAgent ua : unitAgents) {
			cx += ua.unit.getX();
			cy += ua.unit.getY();
		}
		return new Position((int) (cx / unitAgents.size()), (int) (cy / unitAgents.size()));
	}

	public void addUnitAgent(UnitAgent ua) {
		unitAgents.add(ua);
	}

	public void removeUnit(UnitAgent ua) {
		unitAgents.remove(ua);
	}
}
