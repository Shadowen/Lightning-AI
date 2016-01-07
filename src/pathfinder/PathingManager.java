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
	private static Node[][] walkableNodes;
	private static int mapWalkWidth;
	private static int mapWalkHeight;

	public static void init() {
		System.out.print("Starting PathingManager... ");
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

		// findChokeToMain();
		System.out.println("Success!");
	}

	/** This constructor should never be used. */
	private PathingManager() {
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
		if (!GameHandler.isWalkable(wx, wy)
				|| GameHandler.getUnitsOnTile(wx / 4, wy / 4).stream().anyMatch(u -> u.getType().isBuilding())) {
			return 0;
		}
		// True clearance is one larger than the minimum of the three true
		// clearances below, to the right, and below-right
		int bottomLeft = wy + 1 < mapWalkHeight ? walkableNodes[wx][wy + 1].clearance : 0;
		int topRight = wx + 1 < mapWalkWidth ? walkableNodes[wx + 1][wy].clearance : 0;
		int bottomRight = wy + 1 < mapWalkHeight && wx + 1 < mapWalkWidth ? walkableNodes[wx + 1][wy + 1].clearance : 0;
		return Math.min(Math.min(bottomLeft, bottomRight), topRight) + 1;
	}

	/**
	 * Check if a {@link UnitType} fits into a given clearance
	 * 
	 * @param type
	 * @param clearance
	 * @return <b>true</b> if the unit does not fit, <b>false</b> otherwise.
	 */
	private static boolean unitDoesNotFit(UnitType type, int clearance) {
		// Unit size is in pixels, clearance is is walk-tiles
		// TODO use pixels to allow units to walk between buildings?
		if (clearance < Math.max((type.width() + 4) / 8, (type.height() + 4) / 8)) {
			return true;
		}
		return false;
	}

	public static Queue<Position> findGroundPath(Position start, Position end, UnitType unitType)
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
		Node startNode = walkableNodes[startWx][startWy];
		startNode.parent = null;
		startNode.costFromStart = 0;
		startNode.predictedTotalCost = Point.distance(startWx, startWy, endWx, endWy);
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

	private static List<Node> getNeighbors(int x, int y) {
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

	private static Deque<Position> reconstructPath(Deque<Position> path, Node finalNode, UnitType unitType) {
		path.push(new Position(finalNode.wx * 8 + unitType.width() / 2, finalNode.wy * 8 + unitType.height() / 2));

		// Base case
		if (finalNode.parent == null) {
			return path;
		}
		return reconstructPath(path, finalNode.parent, unitType);
	}

	private static Deque<Position> reconstructAirPath(Deque<Position> path, Node finalNode) {
		path.push(new Position(finalNode.wx * 8 + 4, finalNode.wy * 8 + 4));

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
						Node n = walkableNodes[wx][wy];
						if (n.clearance == 0) {
							DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 + 8, n.wy * 8 + 8, Color.Red, true);
						}
						// else if (n.clearance == 1) {
						// DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 +
						// 8, n.wy * 8 + 8, Color.Orange, true);
						// } else if (n.clearance == 2) {
						// DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 +
						// 8, n.wy * 8 + 8, Color.Yellow, true);
						// } else if (n.clearance == 3) {
						// DrawEngine.drawBoxMap(n.wx * 8, n.wy * 8, n.wx * 8 +
						// 8, n.wy * 8 + 8, Color.Green, true);
						// }
					}
				}
				Position mousePosition = GameHandler.getMousePositionOnMap();
				if (mousePosition.getX() / 8 < mapWalkWidth && mousePosition.getY() / 8 < mapWalkHeight) {
					Node thisCell = walkableNodes[mousePosition.getX() / 8][mousePosition.getY() / 8];
					DrawEngine.drawBoxMap(thisCell.wx * 8, thisCell.wy * 8, (thisCell.wx + thisCell.clearance) * 8,
							(thisCell.wy + thisCell.clearance) * 8, Color.Yellow, false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).setActive(true);
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
					}
				} catch (ShapeOverflowException s) {
					System.out.println("Shape overflow!");
				}
			});
		});
	}
}