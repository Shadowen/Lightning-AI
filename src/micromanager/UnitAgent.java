package micromanager;

import java.util.ArrayDeque;
import java.util.Queue;

import bwapi.Position;
import bwapi.Unit;
import bwapi.WalkPosition;

public class UnitAgent {
	public Unit unit;
	public Queue<WalkPosition> path;
	public int timeout;
	public UnitTask task;

	public UnitAgent(Unit u) {
		unit = u;
		path = new ArrayDeque<WalkPosition>();
		task = UnitTask.IDLE;
		timeout = 0;
	}
}
