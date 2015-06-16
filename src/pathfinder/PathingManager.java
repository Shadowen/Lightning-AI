package pathfinder;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import datastructure.BaseManager;
import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.Chokepoint;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;

public class PathingManager {

	private static final int MAX_RAMP_WALK_TILES = 500;

	private static ArrayList<ArrayList<Node>> walkableNodes;
	private static int mapWalkWidth;
	private static int mapWalkHeight;

	// A list of tiles detailing a path into the main from the choke
	private static Queue<Point> pathIntoMain;
	private static Point topOfRamp;
	private static List<Point> chokeRampWalkTiles;

	static {
		mapWalkWidth = 0; // TODO
		mapWalkHeight = 0; // TODO

		// Init walkable map
		walkableNodes = new ArrayList<ArrayList<Node>>();
		for (int wx = 0; wx < 0 * 4; wx++) { // TODO
			walkableNodes.add(new ArrayList<Node>());
			for (int wy = 0; wy < 0 * 4; wy++) { // TODO
				walkableNodes.get(wx).add(new Node(wx, wy));
			}
		}

		registerDebugFunctions();
	}

	/** This constructor should never be used. */
	@Deprecated
	private PathingManager() {
	}

	public static void findChokeToMain() {
		Chokepoint choke = BWTA.getNearestChokepoint(BaseManager.main
				.getLocation().getTilePosition());
		// Find the path into the main
		pathIntoMain = findGroundPath(choke.getCenter(), BaseManager.main
				.getLocation().getPosition(), UnitType.Zerg_Zergling);

		// Find top of ramp
		for (Point p : pathIntoMain) {
			if (GameHandler.isBuildable(p.x / 32, p.y / 32, false)) {
				topOfRamp = p;
				break;
			}
		}

		// Mark entire ramp
		chokeRampWalkTiles = new ArrayList<Point>();
		Queue<Point> rampWalkTileOpenSet = new PriorityQueue<Point>(1,
				new Comparator<Point>() {
					@Override
					public int compare(Point p1, Point p2) {
						Point topOfRampWalkTile = new Point(topOfRamp.x / 8,
								topOfRamp.y / 8);
						return (int) (p1.distanceSq(topOfRampWalkTile) - p2
								.distanceSq(topOfRampWalkTile));
					}
				});
		rampWalkTileOpenSet.add(new Point(choke.getCenter().getX() / 8, choke
				.getCenter().getY() / 8));
		while (!rampWalkTileOpenSet.isEmpty()) {
			Point currentNode = rampWalkTileOpenSet.poll();
			if (GameHandler.isWalkable(currentNode.x, currentNode.y)
					&& !GameHandler.isBuildable(currentNode.x / 4,
							currentNode.y / 4, false)) {
				chokeRampWalkTiles.add(new Point(currentNode.x, currentNode.y));
				Point nextNode;
				nextNode = new Point(currentNode.x - 1, currentNode.y);
				if (!chokeRampWalkTiles.contains(nextNode)
						&& !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new Point(currentNode.x + 1, currentNode.y);
				if (!chokeRampWalkTiles.contains(nextNode)
						&& !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new Point(currentNode.x, currentNode.y - 1);
				if (!chokeRampWalkTiles.contains(nextNode)
						&& !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new Point(currentNode.x, currentNode.y + 1);
				if (!chokeRampWalkTiles.contains(nextNode)
						&& !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
			}

			// Safety to prevent the whole map from being interpreted as
			// a ramp
			if (chokeRampWalkTiles.size() >= MAX_RAMP_WALK_TILES) {
				break;
			}
		}
	}

	private static Queue<Point> findGroundPath(Position start, Position end,
			UnitType unitType) {
		return findGroundPath(start.getX(), start.getY(), end.getX(),
				end.getY(), unitType);
	}

	public static Queue<Point> findGroundPath(int startx, int starty, int endx,
			int endy, UnitType unitType) {
		return findGroundPath(startx, starty, endx, endy, unitType,
				Integer.MAX_VALUE);
	}

	public static Queue<Point> findGroundPath(int startx, int starty, int endx,
			int endy, UnitType type, int length) {
		int startWx = startx / 8;
		int startWy = starty / 8;
		int endWx = endx / 8;
		int endWy = endy / 8;

		// Initialize
		for (int wx = 0; wx < mapWalkWidth; wx++) {
			for (int wy = 0; wy < mapWalkHeight; wy++) {
				walkableNodes.get(wx).get(wy).walkable = true;
			}
		}
		// Avoid cliffs
		// TODO New implementation
		// IntStream
		// .range(0, GameHandler.getMapWidth() * 4)
		// .forEach(
		// wx -> IntStream
		// .range(0, GameHandler.getMapHeight() * 4)
		// .forEach(
		// wy -> {
		// for (int iwx = Math.max(wx - 3, 0); iwx < Math
		// .min(wx + 3, mapWalkWidth); iwx++) {
		// for (int iwy = Math.max(wy - 3,
		// 0); iwy < Math.min(
		// wy + 3, mapWalkHeight); iwy++) {
		// walkableNodes.get(iwx).get(
		// iwy).walkable = GameHandler
		// .isWalkable(wx, wy);
		// }
		// }
		// }));
		for (int wx = 0; wx < GameHandler.getMapWidth() * 4; wx++) {
			for (int wy = 0; wy < GameHandler.getMapHeight() * 4; wy++) {
				if (!GameHandler.isWalkable(wx, wy)) {
					for (int iwx = Math.max(wx - 3, 0); iwx < Math.min(wx + 3,
							mapWalkWidth); iwx++) {
						for (int iwy = Math.max(wy - 3, 0); iwy < Math.min(
								wy + 3, mapWalkHeight); iwy++) {
							walkableNodes.get(iwx).get(iwy).walkable = false;
						}
					}
				}
			}
		}
		// Avoid buildings
		for (Unit u : GameHandler.getAllUnits()) {
			UnitType utype = u.getType();
			if (utype.isBuilding()) {
				int uwidth = utype.tileWidth();
				int uheight = utype.tileHeight();
				int tx = u.getTilePosition().getX();
				int ty = u.getTilePosition().getY();
				for (int wx = tx * 4; wx < (ty + uwidth) * 4; wx++) {
					for (int wy = tx * 4; wy < (ty + uheight) * 4; wy++) {
						walkableNodes.get(wx).get(wy).walkable = false;
					}
				}
			}
		}

		Queue<Node> openSet = new PriorityQueue<Node>(1,
				new Comparator<Node>() {
					@Override
					public int compare(Node n1, Node n2) {
						return (int) Math
								.round((n1.predictedTotalCost - n2.predictedTotalCost) * 100);
					}
				});
		walkableNodes.get(startWx).get(startWy).parent = null;
		walkableNodes.get(startWx).get(startWy).costFromStart = 0;
		walkableNodes.get(startWx).get(startWy).predictedTotalCost = Point
				.distance(startWx, startWy, endWx, endWy);
		openSet.add(walkableNodes.get(startWx).get(startWy));
		Set<Node> closedSet = new HashSet<Node>();

		// Iterate
		while (openSet.size() > 0) {
			Node currentNode = openSet.remove();
			// Base case
			if ((currentNode.x == endWx && currentNode.y == endWy)
					|| currentNode.costFromStart > length) {
				Deque<Point> path = new ArrayDeque<Point>();
				reconstructPath(path, currentNode);
				return path;
			}
			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(currentNode.x, currentNode.y)) {
				if (closedSet.contains(neighbor) || !neighbor.walkable) {
					continue;
				}

				double tentative_g_score = currentNode.costFromStart
						+ Point.distance(currentNode.x, currentNode.y,
								neighbor.x, neighbor.y);
				if (!openSet.contains(neighbor)
						|| tentative_g_score < neighbor.costFromStart) {
					neighbor.parent = currentNode;
					neighbor.costFromStart = tentative_g_score;
					neighbor.predictedTotalCost = tentative_g_score
							+ Point.distance(neighbor.x, neighbor.y, endWx,
									endWy);
					openSet.add(neighbor);
				}
			}
		}

		throw new NullPointerException();
	}

	private static List<Node> getNeighbors(int x, int y) {
		List<Node> neighbors = new ArrayList<Node>();

		// NORTH
		if (y + 1 < walkableNodes.get(x).size()) {
			neighbors.add(walkableNodes.get(x).get(y + 1));
			// NORTH-EAST
			if (x + 1 < walkableNodes.size()) {
				neighbors.add(walkableNodes.get(x + 1).get(y + 1));
			}
			// NORTH-WEST
			if (x - 1 >= 0) {
				neighbors.add(walkableNodes.get(x - 1).get(y + 1));
			}
		}
		// EAST
		if (x + 1 < walkableNodes.size()) {
			neighbors.add(walkableNodes.get(x + 1).get(y));
		}
		// SOUTH
		if (y - 1 >= 0) {
			neighbors.add(walkableNodes.get(x).get(y - 1));
			// SOUTH-EAST
			if (x + 1 < walkableNodes.size()) {
				neighbors.add(walkableNodes.get(x + 1).get(y - 1));
			}
			// SOUTH-WEST
			if (x - 1 >= 0) {
				neighbors.add(walkableNodes.get(x - 1).get(y - 1));
			}
		}
		// WEST
		if (x - 1 >= 0) {
			neighbors.add(walkableNodes.get(x - 1).get(y));
		}

		return neighbors;
	}

	private static Deque<Point> reconstructPath(Deque<Point> path,
			Node finalNode) {
		path.push(new Point(finalNode.x * 8 + 4, finalNode.y * 8 + 4));

		// Base case
		if (finalNode.parent == null) {
			return path;
		}
		return reconstructPath(path, finalNode.parent);
	}

	public static void registerDebugFunctions() {
		DebugModule chokeDM = DebugManager.createDebugModule("choke");
		// Label all chokes
		chokeDM.addSubmodule("draw").setDraw(
				() -> {
					int i = 0;
					for (Chokepoint choke : BWTA.getChokepoints()) {
						DrawEngine.drawTextMap(choke.getCenter().getX() - 10,
								choke.getCenter().getY() - 20, "Choke " + i);
						DrawEngine.drawTextMap(choke.getCenter().getX() - 10,
								choke.getCenter().getY() - 10, "Radius "
										+ choke.getWidth());
						i++;
					}
				});
		// Draw the path from the choke point into the main
		chokeDM.addSubmodule("path")
				.setDraw(
						() -> {
							for (Point location : pathIntoMain) {
								DrawEngine.drawBoxMap(location.x + 1,
										location.y + 1, location.x + 6,
										location.y + 6, Color.Grey, false);
							}
							DrawEngine.drawBoxMap(topOfRamp.x + 1,
									topOfRamp.y + 1, topOfRamp.x + 6,
									topOfRamp.y + 6, Color.Red, false);
						});
		// Highlight the tiles of the main ramp
		chokeDM.addSubmodule("ramp").setDraw(
				() -> {
					for (Point location : chokeRampWalkTiles) {
						DrawEngine.drawBoxMap(location.x * 8, location.y * 8,
								location.x * 8 + 8, location.y * 8 + 8,
								Color.Green, false);
					}
				});
	}
}
