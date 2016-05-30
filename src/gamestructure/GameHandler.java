package gamestructure;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import build.BuildManager;
import build.BuildingPlan;
import bwapi.Bullet;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Region;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.WalkPosition;

public final class GameHandler {
	private static Game game;

	public static void init(Game igame) {
		System.out.print("Starting GameHandler... ");
		game = igame;
		game.setTextSize(bwapi.Text.Size.Enum.Small);
		// allow me to manually control units during the game
		game.enableFlag(1);

		System.out.println("Success!");
	}

	/**
	 * @return the width of the map in build tiles
	 */
	public static int getMapWidth() {
		return game.mapWidth();
	}

	/**
	 * @return the width of the map in walk tiles
	 */
	public static int getMapWalkWidth() {
		return 4 * getMapWidth();
	}

	/**
	 * @return the height of the map in build tiles
	 */
	public static int getMapHeight() {
		return game.mapHeight();
	}

	/**
	 * @return the height of the map in walk tiles
	 */
	public static int getMapWalkHeight() {
		return 4 * getMapHeight();
	}

	/**
	 * Get the closest unit that is one of the given types
	 * 
	 * @param x
	 * @param y
	 * @param types
	 *            a varags parameter of the types of units to search for.
	 * @return a unit if found
	 */
	public static Optional<Unit> getClosestUnit(int x, int y, UnitType... types) {
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

	public static Optional<Unit> getClosestEnemyUnit(int x, int y) {
		double closestDistance = Double.MAX_VALUE;
		Unit closestUnit = null;
		for (Unit u : game.enemy().getUnits()) {
			double distanceX = x - u.getX();
			double distanceY = y - u.getY();
			double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));

			if (distance < closestDistance) {
				closestUnit = u;
				closestDistance = distance;
			}
		}
		return Optional.ofNullable(closestUnit);
	}

	public static Optional<Unit> getClosestEnemy(Unit toWho) {
		return getClosestEnemyUnit(toWho.getX(), toWho.getY());
	}

	/**
	 * Retrieves the set of accessible units that are on a given build tile.
	 * 
	 * @param tileX
	 *            The X position, in tiles.
	 * @param tileY
	 *            The Y position, in tiles.
	 * @return A Unitset object consisting of all the units that have any part
	 *         of them on the given build tile.
	 */
	public static List<Unit> getUnitsOnTile(int tileX, int tileY) {
		return game.getUnitsOnTile(tileX, tileY);
	}

	/**
	 * Retrieves the set of accessible units that are on a given build tile.
	 * 
	 * @param tile
	 *            The tile position.
	 * @return A Unitset object consisting of all the units that have any part
	 *         of them on the given build tile.
	 */
	public static List<Unit> getUnitsOnTile(TilePosition tile) {
		return game.getUnitsOnTile(tile);
	}

	/**
	 * Retrieves the set of accessible units that are in a given rectangle.
	 * 
	 * @param left
	 *            The X coordinate of the left position of the bounding box, in
	 *            pixels.
	 * @param top
	 *            The Y coordinate of the top position of the bounding box, in
	 *            pixels.
	 * @param right
	 *            The X coordinate of the right position of the bounding box, in
	 *            pixels.
	 * @param bottom
	 *            The Y coordinate of the bottom position of the bounding box,
	 *            in pixels.
	 * @return A Unitset object consisting of all the units that have any part
	 *         of them within the given rectangle bounds.
	 */
	public static List<Unit> getUnitsInRectangle(int left, int top, int right, int bottom) {
		return game.getUnitsInRectangle(left, top, right, bottom);
	}

	/**
	 * Retrieves the set of accessible units that are in a given rectangle.
	 * 
	 * @param topLeft
	 *            The top left corner position of the bounding box, in pixels.
	 * @param bottomRight
	 *            The bottom right corner position of the bounding box, in
	 *            pixels.
	 * @return A Unitset object consisting of all the units that have any part
	 *         of them within the given rectangle bounds.
	 */
	public static List<Unit> getUnitsInRectangle(Position topLeft, Position bottomRight) {
		return game.getUnitsInRectangle(topLeft, bottomRight);
	}

	/**
	 * Retrieves the set of accessible units that are within a given radius of a
	 * position.
	 * 
	 * @param x
	 *            The x coordinate of the center, in pixels.
	 * @param y
	 *            The y coordinate of the center, in pixels.
	 * @param radius
	 *            The radius from the center, in pixels, to include units. pred
	 *            (optional) A function predicate that indicates which units are
	 *            included in the returned set.
	 * @return A Unitset object consisting of all the units that have any part
	 *         of them within the given radius from the center position.
	 */
	public static List<Unit> getUnitsInRadius(int x, int y, int radius) {
		return game.getUnitsInRadius(x, y, radius);
	}

	/**
	 * Retrieves the set of accessible units that are within a given radius of a
	 * position.
	 * 
	 * @param center
	 *            The coordinates of the center, in pixels.
	 * @param radius
	 *            The radius from the center, in pixels, to include units. pred
	 *            (optional) A function predicate that indicates which units are
	 *            included in the returned set.
	 * @return A Unitset object consisting of all the units that have any part
	 *         of them within the given radius from the center position.
	 */
	public static List<Unit> getUnitsInRadius(Position center, int radius) {
		return game.getUnitsInRadius(center, radius);
	}

	public static int getFrameCount() {
		return game.getFrameCount();
	}

	public static int getFPS() {
		return game.getFPS();
	}

	public static double getAverageFPS() {
		return game.getAverageFPS();
	}

	public static int getAPM() {
		return game.getAPM();
	}

	public static int getAPM(boolean includeSelects) {
		return game.getAPM(includeSelects);
	}

	public static int countdownTimer() {
		return game.countdownTimer();
	}

	public static int elapsedTime() {
		return game.elapsedTime();
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

	public static Player getEnemyPlayer() {
		return game.enemy();
	}

	public static List<Unit> getAllUnits() {
		return game.getAllUnits();
	}

	/**
	 * Get a list of all units owned by me.
	 * 
	 * @return my units
	 */
	public static List<Unit> getMyUnits() {
		return game.self().getUnits();
	}

	public static List<Unit> getNeutralUnits() {
		return game.getNeutralUnits();
	}

	public static List<Unit> getEnemyUnits() {
		return game.enemies().stream().flatMap(p -> p.getUnits().stream()).collect(Collectors.toList());
	}

	public List<Unit> getMinerals() {
		return game.getMinerals();
	}

	public List<Unit> getGeysers() {
		return game.getGeysers();
	}

	public static boolean isVisible(int tileX, int tileY) {
		return game.isVisible(tileX, tileY);
	}

	public static boolean isVisible(TilePosition tilePosition) {
		return game.isVisible(tilePosition.getX(), tilePosition.getY());
	}

	public static boolean canBuildHere(TilePosition position, UnitType type) {
		return game.canBuildHere(position, type);
	}

	public static boolean canBuildHere(TilePosition position, UnitType type, Unit builder) {
		return game.canBuildHere(position, type, builder);
	}

	public static boolean canBuildHere(TilePosition position, UnitType type, Unit builder, boolean checkExplored) {
		return game.canBuildHere(position, type, builder, checkExplored);
	}

	public static boolean canMake(UnitType type) {
		return game.canMake(type);
	}

	public static boolean canMake(UnitType type, Unit builder) {
		return game.canMake(type, builder);
	}

	public static boolean canResearch(TechType type) {
		return game.canResearch(type);
	}

	public static boolean canResearch(TechType type, Unit unit) {
		return game.canResearch(type, unit);
	}

	public static boolean canResearch(TechType type, Unit unit, boolean checkCanIssueCommandType) {
		return game.canResearch(type, unit, checkCanIssueCommandType);
	}

	public static boolean isBuildable(TilePosition position, boolean includeBuildings) {
		return game.isBuildable(position, includeBuildings);
	}

	public static boolean isBuildable(int tileX, int tileY, boolean includeBuildings) {
		return game.isBuildable(tileX, tileY, includeBuildings);
	}

	public static boolean isWalkable(WalkPosition position) {
		return game.isWalkable(position);
	}

	public static boolean isWalkable(int wx, int wy) {
		return game.isWalkable(wx, wy);
	}

	public static TilePosition getBuildLocation(UnitType type, TilePosition desiredPosition) {
		return game.getBuildLocation(type, desiredPosition);
	}

	public static TilePosition getBuildLocation(UnitType type, TilePosition desiredPosition, int maxRange) {
		return game.getBuildLocation(type, desiredPosition, maxRange);
	}

	public static List<Bullet> getBullets() {
		return game.getBullets();
	}

	public static int getDamageFrom(UnitType fromType, UnitType toType) {
		return game.getDamageFrom(fromType, toType);
	}

	public static int getDamageFrom(UnitType fromType, UnitType toType, Player fromPlayer) {
		return game.getDamageFrom(fromType, toType, fromPlayer);
	}

	public static int getDamageFrom(UnitType fromType, UnitType toType, Player fromPlayer, Player toPlayer) {
		return game.getDamageFrom(fromType, toType, fromPlayer, toPlayer);
	}

	public static int getDamageTo(UnitType toType, UnitType fromType) {
		return game.getDamageTo(toType, fromType);
	}

	public static int getDamageTo(UnitType toType, UnitType fromType, Player toPlayer) {
		return game.getDamageTo(toType, fromType, toPlayer);
	}

	public static int getDamageTo(UnitType toType, UnitType fromType, Player toPlayer, Player fromPlayer) {
		return game.getDamageTo(toType, fromType, toPlayer, fromPlayer);
	}

	public static boolean hasCreep(int tileX, int tileY) {
		return game.hasCreep(tileX, tileY);
	}

	public static boolean hasCreep(TilePosition position) {
		return game.hasCreep(position);
	}

	public static List<Region> getAllRegions() {
		return game.getAllRegions();
	}

	/**
	 * Finds a nearby valid build location for the building of specified type
	 * 
	 * @return a {@link Point} representing the suitable build tile position for
	 *         a given building type near specified pixel position (or
	 *         Point(-1,-1) if not found)
	 */
	public static Point getBuildLocation(int x, int y, UnitType toBuild) {
		Point ret = new Point(-1, -1);
		int maxDist = 3;
		int stopDist = 40;
		int tileX = x / 32;
		int tileY = y / 32;

		while ((maxDist < stopDist) && (ret.x == -1)) {
			for (int i = tileX - maxDist; i <= tileX + maxDist; i++) {
				for (int j = tileY - maxDist; j <= tileY + maxDist; j++) {
					if (canBuildHere(i, j, toBuild)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : GameHandler.getAllUnits()) {
							if ((Math.abs(u.getX() / 32 - i) < 4) && (Math.abs(u.getY() / 32 - j) < 4)) {
								unitsInWay = true;
							}
						}
						if (!unitsInWay) {
							ret.x = i;
							ret.y = j;
							return ret;
						}
					}
				}
			}
			maxDist++;
		}

		if (ret.x == -1) {
			throw new NullPointerException();
		}

		return ret;
	}

	// Checks if the building type specified can be built at the coordinates
	// given
	public static boolean canBuildHere(int left, int top, UnitType type) {
		int width = type.tileWidth();
		int height = type.tileHeight();

		// Check if location is buildable
		for (int i = left; i < left + width; i++) {
			for (int j = top; j < top + height; j++) {
				if (!(GameHandler.isBuildable(i, j, true))) {
					return false;
				}
			}
		}
		// Check if another building is planned for this spot
		for (BuildingPlan bp : BuildManager.buildingQueue) {
			if (bp.getTx() <= left + width && bp.getTx() + bp.getType().tileWidth() >= left) {
				if (bp.getTy() <= top + height && bp.getTy() + bp.getType().tileHeight() >= top) {
					return false;
				}
			}
		}
		return true;
	}

	public static void sendText(String message) {
		game.sendText(message);
	}

	public static List<Unit> getSelectedUnits() {
		return game.getSelectedUnits();
	}

	public static Position getMousePositionOnMap() {
		Position sp = game.getScreenPosition();
		Position mp = game.getMousePosition();
		return new Position(sp.getX() + mp.getX(), sp.getY() + mp.getY());
	}
}
