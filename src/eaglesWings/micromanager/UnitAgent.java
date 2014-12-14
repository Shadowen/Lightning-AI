package eaglesWings.micromanager;

import eaglesWings.gamestructure.GameHandler;
import javabot.model.Unit;

public class UnitAgent {
	private GameHandler game;
	public Unit unit;
	public int timeout;
	public UnitTask task;

	public UnitAgent(GameHandler igame, Unit u) {
		unit = u;
		game = igame;
		task = UnitTask.IDLE;
		timeout = 0;
	}
}
