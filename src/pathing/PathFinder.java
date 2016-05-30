package pathing;

import java.awt.Point;
import java.awt.Rectangle;
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
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.Chokepoint;
import bwta.Region;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.ShapeOverflowException;

public final class PathFinder {
	/**
	 * The furthest distance to look for a walkable tile when a ground unit
	 * appears to be on unwalkable terrain. This is necessary because some
	 * buildings have walkable edges that are marked as impassable.
	 */
	public static final int MAX_WALKABLE_RANGE = 20;

	public static Node[][] walkableNodes;
	private static int mapWalkWidth;
	private static int mapWalkHeight;

	public static void init() {
		System.out.print("Starting PathingFinder... ");
		mapWalkWidth = GameHandler.getMapWalkWidth();
		mapWalkHeight = GameHandler.getMapWalkHeight();

		// Init walkable map
		walkableNodes = new Node[mapWalkWidth][mapWalkHeight];
		for (int wx = 0; wx < mapWalkWidth; wx++) {
			walkableNodes[wx] = new Node[mapWalkHeight];
			for (int wy = 0; wy < mapWalkHeight; wy++) {
				walkableNodes[wx][wy] = new Node(wx, wy);
			}
		}
		refreshWalkableMap();

		registerDebugFunctions();

		System.out.println("Success!");
	}

	/** This constructor should never be used. */
	private PathFinder() {
	}

