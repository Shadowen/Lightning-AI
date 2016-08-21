package micro;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

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

	public double getPercentileDistance(double percentile) {
		PriorityQueue<Double> distances = new PriorityQueue<>();
		Position center = getCenterPosition();
		for (UnitAgent ua : unitAgents) {
			distances.add(ua.unit.getPosition().getDistance(center));
		}
		double distance = Double.MIN_NORMAL;
		for (int i = 0; i < unitAgents.size() * percentile && !distances.isEmpty(); i++) {
			distance = distances.remove();
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
