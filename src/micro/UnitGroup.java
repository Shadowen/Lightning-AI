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
		// TODO cache this
		double cx = 0;
		double cy = 0;
		for (UnitAgent ua : unitAgents) {
			cx += ua.unit.getX();
			cy += ua.unit.getY();
		}
		return new Position((int) (cx / unitAgents.size()), (int) (cy / unitAgents.size()));
	}

	public double getMaxDistance() {
		double distance = Double.MIN_VALUE;
		Position center = getCenterPosition();
		for (UnitAgent ua : unitAgents) {
			distance = Math.max(ua.unit.getPosition().getDistance(center), distance);
		}
		return distance;
	}

	public void addUnitAgent(UnitAgent ua) {
		unitAgents.add(ua);
	}

	public void removeUnit(UnitAgent ua) {
		unitAgents.remove(ua);
	}
}
