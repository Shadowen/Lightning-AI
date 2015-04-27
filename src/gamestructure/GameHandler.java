package gamestructure;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;

public class GameHandler {
	private Game game;

	public GameHandler(Game g) {
		game = g;

		game.setTextSize(1);
		// allow me to manually control units during the game
		game.enableFlag(1);
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

	public Unit getClosestUnitOfType(int x, int y, UnitType... types) {
		Set<UnitType> typesSet = new HashSet<UnitType>(Arrays.asList(types));
		Unit closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Unit u : game.getAllUnits()) {
			if (typesSet.contains(u.getType())) {
				double distance = Point.distance(x, y, u.getX(), u.getY());
				if (distance < closestDistance) {
					closestDistance = distance;
					closest = u;
				}
			}
		}
		return closest;
	}

	public Unit getClosestEnemy(int x, int y) {
		double closestDistance = Double.MAX_VALUE;
		Unit closestUnit = null;
		for (Unit u : game.enemy().getUnits()) {
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

	public int getFrameCount() {
		return game.getFrameCount();
	}

	public int getFPS() {
		return game.getFPS();
	}

	public int getAPM() {
		return game.getAPM();
	}

	public Player getSelfPlayer() {
		return game.self();
	}

	/**
	 * Get the neutral player.
	 * 
	 * @return The neutral player.
	 */
	public Player getNeutralPlayer() {
		return game.neutral();
	}

	public List<Unit> getNeutralUnits() {
		return game.getNeutralUnits();
	}

	public Player getEnemyPlayer() {
		return game.enemy();
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

	public void sendText(String message) {
		game.sendText(message);
	}
}