	public static void refreshWalkableMap() {
		// Evaluate nodes in reverse infinity-norm distance order
		for (int d = Math.max(mapWalkHeight, mapWalkWidth) - 1; d >= 0; d--) {
			// Need to expand diagonally back towards the origin
			int width = Math.min(mapWalkWidth - 1, d);
			int height = Math.min(mapWalkHeight - 1, d);
			// Right to left across the bottom: (wx, d)
			for (int wx = width; wx >= 0; wx--) {
				walkableNodes[wx][height].clearance = getTrueClearance(wx, d);
			}
			// Bottom to top up the right side: (d, wy)
			for (int wy = height; wy >= 0; wy--) {
				walkableNodes[width][wy].clearance = getTrueClearance(d, wy);
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
		// True clearance is one larger than the minimum of the three true
		// clearances below, to the right, and below-right
		int bottomLeft = wy + 1 < mapWalkHeight ? walkableNodes[wx][wy + 1].clearance : 0;
		int topRight = wx + 1 < mapWalkWidth ? walkableNodes[wx + 1][wy].clearance : 0;
		int bottomRight = wy + 1 < mapWalkHeight && wx + 1 < mapWalkWidth ? walkableNodes[wx + 1][wy + 1].clearance : 0;
		return Math.min(Math.min(bottomLeft, bottomRight), topRight) + 1;
	}

	public static void onBuildingCreate(Unit building) {
		Set<Node> zeroMe = new HashSet<>();
		Queue<Node> toRecalculate = new ArrayDeque<>();
		// Add the building to the walkable map
		TilePosition tp = building.getTilePosition();
		for (int wx = tp.getX() * 4; wx < (tp.getX() + building.getType().tileWidth()) * 4; wx++) {
			for (int wy = tp.getY() * 4; wy < (tp.getY() + building.getType().tileHeight()) * 4; wy++) {
				zeroMe.add(walkableNodes[wx][wy]);
				toRecalculate.add(walkableNodes[wx][wy]);
			}
		}

		while (!toRecalculate.isEmpty()) {
			Node current = toRecalculate.remove();
			int oldClearance = current.clearance;
			// Current tile is not walkable
			if (zeroMe.contains(current)) {
				current.clearance = 0;
			} else {
				// Otherwise, true clearance is one larger than the minimum of
				// the three true clearances below, to the right, and
				// below-right, or itself if clearance was zero
				int bottomLeft = current.wy + 1 < mapWalkHeight ? walkableNodes[current.wx][current.wy + 1].clearance
						: 0;
				int topRight = current.wx + 1 < mapWalkWidth ? walkableNodes[current.wx + 1][current.wy].clearance : 0;
				int bottomRight = current.wy + 1 < mapWalkHeight && current.wx + 1 < mapWalkWidth
						? walkableNodes[current.wx + 1][current.wy + 1].clearance : 0;
				current.clearance = Math.min(current.clearance,
						Math.min(Math.min(bottomLeft, bottomRight), topRight) + 1);
			}
			// If the clearance value has changed,
			if (current.clearance != oldClearance) {
				if (current.wx - 1 > 0) {
					toRecalculate.add(walkableNodes[current.wx - 1][current.wy]);
				}
				if (current.wy > 0) {
					toRecalculate.add(walkableNodes[current.wx][current.wy - 1]);
				}
				if (current.wx > 0 && current.wy > 0) {
					toRecalculate.add(walkableNodes[current.wx - 1][current.wy - 1]);
				}
			}
		}
	}

	/**
	 * Check if a {@link UnitType} fits into a given clearance
	 * 
	 * @param type
	 * @param clearance
	 * @return <b>true</b> if the unit does not fit, <b>false</b> otherwise.
	 */
	public static boolean unitDoesNotFit(UnitType type, int clearance) {
		// Unit size is in pixels, clearance is is walk-tiles
		// TODO use pixels to allow units to walk between buildings?
		if (clearance < Math.max((type.width() + 4) / 8, (type.height() + 4) / 8)) {
			return true;
		}
		return false;
	}

	public static Deque<Position> findGroundPath(Unit unit, Rectangle boundingBox, int maxLength)
			throws NoPathFoundException, InvalidStartNodeException {
		return findGroundPath(unit.getX(), unit.getY(), boundingBox, unit.getType(), maxLength);
	}

	public static Deque<Position> findGroundPath(Unit u, Position end)
			throws NoPathFoundException, InvalidStartNodeException {
		return findGroundPath(u.getX(), u.getY(), end.getX(), end.getY(), u.getType());
	}

	public static Deque<Position> findGroundPath(Unit u, Position end, int maxLength)
			throws NoPathFoundException, InvalidStartNodeException {
		return findGroundPath(u.getX(), u.getY(), end.getX(), end.getY(), u.getType(), maxLength);
	}

	public static Deque<Position> findGroundPath(Position start, Position end, UnitType unitType)
			throws NoPathFoundException, InvalidStartNodeException {
		return findGroundPath(start.getX(), start.getY(), end.getX(), end.getY(), unitType);
	}

	public static Deque<Position> findGroundPath(int startx, int starty, int endx, int endy, UnitType unitType)
			throws NoPathFoundException, InvalidStartNodeException {
		return findGroundPath(startx, starty, endx, endy, unitType, Integer.MAX_VALUE);
	}

	public static Deque<Position> findGroundPath(int startx, int starty, int endx, int endy, UnitType unitType,
			int maxLength) throws NoPathFoundException, InvalidStartNodeException {
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
		// Find the closest walkable node
		Node startNode = null;
		distanceLoop: for (int d = 0; d < MAX_WALKABLE_RANGE; d++) {
			for (int x = 0; x <= d; x++) {
				if (!unitDoesNotFit(unitType, walkableNodes[startWx + x][startWy + d].clearance)) {
					startNode = walkableNodes[startWx + x][startWy + d];
					break distanceLoop;
				}
				if (!unitDoesNotFit(unitType, walkableNodes[startWx - x][startWy + d].clearance)) {
					startNode = walkableNodes[startWx - x][startWy + d];
					break distanceLoop;
				}
			}
			for (int y = 0; y <= d; y++) {
				if (!unitDoesNotFit(unitType, walkableNodes[startWx + d][startWy + y].clearance)) {
					startNode = walkableNodes[startWx + d][startWy + y];
					break distanceLoop;
				}
				if (!unitDoesNotFit(unitType, walkableNodes[startWx + d][startWy - y].clearance)) {
					startNode = walkableNodes[startWx + d][startWy - y];
					break distanceLoop;
				}
			}
		}
		if (startNode == null) {
			throw new InvalidStartNodeException();
		}
		startNode.parent = null;
		startNode.costFromStart = 0;
		openSet.add(startNode);
		Set<Node> closedSet = new HashSet<Node>();

		// Iterate
		while (openSet.size() > 0) {
			Node currentNode = openSet.remove();
			// Base case
			if ((currentNode.wx == endWx && currentNode.wy == endWy) || currentNode.costFromStart > maxLength) {
				Deque<Position> path = new ArrayDeque<>();
				reconstructPath(path, currentNode, unitType);
				return path;
			}
			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(currentNode.wx, currentNode.wy)) {
				if (closedSet.contains(neighbor) || unitDoesNotFit(unitType, neighbor.clearance)) {
					continue;
				}

				double tentative_g_score = currentNode.costFromStart
						+ Point.distance(currentNode.wx, currentNode.wy, neighbor.wx, neighbor.wy);
				if (!openSet.contains(neighbor) || tentative_g_score < neighbor.costFromStart) {
					neighbor.parent = currentNode;
					neighbor.costFromStart = tentative_g_score;
					neighbor.predictedTotalCost = tentative_g_score
							+ Point.distance(neighbor.wx, neighbor.wy, endWx, endWy);
					openSet.add(neighbor);
				}
			}
		}
		throw new NoPathFoundException();
	}

	/**
	 * If the starting position is "unwalkable" to ground units, the nearest
	 * valid location is chosen instead.
	 * 
	 * @param startx
	 *            ending location in pixels
	 * @param starty
	 *            starting location in pixels
	 * @param destination
	 *            a destination rectangle, coordinates in pixels
	 * @param unitType
	 *            the unit type used to determine clearances
	 * @param maxLength
	 *            the length of the path at which to terminate the search
	 * @return
	 * @throws NoPathFoundException
	 *             if no path can be found
	 * @throws InvalidStartNodeException
	 *             if the start node is too far from any valid ground position
	 *             for this unit
	 */
	public static Deque<Position> findGroundPath(int startx, int starty, Rectangle destination, UnitType unitType,
			int maxLength) throws NoPathFoundException, InvalidStartNodeException {
		int startWx = startx / 8;
		int startWy = starty / 8;

		Queue<Node> openSet = new PriorityQueue<Node>(1, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return (int) Math.round((n1.predictedTotalCost - n2.predictedTotalCost) * 100);
			}
		});
		// Find the closest walkable node
		Node startNode = findClosestWalkableNode(startWx, startWy, unitType);
		startNode.parent = null;
		startNode.costFromStart = 0;
		openSet.add(startNode);
		Set<Node> closedSet = new HashSet<Node>();

