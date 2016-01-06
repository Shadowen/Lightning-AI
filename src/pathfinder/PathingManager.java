package pathfinder;

import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.ShapeOverflowException;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import bwapi.Color;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.UnitType;

public final class PathingManager {
	private static Quadtree<WalkNode> walkNodes;
	private static int mapWalkWidth;
	private static int mapWalkHeight;

	public static void init() {
		System.out.print("Starting PathingManager... ");
		mapWalkWidth = GameHandler.getMapWalkWidth();
		mapWalkHeight = GameHandler.getMapWalkHeight();

		int qtdim = Math.max(mapWalkWidth * 8, mapWalkHeight * 8);
		// Init walkable map
		walkNodes = new Quadtree<WalkNode>(qtdim);
		for (int wx = 0; wx < mapWalkWidth; wx++) {
			for (int wy = 0; wy < 8; wy++) {
				WalkNode wn = new WalkNode(wx * 8, wy * 8, 8);
				// TODO actual clearance calculation
				if (!GameHandler.isWalkable(wx, wy)) {
					wn.clearance = 0;
				} else {
					wn.clearance = 1;
				}
				walkNodes.insert(wn);
			}
		}
		compressMap();
		// refreshWalkableMap();

		registerDebugFunctions();

		// findChokeToMain();
		System.out.println("Success!");
	}

	/** This constructor should never be used. */
	private PathingManager() {
	}

	private static void compressMap() {
		System.out.println("Before compression, QTNodes: " + walkNodes.stream().count());
		walkNodes.recurseFromLeaves((node, childResults) -> {
			if (childResults.size() == 0) {
				return node.objects.stream().mapToInt(o -> o.clearance).min().getAsInt();
			}
			int sum = childResults.stream().mapToInt(r -> (int) r).sum();
			if (sum == 0 || sum == 1) {
				node.merge((a, b) -> {
					return b;
				});
			}
			return 1;
		});
		System.out.println("After compression, QTNodes: " + walkNodes.stream().count());
	}

	public static void refreshClearanceMap() {
		// Evaluate nodes in reverse infinity-norm distance order
		for (int d = Math.max(mapWalkHeight, mapWalkWidth) - 1; d >= 0; d--) {
			// Need to expand diagonally back towards the origin
			int width = Math.min(mapWalkWidth - 1, d);
			int height = Math.min(mapWalkHeight - 1, d);
			// Right to left across the bottom: (wx, d)
			for (int wx = width; wx >= 0; wx--) {
				walkNodes.getAt(new Point(wx, height)).ifPresent(n -> n.clearance = getTrueClearance(n));
			}
			// Bottom to top up the right side: (d, wy)
			for (int wy = height; wy >= 0; wy--) {
				walkNodes.getAt(new Point(width, wy)).ifPresent(n -> n.clearance = getTrueClearance(n));
			}
		}
	}

