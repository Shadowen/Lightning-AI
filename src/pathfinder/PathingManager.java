package pathfinder;

import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.ShapeOverflowException;

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
	private static Queue<Position> pathIntoMain = new ArrayDeque<>();
	private static WalkPosition topOfRamp;
	private static List<WalkPosition> chokeRampWalkTiles = new ArrayList<>();;

	// TODO
	private static Set<Node> latestClosedSet;

	public static void init() {
		System.out.print("Starting PathingManager... ");
		mapWalkWidth = GameHandler.getMapWalkWidth();
		mapWalkHeight = GameHandler.getMapWalkHeight();

		// Init walkable map
		walkableNodes = new ArrayList<ArrayList<Node>>(mapWalkWidth);
		for (int wx = 0; wx < mapWalkWidth; wx++) {
			walkableNodes.add(new ArrayList<Node>(mapWalkHeight));
			for (int wy = 0; wy < mapWalkHeight; wy++) {
				walkableNodes.get(wx).add(new Node(wx, wy));
			}
		}
		final long startTime = System.currentTimeMillis();
		refreshWalkableMap();
		final long endTime = System.currentTimeMillis();
		System.out.println("Total execution time: " + (endTime - startTime) + "ms");

		registerDebugFunctions();

		// findChokeToMain();
		System.out.println("Success!");

	}

	/** This constructor should never be used. */
	private PathingManager() {
	}

	public static void refreshWalkableMap() {
		// Infinity-norm
		for (int d = Math.max(mapWalkHeight, mapWalkWidth) - 1; d >= 0; d--) {
			// Right to left across the bottom: (wx, d)
			for (int wx = d; wx >= 0; wx--) {
				walkableNodes.get(wx).get(d).clearance = getTrueClearance(wx, d);
			}
			// Up the right side: (d, wy)
			for (int wy = d - 1; wy >= 0; wy--) {
				walkableNodes.get(d).get(wy).clearance = getTrueClearance(d, wy);
			}
		}
	}

	/**
	 * Finds the true clearance for a certain walk tile
	 **/
	private static int getTrueClearance(int wx, int wy) {
		// Current tile is not walkable
		if (!GameHandler.isWalkable(wx, wy)) {
			return 0;
		}
		int bottomLeft = wy + 1 < mapWalkHeight ? walkableNodes.get(wx).get(wy + 1).clearance : 0;
		int topRight = wx + 1 < mapWalkWidth ? walkableNodes.get(wx + 1).get(wy).clearance : 0;
		int bottomRight = wy + 1 < mapWalkHeight && wx + 1 < mapWalkWidth
				? walkableNodes.get(wx + 1).get(wy + 1).clearance : 0;
		return Math.min(Math.min(bottomLeft, bottomRight), topRight) + 1;
	}

	public static void findChokeToMain() throws NoPathFoundException {
		Chokepoint choke = BWTA.getNearestChokepoint(BaseManager.main.getLocation().getTilePosition());
		// Find the path into the main
		pathIntoMain = findGroundPath(choke.getCenter(), BaseManager.main.getLocation().getPosition(),
				UnitType.Zerg_Zergling);

		// Find top of ramp
		for (Position p : pathIntoMain) {
			if (GameHandler.isBuildable(p.getX() / 32, p.getY() / 32, false)) {
				topOfRamp = new WalkPosition(p.getX() / 8, p.getY() / 8);
				break;
			}
		}

		// Mark entire ramp
		chokeRampWalkTiles = new ArrayList<WalkPosition>();
		Queue<WalkPosition> rampWalkTileOpenSet = new PriorityQueue<>(1, new Comparator<WalkPosition>() {
			@Override
			public int compare(WalkPosition p1, WalkPosition p2) {
				WalkPosition topOfRampWalkTile = new WalkPosition(topOfRamp.getX() / 8, topOfRamp.getY() / 8);
				return (int) (p1.getApproxDistance(topOfRampWalkTile) - p2.getApproxDistance(topOfRampWalkTile));
			}
		});
		rampWalkTileOpenSet.add(new WalkPosition(choke.getCenter().getX() / 8, choke.getCenter().getY() / 8));
		while (!rampWalkTileOpenSet.isEmpty()) {
			WalkPosition currentNode = rampWalkTileOpenSet.poll();
			if (GameHandler.isWalkable(currentNode)
					&& !GameHandler.isBuildable(currentNode.getX() / 4, currentNode.getY() / 4, false)) {
				chokeRampWalkTiles.add(new WalkPosition(currentNode.getX(), currentNode.getY()));
				WalkPosition nextNode;
				nextNode = new WalkPosition(currentNode.getX() - 1, currentNode.getY());
				if (!chokeRampWalkTiles.contains(nextNode) && !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new WalkPosition(currentNode.getX() + 1, currentNode.getY());
				if (!chokeRampWalkTiles.contains(nextNode) && !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new WalkPosition(currentNode.getX(), currentNode.getY() - 1);
				if (!chokeRampWalkTiles.contains(nextNode) && !rampWalkTileOpenSet.contains(nextNode)) {
					rampWalkTileOpenSet.add(nextNode);
				}
				nextNode = new WalkPosition(currentNode.getX(), currentNode.getY() + 1);
				if (!chokeRampWalkTiles.contains(nextNode) && !rampWalkTileOpenSet.contains(nextNode)) {
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

	private static boolean unitDoesNotFit(UnitType type, int clearance) {
		// Unit size is in pixels, clearance is is walk-tiles
		// TODO use pixels to allow units to walk between buildings?
		if (clearance < Math.max((type.width() + 4) / 8, (type.height() + 4) / 8)) {
			return true;
		}
		return false;
	}

	private static Queue<Position> findGroundPath(Position start, Position end, UnitType unitType)
			throws NoPathFoundException {
		return findGroundPath(start.getX(), start.getY(), end.getX(), end.getY(), unitType);
	}

	public static Queue<Position> findGroundPath(int startx, int starty, int endx, int endy, UnitType unitType)
			throws NoPathFoundException {
		return findGroundPath(startx, starty, endx, endy, unitType, Integer.MAX_VALUE);
	}

	public static Queue<Position> findGroundPath(int startx, int starty, int endx, int endy, UnitType unitType,
			int maxLength) throws NoPathFoundException {
		int startWx = (startx - unitType.width() / 2) / 8;
		int startWy = (starty - unitType.height() / 2) / 8;
		int endWx = (endx - unitType.width() / 2) / 8;
		int endWy = (endy - unitType.height() / 2) / 8;

		Queue<Node> openSet = new PriorityQueue<Node>(1, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return (int) Math.round((n1.predictedTotalCost - n2.predictedTotalCost) * 100);
			}
		});
		walkableNodes.get(startWx).get(startWy).parent = null;
		walkableNodes.get(startWx).get(startWy).costFromStart = 0;
		walkableNodes.get(startWx).get(startWy).predictedTotalCost = Point.distance(startWx, startWy, endWx, endWy);
		openSet.add(walkableNodes.get(startWx).get(startWy));
		Set<Node> closedSet = new HashSet<Node>();

		// Iterate
		while (openSet.size() > 0) {
			Node currentNode = openSet.remove();
			// Base case
			if ((currentNode.x == endWx && currentNode.y == endWy) || currentNode.costFromStart > maxLength) {
				Deque<Position> path = new ArrayDeque<>();
				reconstructPath(path, currentNode, unitType);
				return path;
			}
			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(currentNode.x, currentNode.y)) {
				if (closedSet.contains(neighbor) || unitDoesNotFit(unitType, neighbor.clearance)) {
					continue;
				}

				double tentative_g_score = currentNode.costFromStart
						+ Point.distance(currentNode.x, currentNode.y, neighbor.x, neighbor.y);
				if (!openSet.contains(neighbor) || tentative_g_score < neighbor.costFromStart) {
					neighbor.parent = currentNode;
					neighbor.costFromStart = tentative_g_score;
					neighbor.predictedTotalCost = tentative_g_score
							+ Point.distance(neighbor.x, neighbor.y, endWx, endWy);
					openSet.add(neighbor);
				}
			}
		}

		latestClosedSet = closedSet;
		throw new NoPathFoundException();
	}

	public static Queue<Position> findSafeAirPath(int startx, int starty, int endx, int endy, double[][] threatMap,
			int length) {
		int startWx = startx / 8;
		int startWy = starty / 8;
		int endWx = endx / 8;
		int endWy = endy / 8;

		Queue<Node> openSet = new PriorityQueue<Node>(1, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return (int) Math.round((n1.predictedTotalCost - n2.predictedTotalCost) * 100);
			}
		});
		walkableNodes.get(startWx).get(startWy).parent = null;
		walkableNodes.get(startWx).get(startWy).costFromStart = 0;
		walkableNodes.get(startWx).get(startWy).predictedTotalCost = Point.distance(startWx, startWy, endWx, endWy);
		openSet.add(walkableNodes.get(startWx).get(startWy));
		Set<Node> closedSet = new HashSet<Node>();

		// Iterate
		while (openSet.size() > 0) {
			Node currentNode = openSet.remove();
			// Base case
			if ((currentNode.x == endWx && currentNode.y == endWy) || currentNode.costFromStart > length) {
				Deque<Position> path = new ArrayDeque<>();
				reconstructAirPath(path, currentNode);
				return path;
			}
			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(currentNode.x, currentNode.y)) {
				if (closedSet.contains(neighbor) || threatMap[currentNode.x / 4][currentNode.y / 4] > 0) {
					continue;
				}

				double tentative_g_score = currentNode.costFromStart
						+ Point.distance(currentNode.x, currentNode.y, neighbor.x, neighbor.y);
				if (!openSet.contains(neighbor) || tentative_g_score < neighbor.costFromStart) {
					neighbor.parent = currentNode;
					neighbor.costFromStart = tentative_g_score;
					neighbor.predictedTotalCost = tentative_g_score
							+ Point.distance(neighbor.x, neighbor.y, endWx, endWy);
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

	private static Deque<Position> reconstructPath(Deque<Position> path, Node finalNode, UnitType unitType) {
		path.push(new Position(finalNode.x * 8 + unitType.width() / 2, finalNode.y * 8 + unitType.height() / 2));

		// Base case
		if (finalNode.parent == null) {
			return path;
		}
		return reconstructPath(path, finalNode.parent, unitType);
	}

	private static Deque<Position> reconstructAirPath(Deque<Position> path, Node finalNode) {
		path.push(new Position(finalNode.x * 8 + 4, finalNode.y * 8 + 4));

		// Base case
		if (finalNode.parent == null) {
			return path;
		}
		return reconstructAirPath(path, finalNode.parent);
	}

	public static void registerDebugFunctions() {
		// Clearance values
		DebugManager.createDebugModule("clearance").setDraw(() -> {
			try {
				// Show clearance values
				for (int wx = 0; wx < mapWalkWidth; wx++) {
					for (int wy = 0; wy < mapWalkHeight; wy++) {
						Node n = walkableNodes.get(wx).get(wy);
						if (n.clearance == 0) {
							DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8, Color.Red, true);
						} else if (n.clearance == 1) {
							DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8, Color.Orange, true);
						} else if (n.clearance == 2) {
							DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8, Color.Yellow, true);
						} else if (n.clearance == 3) {
							DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8, Color.Green, true);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		DebugManager.createDebugModule("pathing").setDraw(() -> {
			// Projected paths
			GameHandler.getSelectedUnits().stream().forEach(u -> {
				try {
					try {
						Queue<Position> path = PathingManager.findGroundPath(u.getPosition(),
								GameHandler.getMousePositionOnMap(), u.getType());
						for (Position w : path) {
							DrawEngine.drawBoxMap(w.getX() - 2, w.getY() - 2, w.getX() + 2, w.getY() + 2, Color.Cyan,
									false);
						}
					} catch (NoPathFoundException e) {
						for (Node w : latestClosedSet) {
							DrawEngine.drawBoxMap(w.x * 8, w.y * 8, w.x * 8 + 8, w.y * 8 + 8, Color.Red, false);
						}
					}
				} catch (ShapeOverflowException s) {
					System.out.println("Shape overflow!");
				}
			});
		});

		DebugModule chokeDM = DebugManager.createDebugModule("choke");
		// Label all chokes
		chokeDM.addSubmodule("draw").setDraw(() -> {
			int i = 0;
			for (Chokepoint choke : BWTA.getChokepoints()) {
				DrawEngine.drawTextMap(choke.getCenter().getX() - 10, choke.getCenter().getY() - 20, "Choke " + i);
				DrawEngine.drawTextMap(choke.getCenter().getX() - 10, choke.getCenter().getY() - 10,
						"Radius " + choke.getWidth());
				i++;
			}
		});
		// Draw the path from the choke point into the main
		// chokeDM.addSubmodule("path").setDraw(
		// () -> {
		// for (WalkPosition location : pathIntoMain) {
		// DrawEngine.drawBoxMap(location.getX() + 1,
		// location.getY() + 1, location.getX() + 6,
		// location.getY() + 6, Color.Grey, false);
		// }
		// DrawEngine.drawBoxMap(topOfRamp.getX() + 1,
		// topOfRamp.getY() + 1, topOfRamp.getX() + 6,
		// topOfRamp.getY() + 6, Color.Red, false);
		// });
		// Highlight the tiles of the main ramp
		// chokeDM.addSubmodule("ramp").setDraw(
		// () -> {
		// for (WalkPosition location : chokeRampWalkTiles) {
		// DrawEngine.drawBoxMap(location.getX() * 8,
		// location.getY() * 8, location.getX() * 8 + 8,
		// location.getY() * 8 + 8, Color.Green, false);
		// }
		// });
		// DebugManager.createDebugModule("walkable").setDraw(() -> {
		// for (int wx = 0; wx < mapWalkWidth; wx++) {
		// for (int wy = 0; wy < mapWalkHeight; wy++) {
		// Node n = walkableNodes.get(wx).get(wy);
		// DrawEngine.drawXMap(n.x * 8, n.y * 8, n.walkable ? Color.Green :
		// Color.Red);
		// }
		// }
		// });
	}
}