		// Iterate
		while (openSet.size() > 0) {
			Node currentNode = openSet.remove();

			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(currentNode.wx, currentNode.wy)) {
				double tentative_g_score = currentNode.costFromStart
						+ Point.distance(currentNode.wx, currentNode.wy, neighbor.wx, neighbor.wy);
				// Base case
				if (destination.contains(neighbor.wx * 8, neighbor.wy * 8) || tentative_g_score > maxLength) {
					Deque<Position> path = new ArrayDeque<>();
					reconstructPath(path, currentNode, unitType);
					return path;
				}
				if (closedSet.contains(neighbor) || unitDoesNotFit(unitType, neighbor.clearance)) {
					continue;
				}
				if (!openSet.contains(neighbor) || tentative_g_score < neighbor.costFromStart) {
					neighbor.parent = currentNode;
					neighbor.costFromStart = tentative_g_score;
					neighbor.predictedTotalCost = tentative_g_score
							+ Point.distance(neighbor.wx, neighbor.wy, destination.getX() + destination.getWidth() / 2,
									destination.getY() + destination.getHeight() / 2);
					openSet.add(neighbor);
				}
			}
		}
		throw new NoPathFoundException();
	}

	// public static Queue<Position> findSafeAirPath(int startx, int starty,
	// double[][] threatMap, int length) {
	// int startWx = startx / 8;
	// int startWy = starty / 8;
	//
	// Queue<Node> openSet = new PriorityQueue<Node>(1, new Comparator<Node>() {
	// @Override
	// public int compare(Node n1, Node n2) {
	// return (int) Math.round((n1.predictedTotalCost - n2.predictedTotalCost) *
	// 100);
	// }
	// });
	// Node currentNode = walkableNodes.get(startWx).get(startWy);
	// currentNode.parent = null;
	// currentNode.costFromStart = 0;
	// currentNode.distanceFromStart = 0;
	// currentNode.predictedTotalCost = threatMap[startWx / 4][startWy / 4];
	// openSet.add(currentNode);
	// Set<Node> closedSet = new HashSet<Node>();
	//
	// // Iterate
	// while (currentNode.distanceFromStart < length) {
	// currentNode = openSet.remove();
	// // Move the node from the open set to the closed set
	// closedSet.add(currentNode);
	// // Add all neigbors to the open set
	// for (Node neighbor : getNeighbors(currentNode.wx, currentNode.wy)) {
	// if (closedSet.contains(neighbor)) {
	// continue;
	// }
	//
	// double tentative_g_score = currentNode.costFromStart
	// + Point.distance(currentNode.wx, currentNode.wy, neighbor.wx,
	// neighbor.wy);
	// if (!openSet.contains(neighbor) || tentative_g_score <
	// neighbor.costFromStart) {
	// neighbor.parent = currentNode;
	// neighbor.costFromStart = tentative_g_score;
	// neighbor.predictedTotalCost = tentative_g_score + threatMap[neighbor.wx /
	// 4][neighbor.wy / 4];
	// neighbor.distanceFromStart = currentNode.distanceFromStart + 1;
	// openSet.add(neighbor);
	// }
	// }
	// }
	//
	// Deque<Position> path = new ArrayDeque<>();
	// reconstructAirPath(path, currentNode);
	// return path;
	// }

	public static List<Node> getNeighbors(int x, int y) {
		List<Node> neighbors = new ArrayList<Node>();

		// NORTH
		if (y + 1 < walkableNodes[x].length) {
			neighbors.add(walkableNodes[x][y + 1]);
			// NORTH-EAST
			if (x + 1 < walkableNodes.length) {
				neighbors.add(walkableNodes[x + 1][y + 1]);
			}
			// NORTH-WEST
			if (x - 1 >= 0) {
				neighbors.add(walkableNodes[x - 1][y + 1]);
			}
		}
		// EAST
		if (x + 1 < walkableNodes.length) {
			neighbors.add(walkableNodes[x + 1][y]);
		}
		// SOUTH
		if (y - 1 >= 0) {
			neighbors.add(walkableNodes[x][y - 1]);
			// SOUTH-EAST
			if (x + 1 < walkableNodes.length) {
				neighbors.add(walkableNodes[x + 1][y - 1]);
			}
			// SOUTH-WEST
			if (x - 1 >= 0) {
				neighbors.add(walkableNodes[x - 1][y - 1]);
			}
		}
		// WEST
		if (x - 1 >= 0) {
			neighbors.add(walkableNodes[x - 1][y]);
		}

		return neighbors;
	}

	public static Deque<Position> reconstructPath(Deque<Position> path, Node finalNode, UnitType unitType) {
		path.push(new Position(finalNode.wx * 8 + unitType.width() / 2, finalNode.wy * 8 + unitType.height() / 2));

		Node n = finalNode;
		do {
			path.push(new Position(n.wx * 8 + unitType.width() / 2, n.wy * 8 + unitType.height() / 2));
			n = n.parent;
		} while (n != null);
		return path;
	}

	public static void registerDebugFunctions() {
		// // Clearance values
		// DebugManager.createDebugModule("clearance").setDraw(() -> {
		// try {
		// // Show clearance values
		// for (int wx = 0; wx < mapWalkWidth; wx++) {
		// for (int wy = 0; wy < mapWalkHeight; wy++) {
		// Node n = walkableNodes[wx][wy];
		// if (n.clearance == 0) {
		// DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 + 8, n.wy * 8 + 8,
		// Color.Red, true);
		// } else if (n.clearance == 1) {
		// DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 + 8, n.wy * 8 + 8,
		// Color.Orange, true);
		// }
		// // else if (n.clearance == 2) {
		// // DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 +
		// // 8, n.wy * 8 + 8, Color.Yellow, true);
		// // } else if (n.clearance == 3) {
		// // DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 +
		// // 8, n.wy * 8 + 8, Color.Green, true);
		// // }
		// }
		// }
		// Position mousePosition = GameHandler.getMousePositionOnMap();
		// if (mousePosition.getX() / 8 < mapWalkWidth && mousePosition.getY() /
		// 8 < mapWalkHeight) {
		// Node thisCell = walkableNodes[mousePosition.getX() /
		// 8][mousePosition.getY() / 8];
		// DrawEngine.drawBoxMap(thisCell.wx * 8, thisCell.wy * 8, (thisCell.wx
		// + thisCell.clearance) * 8,
		// (thisCell.wy + thisCell.clearance) * 8, Color.Yellow, false);
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// });
		// DebugManager.createDebugModule("pathing").setDraw(() -> {
		// // Projected paths
		// GameHandler.getSelectedUnits().stream().forEach(u -> {
		// try {
		// try {
		// Queue<Position> path = PathFinder.findGroundPath(u.getPosition(),
		// GameHandler.getMousePositionOnMap(), u.getType());
		// for (Position w : path) {
		// DrawEngine.drawBoxMap(w.getX() - 2, w.getY() - 2, w.getX() + 2,
		// w.getY() + 2, Color.Cyan,
		// false);
		// }
		// } catch (NoPathFoundException e) {
		// System.err.println(e);
		// } catch (InvalidStartNodeException e2) {
		// System.err.println(e2);
		// }
		// } catch (ShapeOverflowException s) {
		// System.out.println("Shape overflow!");
		// }
		// });
		// }).setActive(true);

		// Regions
		DebugManager.createDebugModule("regions").setDraw(() -> {
			List<Region> regions = BWTA.getRegions();
			for (int r = 0; r < regions.size(); r++) {
				final Region region = regions.get(r);
				// Draw name
				DrawEngine.drawTextMap(region.getCenter().getX(), region.getCenter().getY(), "Region " + r);
				// Draw outline
				final List<Position> points = region.getPolygon().getPoints();
				final int numVertices = points.size();
				for (int p = 0; p < numVertices - 1; p++) {
					Position p1 = points.get(p);
					Position p2 = points.get(p + 1);
					DrawEngine.drawLineMap(p1, p2, Color.White);
				}
				// Close the shape
				if (numVertices > 2) {
					Position p1 = points.get(points.size() - 1);
					Position p2 = points.get(0);
					DrawEngine.drawLineMap(p1, p2, Color.White);
				}

				// Draw connections
				for (Chokepoint choke : region.getChokepoints()) {
					DrawEngine.drawLineMap(region.getCenter(), choke.getCenter(), Color.Yellow);
				}
				// for (Chokepoint choke : region.getChokepoints()) {
				// Region neighbor = choke.getRegions().first != region ?
				// choke.getRegions().first
				// : choke.getRegions().second;
				// DrawEngine.drawLineMap(region.getCenter(),
				// neighbor.getCenter(), Color.Yellow);
				// }
			}
		}).setActive(true);
	}

	public static Node findClosestWalkableNode(int wx, int wy, UnitType unitType) throws InvalidStartNodeException {
		for (int d = 0; d < PathFinder.MAX_WALKABLE_RANGE; d++) {
			if (wy + d < GameHandler.getMapWalkHeight()) {
				for (int x = 0; x <= d; x++) {
					if (wx + x < GameHandler.getMapWidth() && !PathFinder.unitDoesNotFit(unitType,
							PathFinder.walkableNodes[wx + x][wy + d].clearance)) {
						return walkableNodes[wx + x][wy + d];
					}
					if (wx - x >= 0 && !PathFinder.unitDoesNotFit(unitType,
							PathFinder.walkableNodes[wx - x][wy + d].clearance)) {
						return walkableNodes[wx - x][wy + d];
					}
				}
			}
			if (wx + d < GameHandler.getMapWalkWidth()) {
				for (int y = 0; y <= d; y++) {
					if (wy + y < GameHandler.getMapWalkHeight() && !PathFinder.unitDoesNotFit(unitType,
							PathFinder.walkableNodes[wx + d][wy + y].clearance)) {
						return walkableNodes[wx + d][wy + y];
					}
					if (wy - y >= 0 && !PathFinder.unitDoesNotFit(unitType,
							PathFinder.walkableNodes[wx + d][wy - y].clearance)) {
						return walkableNodes[wx + d][wy - y];
					}
				}
			}
		}

		throw new InvalidStartNodeException();
	}
}