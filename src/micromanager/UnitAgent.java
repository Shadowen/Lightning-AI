package micromanager;

import bwapi.Unit;

public class UnitAgent {
	public Unit unit;
	public int timeout;
	public UnitTask task;

	public UnitAgent(Unit u) {
		unit = u;
		task = UnitTask.IDLE;
		timeout = 0;
	}
}
