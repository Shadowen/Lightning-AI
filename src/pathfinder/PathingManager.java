package pathfinder;

import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.ShapeOverflowException;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

import bwapi.Color;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public final class PathingManager {
	private static WalkNode[][] walkableNodes;
	private static int mapWalkWidth;
	private static int mapWalkHeight;

	public static void init() {
		System.out.print("Starting PathingManager... ");
		mapWalkWidth = GameHandler.getMapWalkWidth();
		mapWalkHeight = GameHandler.getMapWalkHeight();

		// Init walkable map
		walkableNodes = new WalkNode[mapWalkWidth][mapWalkHeight];
		for (int wx = 0; wx < mapWalkWidth; wx++) {
			walkableNodes[wx] = new WalkNode[mapWalkHeight];
			for (int wy = 0; wy < mapWalkHeight; wy++) {
				walkableNodes[wx][wy] = new WalkNode8(new WalkNodeData(wx * 8, wy * 8, 8));
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
				walkableNodes[wx][height].setClearance(getTrueClearance(wx, d));
			}
			// Bottom to top up the right side: (d, wy)
			for (int wy = height; wy >= 0; wy--) {
				walkableNodes[width][wy].setClearance(getTrueClearance(d, wy));
			}
		}

		// Buildings
		for (Unit u : GameHandler.getAllGroundedBuildings()) {
			UnitType type = u.getType();
			Position p = u.getPosition();
			TilePosition tp = u.getTilePosition();
			// Interior
			for (int wx = tp.getX() * 4 + 1; wx < (tp.getX() + u.getType().tileWidth()) * 4 - 1; wx++) {
				for (int wy = tp.getY() * 4 + 1; wy < (tp.getY() + u.getType().tileHeight()) * 4 - 1; wy++) {
					walkableNodes[wx][wy].getCell(0, 0).clearance = 0;
				}
			}
			// Left and Right
			int leftWx = tp.getX() * 4;
			int rightWx = (tp.getX() + u.getType().tileWidth()) * 4 - 1;
			int leftClearance = type.tileWidth() * 32 / 2 - type.dimensionLeft();
			int rightClearance = type.tileWidth() * 32 / 2 - type.dimensionRight() - 1;
			for (int wy = tp.getY() * 4; wy < (tp.getY() + u.getType().tileHeight()) * 4; wy++) {
				// Left
				for (int wx = leftWx; wx < leftWx + (leftClearance + 7) / 8; wx++) {
					refineWalkNode(wx, wy, (px, py) -> px < p.getX() - type.dimensionLeft() ? 1 : 0);
				}
				// Right
				for (int wx = rightWx; wx > rightWx - (rightClearance + 7) / 8; wx--) {
					refineWalkNode(wx, wy, (px, py) -> px > p.getX() + type.dimensionRight() ? 1 : 0);
				}
			}
			// Top and Bottom
			int topWy = tp.getY() * 4;
			int bottomWy = (tp.getY() + u.getType().tileHeight()) * 4 - 1;
			int topClearance = type.tileHeight() * 32 / 2 - type.dimensionUp();
			int bottomClearance = type.tileHeight() * 32 / 2 - type.dimensionDown() - 1;
			for (int wx = tp.getX() * 4 + 1; wx < (tp.getX() + u.getType().tileWidth()) * 4 - 1; wx++) {
				// Top
				for (int wy = topWy; wy < topWy + (topClearance + 7) / 8; wy++) {
					refineWalkNode(wx, wy, (px, py) -> py < p.getY() - type.dimensionUp() ? 1 : 0);
				}
				// Bottom
				for (int wy = bottomWy; wy > bottomWy - (bottomClearance + 7) / 8; wy--) {
					refineWalkNode(wx, wy, (px, py) -> py > p.getY() + type.dimensionDown() ? 1 : 0);
				}
			}
		}

	}

	private static void refineWalkNode(int wx, int wy, BiFunction<Integer, Integer, Integer> checkClear) {
		WalkNodeData[][] data = new WalkNodeData[8][8];
		for (int px = 0; px < 8; px++) {
			data[px] = new WalkNodeData[8];
			for (int py = 0; py < 8; py++) {
				data[px][py] = new WalkNodeData(wx * 8 + px, wy * 8 + py, 1);
				data[px][py].clearance = checkClear.apply(wx * 8 + px, wy * 8 + py);
			}
		}
		walkableNodes[wx][wy] = new WalkNode1(data);
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
		int bottomLeft = wy + 1 < mapWalkHeight ? walkableNodes[wx][wy + 1].getClearance() : 0;
		int topRight = wx + 1 < mapWalkWidth ? walkableNodes[wx + 1][wy].getClearance() : 0;
		int bottomRight = wy + 1 < mapWalkHeight && wx + 1 < mapWalkWidth ? walkableNodes[wx + 1][wy + 1].getClearance()
				: 0;
		return Math.min(Math.min(bottomLeft, bottomRight), topRight) + 8;
	}

	/**
	 * Check if a {@link UnitType} fits into a given clearance
	 * 
	 * @param type
	 * @param clearance
	 * @return <b>true</b> if the unit does not fit, <b>false</b> otherwise.
	 */
	private static boolean unitDoesNotFit(UnitType type, int clearance) {
		// Unit size is in pixels, clearance is is pixels
		if (clearance < Math.max(type.width(), type.height())) {
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

		Queue<WalkNodeData> openSet = new PriorityQueue<WalkNodeData>(1, new Comparator<WalkNodeData>() {
			@Override
			public int compare(WalkNodeData n1, WalkNodeData n2) {
				return (int) Math.round((n1.predictedTotalCost - n2.predictedTotalCost) * 100);
			}
		});
		WalkNodeData startNode = walkableNodes[startWx][startWy].getCell(startx % 8, starty % 8);
		startNode.parent = null;
		startNode.costFromStart = 0;
		startNode.predictedTotalCost = Point.distance(startWx, startWy, endWx, endWy);
		openSet.add(startNode);
		Set<WalkNodeData> closedSet = new HashSet<WalkNodeData>();

		// Iterate
		while (openSet.size() > 0) {
			WalkNodeData currentNode = openSet.remove();
			// Base case
			if ((currentNode.x / 8 == endWx && currentNode.y / 8 == endWy) || currentNode.costFromStart > maxLength) {
				Deque<Position> path = new ArrayDeque<>();
				reconstructPath(path, currentNode, unitType);
				return path;
			}
			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (WalkNodeData neighbor : getNeighbors(currentNode)) {
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
		throw new NoPathFoundException();
	}

	// public static Queue<Position> findSafeAirPath(int startx, int starty,
	// double[][] threatMap, int length) {
	// int startWx = startx / 8;
	// int startWy = starty / 8;
	//
	// Queue<WalkNode> openSet = new PriorityQueue<WalkNode>(1, new
	// Comparator<WalkNode>() {
	// @Override
	// public int compare(WalkNode n1, WalkNode n2) {
	// return (int) Math.round((n1.predictedTotalCost - n2.predictedTotalCost) *
	// 100);
	// }
	// });
	// WalkNode currentNode = walkableNodes.get(startWx).get(startWy);
	// currentNode.parent = null;
	// currentNode.costFromStart = 0;
	// currentNode.distanceFromStart = 0;
	// currentNode.predictedTotalCost = threatMap[startWx / 4][startWy / 4];
	// openSet.add(currentNode);
	// Set<WalkNode> closedSet = new HashSet<WalkNode>();
	//
	// // Iterate
	// while (currentNode.distanceFromStart < length) {
	// currentNode = openSet.remove();
	// // Move the node from the open set to the closed set
	// closedSet.add(currentNode);
	// // Add all neigbors to the open set
	// for (WalkNode neighbor : getNeighbors(currentNode.wx, currentNode.wy)) {
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

	private static Collection<WalkNodeData> getNeighbors(WalkNodeData node) {
		Collection<WalkNodeData> neighbors = new ArrayList<>();

		if (node.size == 8) {
			// One walk-tile WalkNode
			int wx = node.x / 8;
			int wy = node.y / 8;
			// North
			if (wy - 1 >= 0) {
				neighbors.addAll(walkableNodes[wx][wy - 1].getCellsByRow(7));
				// North-West
				if (wx - 1 >= 0) {
					neighbors.add(walkableNodes[wx - 1][wy - 1].getCell(7, 7));
				}
				// North-East
				if (wx + 1 < walkableNodes.length) {
					neighbors.add(walkableNodes[wx + 1][wy - 1].getCell(0, 7));
				}
			}
			// South
			if (wy + 1 < walkableNodes[wx].length) {
				neighbors.addAll(walkableNodes[wx][wy + 1].getCellsByRow(0));
				// South-West
				if (wx - 1 >= 0) {
					neighbors.add(walkableNodes[wx - 1][wy + 1].getCell(7, 0));
				}
				// South-East
				if (wx + 1 < walkableNodes.length) {
					neighbors.add(walkableNodes[wx + 1][wy + 1].getCell(0, 0));
				}
			}
			// West
			if (wx - 1 >= 0) {
				neighbors.addAll(walkableNodes[wx - 1][wy].getCellsByColumn(7));
			}
			// East
			if (wx + 1 < mapWalkWidth) {
				neighbors.addAll(walkableNodes[wx + 1][wy].getCellsByColumn(0));
			}
			return neighbors;
		}

		// One-pixel WalkNode
		// North
		if (node.y - 1 >= 0) {
			neighbors.add(walkableNodes[node.x / 8][(node.y - 1) / 8].getCell(node.x % 8, (node.y - 1) % 8));
			// North-West
			if (node.x - 1 >= 0) {
				neighbors.add(
						walkableNodes[(node.x - 1) / 8][(node.y + 1) / 8].getCell((node.x - 1) % 8, (node.y - 1) % 8));
			}
			// North-East
			if ((node.x + 1) / 8 < walkableNodes.length) {
				neighbors.add(
						walkableNodes[(node.x + 1) / 8][(node.y + 1) / 8].getCell((node.x + 1) % 8, (node.y - 1) % 8));
			}
		}
		// South
		if ((node.y + 1) / 8 < walkableNodes[node.x / 8].length) {
			neighbors.add(walkableNodes[node.x / 8][(node.y - 1) / 8].getCell(node.x % 8, (node.y + 1) % 8));
			// South-West
			if (node.x - 1 >= 0) {
				neighbors.add(
						walkableNodes[(node.x - 1) / 8][(node.y - 1) / 8].getCell((node.x - 1) % 8, (node.y + 1) % 8));
			}
			// South-East
			if ((node.x + 1) / 8 < walkableNodes.length) {
				neighbors.add(
						walkableNodes[(node.x + 1) / 8][(node.y - 1) / 8].getCell((node.x + 1) % 8, (node.y + 1) % 8));
			}
		}
		// West
		if (node.x - 1 >= 0) {
			neighbors.add(walkableNodes[(node.x - 1) / 8][node.y / 8].getCell((node.x - 1) % 8, node.y % 8));
		}
		// East
		if ((node.x + 1) / 8 < mapWalkWidth) {
			neighbors.add(walkableNodes[(node.x + 1) / 8][node.y / 8].getCell((node.x + 1) % 8, node.y % 8));
		}
		return neighbors;
	}

	private static Deque<Position> reconstructPath(Deque<Position> path, WalkNodeData currentNode, UnitType unitType) {
		path.push(new Position(currentNode.x + unitType.width() / 2, currentNode.y + unitType.height() / 2));

		// Base case
		if (currentNode.parent == null) {
			return path;
		}
		return reconstructPath(path, currentNode.parent, unitType);
	}

	public static void registerDebugFunctions() {
		// Clearance values
		DebugManager.createDebugModule("clearance").setDraw(() -> {
			try {
				// // Show clearance values
				// for (int wx = 0; wx < mapWalkWidth; wx++) {
				// for (int wy = 0; wy < mapWalkHeight; wy++) {
				// for (WalkNodeData n : walkableNodes[wx][wy])
				// if (n.clearance == 0) {
				// DrawEngine.drawBoxMap(n.x, n.y, n.x + 8, n.y + 8, Color.Red,
				// true);
				// } else if (n.clearance == 1) {
				// DrawEngine.drawBoxMap(n.x, n.y, n.x + 8, n.y + 8,
				// Color.Orange, true);
				// } else if (n.clearance == 2) {
				// DrawEngine.drawBoxMap(n.x, n.y, n.x + 8, n.y + 8,
				// Color.Yellow, true);
				// } else if (n.clearance == 3) {
				// DrawEngine.drawBoxMap(n.x, n.y, n.x + 8, n.y + 8,
				// Color.Green, true);
				// }
				// }
				// }
				// for (int wx = 0; wx < GameHandler.getMapWalkWidth(); wx++) {
				// for (int wy = 0; wy < GameHandler.getMapWalkHeight(); wy++) {
				// walkableNodes[wx][wy].forEach(n -> {
				// Color c = n.clearance == 0 ? Color.Red : Color.Green;
				// try {
				// DrawEngine.drawDotMap(n.x, n.y, c);
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				// });
				// }
				// }
				Position mousePosition = GameHandler.getMousePositionOnMap();
				if (mousePosition.getX() / 8 < mapWalkWidth && mousePosition.getY() / 8 < mapWalkHeight) {
					WalkNodeData thisCell = walkableNodes[mousePosition.getX() / 8][mousePosition.getY() / 8]
							.getCell(mousePosition.getX() % 8, mousePosition.getY() % 8);
					DrawEngine.drawBoxMap(thisCell.x, thisCell.y, thisCell.x + thisCell.size,
							thisCell.y + thisCell.size, Color.Purple, true);
					DrawEngine.drawBoxMap(thisCell.x, thisCell.y, thisCell.x + thisCell.clearance,
							thisCell.y + thisCell.clearance, Color.Yellow, false);
					for (WalkNodeData n : getNeighbors(thisCell)) {
						for (WalkNodeData w : walkableNodes[n.x / 8][n.y / 8]) {
							Color c = w.clearance == 0 ? Color.Red : Color.Green;
							DrawEngine.drawBoxMap(w.x, w.y, w.x + w.size, w.y + w.size, c, true);
						}
					}
					DrawEngine.drawTextMap(mousePosition.getX() + 20, mousePosition.getY(),
							mousePosition.getX() + ", " + mousePosition.getY());
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