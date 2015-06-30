package pathfinder;

import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;

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

import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.WalkPosition;
import bwta.BWTA;
import bwta.Chokepoint;
import datastructure.BaseManager;

public final class PathingManager {

	private static final int MAX_RAMP_WALK_TILES = 500;

	private static ArrayList<ArrayList<Node>> walkableNodes;
	private static int mapWalkWidth;
	private static int mapWalkHeight;

	// A list of tiles detailing a path into the main from the choke
	private static Queue<WalkPosition> pathIntoMain;
	private static WalkPosition topOfRamp;
	private static List<WalkPosition> chokeRampWalkTiles;

	public static void init() {
		System.out.print("Starting PathingManager... ");
		mapWalkWidth = GameHandler.getMapWalkWidth();
		mapWalkHeight = GameHandler.getMapWalkHeight();

		// Init walkable map
		walkableNodes = new ArrayList<ArrayList<Node>>();
		for (int wx = 0; wx < mapWalkWidth; wx++) {
			walkableNodes.add(new ArrayList<Node>());
			for (int wy = 0; wy < mapWalkHeight; wy++) {
				walkableNodes.get(wx).add(new Node(wx, wy));
			}
		}

		registerDebugFunctions();
		System.out.println("Success!");
	}

	/** This constructor should never be used. */
	private PathingManager() {
	}

	public static void findChokeToMain() {
		Chokepoint choke = BWTA.getNearestChokepoint(BaseManager.main
				.getLocation().getTilePosition());
		// Find the path into the main
		pathIntoMain = findGroundPath(choke.getCenter(), BaseManager.main
				.getLocation().getPosition(), UnitType.Zerg_Zergling);

		// Find top of ramp
		for (WalkPosition p : pathIntoMain) {
			if (GameHandler.isBuildable(p.getX() / 32, p.getY() / 32, false)) {
				topOfRamp = p;
				break;
			}
		}

		// Mark entire ramp
		chokeRampWalkTiles = new ArrayList<WalkPosition>();
		Queue<WalkPosition> rampWalkTileOpenSet = new PriorityQueue<>(1,
				new Comparator<WalkPosition>() {
					@Override
					public int compare(WalkPosition p1, WalkPosition p2) {
						WalkPosition topOfRampWalkTile = new WalkPosition(
								topOfRamp.getX() / 8, topOfRamp.getY() / 8);
						return (int) (p1.getApproxDistance(topOfRampWalkTile) - p2
								.getApproxDistance(topOfRampWalkTile));
					}
				});
		rampWalkTileOpenSet.add(new WalkPosition(choke.getCenter().getX() / 8,
				choke.getCenter().getY() / 8));
		while (!rampWalkTileOpenSet.isEmpty()) {
			WalkPosition currentNode = rampWalkTileOpenSet.poll();
			if (GameHandler.isWalkable(currentNode)
					&& !GameHandler.isBuildable(currentNode.getX() / 4,
							currentNode.getY() / 4, false)) {
				chokeRampWalkTiles.add(new WalkPosition(currentNode.getX(),
						currentNode.getY()));
				WalkPosition nextNode;
				nextNode = new WalkPosition(currentNode.getX() - 1,
						currentNode.getY());
				if (!chokeRampWalkTiles.contains(nextNode)
						&& !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new WalkPosition(currentNode.getX() + 1,
						currentNode.getY());
				if (!chokeRampWalkTiles.contains(nextNode)
						&& !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new WalkPosition(currentNode.getX(),
						currentNode.getY() - 1);
				if (!chokeRampWalkTiles.contains(nextNode)
						&& !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new WalkPosition(currentNode.getX(),
						currentNode.getY() + 1);
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

	private static Queue<WalkPosition> findGroundPath(Position start,
			Position end, UnitType unitType) {
		return findGroundPath(start.getX(), start.getY(), end.getX(),
				end.getY(), unitType);
	}

	public static Queue<WalkPosition> findGroundPath(int startx, int starty,
			int endx, int endy, UnitType unitType) {
		return findGroundPath(startx, starty, endx, endy, unitType,
				Integer.MAX_VALUE);
	}

	public static Queue<WalkPosition> findGroundPath(int startx, int starty,
			int endx, int endy, UnitType type, int length) {
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
				Deque<WalkPosition> path = new ArrayDeque<>();
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

	public static Queue<WalkPosition> findSafeAirPath(int startx, int starty,
			int endx, int endy, double[][] threatMap, int length) {
		int startWx = startx / 8;
		int startWy = starty / 8;
		int endWx = endx / 8;
		int endWy = endy / 8;

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
				Deque<WalkPosition> path = new ArrayDeque<>();
				reconstructPath(path, currentNode);
				return path;
			}
			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(currentNode.x, currentNode.y)) {
				if (closedSet.contains(neighbor)
						|| threatMap[currentNode.x / 4][currentNode.y / 4] > 0) {
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

	private static Deque<WalkPosition> reconstructPath(
			Deque<WalkPosition> path, Node finalNode) {
		path.push(new WalkPosition(finalNode.x * 8 + 4, finalNode.y * 8 + 4));

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
		chokeDM.addSubmodule("path").setDraw(
				() -> {
					for (WalkPosition location : pathIntoMain) {
						DrawEngine.drawBoxMap(location.getX() + 1,
								location.getY() + 1, location.getX() + 6,
								location.getY() + 6, Color.Grey, false);
					}
					DrawEngine.drawBoxMap(topOfRamp.getX() + 1,
							topOfRamp.getY() + 1, topOfRamp.getX() + 6,
							topOfRamp.getY() + 6, Color.Red, false);
				});
		// Highlight the tiles of the main ramp
		chokeDM.addSubmodule("ramp").setDraw(
				() -> {
					for (WalkPosition location : chokeRampWalkTiles) {
						DrawEngine.drawBoxMap(location.getX() * 8,
								location.getY() * 8, location.getX() * 8 + 8,
								location.getY() * 8 + 8, Color.Green, false);
					}
				});
	}
}