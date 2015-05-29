package gamestructure;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import bwapi.Game;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.WalkPosition;

public final class GameHandler {
	private static Game game = JavaBot.mirror.getGame();
	/**
	 * A cache keeping track of my units. Recomputed every time allUnits
	 * changes.
	 */
	private static Set<Unit> myUnits;
	/** The hash code of allUnits at the time myUnits was last computed. */
	private static int myUnitsHash = 0;

	static {
		game.setTextSize(bwapi.Text.Size.Enum.Small);
		// allow me to manually control units during the game
		game.enableFlag(1);
	}

	public static int getMapWidth() {
		return game.mapWidth();
	}

	public static int getMapHeight() {
		return game.mapHeight();
	}

	public static Optional<Unit> getClosestUnitOfType(int x, int y,
			UnitType... types) {
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
		return Optional.ofNullable(closest);
	}

	public static Optional<Unit> getClosestEnemy(int x, int y) {
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
		return Optional.ofNullable(closestUnit);
	}

	public static Optional<Unit> getClosestEnemy(Unit toWho) {
		return getClosestEnemy(toWho.getX(), toWho.getY());
	}

	public static int getFrameCount() {
		return game.getFrameCount();
	}

	public static int getFPS() {
		return game.getFPS();
	}

	public static int getAPM() {
		return game.getAPM();
	}

	public static Player getSelfPlayer() {
		return game.self();
	}

	/**
	 * Get the neutral player.
	 * 
	 * @return The neutral player.
	 */
	public static Player getNeutralPlayer() {
		return game.neutral();
	}

	public static List<Unit> getNeutralUnits() {
		return game.getNeutralUnits();
	}

	public static Player getEnemyPlayer() {
		return game.enemy();
	}

	public static boolean isVisible(int tileX, int tileY) {
		return game.isVisible(tileX, tileY);
	}

	public static List<Unit> getAllUnits() {
		return game.getAllUnits();
	}

	/**
	 * Get the set of units owned by me. Caches myUnits internally and
	 * recomputes if allUnits has changed since the last invocation.
	 * 
	 * @return my units
	 * */
	public static Set<Unit> getMyUnits() {
		// Recompute my units
		List<Unit> allUnits = game.getAllUnits();
		if (allUnits.hashCode() != myUnitsHash) {
			System.out.println("Recomputing myUnits..."); // TODO
			allUnits.stream().filter(u -> u.getPlayer() == getSelfPlayer())
					.collect(Collectors.toSet());
			myUnitsHash = allUnits.hashCode();
		} else {
			System.out.println("Retrieving myUnits from cache!"); // TODO
		}
		return myUnits;
	}

	public static boolean isBuildable(TilePosition position,
			boolean includeBuildings) {
		return game.isBuildable(position, includeBuildings);
	}

	public static boolean isBuildable(int tileX, int tileY,
			boolean includeBuildings) {
		return game.isBuildable(tileX, tileY, includeBuildings);
	}

	public static boolean isWalkable(WalkPosition position) {
		return game.isWalkable(position);
	}

	public static boolean isWalkable(int wx, int wy) {
		return game.isWalkable(wx, wy);
	}

	public static void sendText(String message) {
		game.sendText(message);
	}
}
