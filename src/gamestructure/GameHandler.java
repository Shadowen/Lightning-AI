package gamestructure;

import java.awt.Point;
import java.util.List;
import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;

public class GameHandler {
	private Game game;
	private DebugEngine debugEngine;

	public GameHandler(Game g) {
		game = g;
		debugEngine = new DebugEngine(game);
	}

	public Unit getClosestUnitOfType(int x, int y, UnitType type) {
		Unit closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Unit u : game.getAllUnits()) {
			if (u.getType() == type) {
				double distance = Point.distance(x, y, u.getX(), u.getY());
				if (distance < closestDistance) {
					closestDistance = distance;
					closest = u;
				}
			}
		}
		return closest;
	}

	public void registerDebugFunction(DebugModule m) {
		debugEngine.debugModules.add(m);
	}

	public void drawDebug() {
		debugEngine.draw();
	}

	public Unit getClosestEnemy(int x, int y) {
		double closestDistance = Double.MAX_VALUE;
		Unit closestUnit = null;
		for (Unit u : game.getAllUnits()) {
			// TODO get enemy units only
			double distanceX = x - u.getX();
			double distanceY = y - u.getY();
			double distance = Math.sqrt(Math.pow(distanceX, 2)
					+ Math.pow(distanceY, 2));

			if (distance < closestDistance) {
				closestUnit = u;
				closestDistance = distance;
			}
		}
		return closestUnit;
	}

	public Unit getClosestEnemy(Unit toWho) {
		return getClosestEnemy(toWho.getX(), toWho.getY());
	}

	public void setTextSize(int size) {
		game.setTextSize(size);
	}

	public void enableFlag(int i) {
		game.enableFlag(i);
	}

	public int getFrameCount() {
		return game.getFrameCount();
	}

	public Player self() {
		return game.self();
	}

	public boolean isVisible(int tileX, int tileY) {
		return game.isVisible(tileX, tileY);
	}

	public List<Unit> getAllUnits() {
		return game.getAllUnits();
	}

	public boolean isBuildable(int tileX, int tileY, boolean includeBuildings) {
		return game.isBuildable(tileX, tileY, includeBuildings);
	}

	public void sendText(String string) {
		game.sendText(string);
	}
}