	/**
	 * Finds the true clearance for a certain walk tile
	 **/
	private static int getTrueClearance(WalkNode node) {
		// Current tile is not walkable
		if (!GameHandler.isWalkable(node.x, node.y)) {
			return 0;
		}
		// True clearance is one larger than the minimum of the three true
		// clearances below, to the right, and below-right
		return walkNodes.getDownAndRight().stream().mapToInt(n -> n.clearance).min().orElse(0) + 1;
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

	public static Deque<Position> findGroundPath(Position start, Position end, UnitType unitType)
			throws NoPathFoundException {
		return findGroundPath(start.getX(), start.getY(), end.getX(), end.getY(), unitType);
	}

	public static Deque<Position> findGroundPath(int startx, int starty, int endx, int endy, UnitType unitType)
			throws NoPathFoundException {
		return findGroundPath(startx, starty, endx, endy, unitType, Integer.MAX_VALUE);
	}

	public static Deque<Position> findGroundPath(int startx, int starty, int endx, int endy, UnitType unitType,
			int maxLength) throws NoPathFoundException {
		int startWx = (startx - unitType.width() / 2) / 8;
		int startWy = (starty - unitType.height() / 2) / 8;
		int endWx = (endx - unitType.width() / 2) / 8;
		int endWy = (endy - unitType.height() / 2) / 8;

		Queue<WalkNode> openSet = new PriorityQueue<WalkNode>(1, new Comparator<WalkNode>() {
			@Override
			public int compare(WalkNode n1, WalkNode n2) {
				return (int) Math.round((n1.predictedTotalCost - n2.predictedTotalCost) * 100);
			}
		});
		WalkNode startNode = walkNodes.getAt(new Point(startx, starty)).orElseThrow(() -> new NullPointerException());
		startNode.parent = null;
		startNode.costFromStart = 0;
		startNode.predictedTotalCost = Point.distance(startWx, startWy, endWx, endWy);
		openSet.add(startNode);
		Set<WalkNode> closedSet = new HashSet<WalkNode>();

		// Iterate
		while (openSet.size() > 0) {
			WalkNode currentNode = openSet.remove();
			// Base case
			if ((currentNode.x == endWx && currentNode.y == endWy) || currentNode.costFromStart > maxLength) {
				Deque<Position> path = new ArrayDeque<>();
				reconstructPath(path, currentNode, unitType);
				return path;
			}
			// Move the node from the open set to the closed set
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (WalkNode neighbor : walkNodes.getNeighbors(currentNode)) {
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

	// public static Deque<Position> findSafeAirPath(int startx, int starty,
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
	// WalkNode currentNode = walkNodes.get(startWx).get(startWy);
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
	// for (WalkNode neighbor : currentNode.neighbors) {
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

	private static Deque<Position> reconstructPath(Deque<Position> path, WalkNode finalNode, UnitType unitType) {
		path.push(new Position(finalNode.x * 8 + unitType.width() / 2, finalNode.y * 8 + unitType.height() / 2));

		// Base case
		if (finalNode.parent == null) {
			return path;
		}
		return reconstructPath(path, finalNode.parent, unitType);
	}

	// private static Deque<Position> reconstructAirPath(Deque<Position> path,
	// WalkNode finalNode) {
	// path.push(new Position(finalNode.wx * 8 + 4, finalNode.wy * 8 + 4));
	//
	// // Base case
	// if (finalNode.parent == null) {
	// return path;
	// }
	// return reconstructAirPath(path, finalNode.parent);
	// }

	public static void registerDebugFunctions() {
		// // Clearance values
		// DebugManager.createDebugModule("clearance").setDraw(() -> {
		// try {
		// // Show clearance values
		// for (int wx = 0; wx < mapWalkWidth; wx++) {
		// for (int wy = 0; wy < mapWalkHeight; wy++) {
		// final WalkNode n = walkNodes.getAt(new Point(wx * 8, wy * 8)).get();
		// if (n.clearance == 0) {
		// DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8,
		// Color.Red, true);
		// } else if (n.clearance == 1) {
		// DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8,
		// Color.Orange, true);
		// } else if (n.clearance == 2) {
		// DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8,
		// Color.Yellow, true);
		// } else if (n.clearance == 3) {
		// DrawEngine.drawBoxMap(n.x * 8, n.y * 8, n.x * 8 + 8, n.y * 8 + 8,
		// Color.Green, true);
		// }
		// }
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// });
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
		// Buildings
		DebugManager.createDebugModule("buildings").setDraw(() -> {
			GameHandler.getAllUnits().stream().filter(u -> u.getType().isBuilding() && !u.isFlying()).forEach(u -> {
				try {
					TilePosition tp = u.getTilePosition();
					DrawEngine.drawBoxMap(tp.getX() * 32, tp.getY() * 32, tp.getX() * 32 + u.getType().tileWidth() * 32,
							tp.getY() * 32 + u.getType().tileHeight() * 32, Color.Cyan, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}).setActive(false);
		// // Building gaps
		// DebugManager.createDebugModule("buildinggaps").setDraw(() -> {
		// GameHandler.getAllUnits().stream().filter(u ->
		// GameHandler.getSelectedUnits().contains(u))
		// .filter(u -> u.getType().isBuilding() && !u.isFlying()).forEach(u ->
		// {
		// try {
		// for (Unit b : u.getUnitsInRadius(32).stream().filter(x ->
		// x.getType().isBuilding() && !u.isFlying())
		// .collect(Collectors.toList())) {
		// if (b == u) {
		// continue;
		// }
		// // u and b share an edge
		// int utx = u.getTilePosition().getX();
		// int uty = u.getTilePosition().getY();
		// int btx = b.getTilePosition().getX();
		// int bty = b.getTilePosition().getY();
		// UnitType utype = u.getType();
		// UnitType btype = b.getType();
		//
		// // b to the left of u (including diagonals)
		// if (btx + b.getType().tileWidth() <= utx) {
		// DrawEngine
		// .drawTextMap(u.getX() - 20, u.getY() - 10,
		// "L:" + "("
		// + ((utype.tileWidth() * 32 / 2 - utype.dimensionLeft()) + ","
		// + (btype.tileWidth() * 32 / 2 - btype.dimensionRight() - 1))
		// + ")");
		// }
		// // b to the right of u (including diagonals)
		// else if (btx >= utx + u.getType().tileWidth()) {
		// DrawEngine
		// .drawTextMap(u.getX() + 20, u.getY() + 10,
		// "R:" + "("
		// + ((utype.tileWidth() * 32 / 2 - utype.dimensionRight() - 1) + ","
		// + (btype.tileWidth() * 32 / 2 - btype.dimensionLeft()))
		// + ")");
		// }
		// // b atop u
		// else if (bty + b.getType().tileHeight() <= uty) {
		// DrawEngine
		// .drawTextMap(u.getX() - 10, u.getY() - 20,
		// "T:" + "("
		// + ((utype.tileHeight() * 32 / 2 - utype.dimensionUp()) + ","
		// + (btype.tileHeight() * 32 / 2 - btype.dimensionDown() - 1))
		// + ")");
		// }
		// // b below u
		// else if (bty >= uty + u.getType().tileHeight()) {
		// DrawEngine
		// .drawTextMap(u.getX() - 10, u.getY() + 20,
		// "B:" + "("
		// + ((utype.tileHeight() * 32 / 2 - utype.dimensionDown() - 1) + ","
		// + (btype.tileHeight() * 32 / 2 - btype.dimensionUp()))
		// + ")");
		// }
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// });
		// }).setActive(true);
		DebugManager.createDebugModule("quadtree").setDraw(() -> {
			DrawEngine.drawTextScreen(100, 100, "QT Depth: " + walkNodes.getTotalDepth());
			// walkNodes.processNodes(tree -> {
			// Rectangle bounds = tree.getBounds();
			// if (tree.getDepthBelow() < 2) {
			// try {
			// DrawEngine.drawBoxMap(bounds.x, bounds.y, bounds.x +
			// bounds.width, bounds.y + bounds.height,
			// Color.Grey, false);
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			// });
			walkNodes.stream().forEach(w -> {
				Color c = w.clearance > 0 ? Color.Green : Color.Red;
				try {
					DrawEngine.drawBoxMap(w.x + 1, w.y + 1, w.x + w.width - 1, w.y + w.height - 1, c, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}).setActive(true);
	}
}