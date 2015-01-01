package eaglesWings.pathfinder;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import javabot.model.ChokePoint;
import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;
import eaglesWings.datastructure.BaseManager;
import eaglesWings.gamestructure.DebugEngine;
import eaglesWings.gamestructure.DebugModule;
import eaglesWings.gamestructure.Debuggable;
import eaglesWings.gamestructure.GameHandler;

public class PathingManager implements Debuggable {
	private static final int MAX_RAMP_WALK_TILES = 500;
	private GameHandler game;
	private BaseManager baseManager;

	private ArrayList<ArrayList<Node>> walkableNodes;
	private int mapWalkWidth;
	private int mapWalkHeight;

	// A list of tiles detailing a path into the main from the choke
	private List<Point> pathIntoMain;
	private Point topOfRamp;
	private List<Point> chokeRampWalkTiles;

	public PathingManager(GameHandler g, BaseManager bm) {
		game = g;
		baseManager = bm;

		mapWalkWidth = game.getMap().getWalkWidth();
		mapWalkHeight = game.getMap().getWalkHeight();

		// Init walkable map
		walkableNodes = new ArrayList<ArrayList<Node>>();
		for (int wx = 0; wx < game.getMap().getWidth() * 4; wx++) {
			walkableNodes.add(new ArrayList<Node>());
			for (int wy = 0; wy < game.getMap().getHeight() * 4; wy++) {
				walkableNodes.get(wx).add(new Node(wx, wy));
			}
		}
	}

	public void findChokeToMain() {
		for (ChokePoint choke : game.getMap().getChokePoints()) {
			// Find choke to main base
			if (choke.getFirstRegionID() == baseManager.main.getLocation()
					.getRegionID()
					|| choke.getSecondRegionID() == baseManager.main
							.getLocation().getRegionID()) {
				// Find the path into the main
				pathIntoMain = new ArrayList<Point>(findGroundPath(
						choke.getCenterX(), choke.getCenterY(),
						baseManager.main.getX(), baseManager.main.getY(),
						UnitTypes.Zerg_Zergling));

				// Find top of ramp
				for (Point p : pathIntoMain) {
					if (game.getMap().isBuildable(p.x / 32, p.y / 32)) {
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
								Point topOfRampWalkTile = new Point(
										topOfRamp.x / 8, topOfRamp.y / 8);
								return (int) (p1.distanceSq(topOfRampWalkTile) - p2
										.distanceSq(topOfRampWalkTile));
							}
						});
				rampWalkTileOpenSet.add(new Point(choke.getCenterX() / 8, choke
						.getCenterY() / 8));
				while (!rampWalkTileOpenSet.isEmpty()) {
					Point currentNode = rampWalkTileOpenSet.poll();
					if (game.getMap().isWalkable(currentNode.x, currentNode.y)
							&& !game.getMap().isBuildable(currentNode.x / 4,
									currentNode.y / 4)) {
						chokeRampWalkTiles.add(new Point(currentNode.x,
								currentNode.y));
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
		}
	}

	public Queue<Point> findGroundPath(int startx, int starty, int endx,
			int endy, int type, int length) {
		return findGroundPath(startx, starty, endx, endy,
				game.getUnitType(type), length);
	}

	public Queue<Point> findGroundPath(int startx, int starty, int endx,
			int endy, UnitTypes type) {
		return findGroundPath(startx, starty, endx, endy,
				game.getUnitType(type.ordinal()), Integer.MAX_VALUE);
	}

	public Queue<Point> findGroundPath(int startx, int starty, int endx,
			int endy, UnitType type) {
		return findGroundPath(startx, starty, endx, endy, type,
				Integer.MAX_VALUE);
	}

	public Queue<Point> findGroundPath(int startx, int starty, int endx,
			int endy, UnitType type, int length) {
		int startWx = startx / 8;
		int startWy = starty / 8;
		int endWx = endx / 8;
		int endWy = endy / 8;

		// Initialize
		for (int wx = 0; wx < mapWalkWidth; wx++) {
			for (int wy = 0; wy < mapWalkHeight; wy++) {
				walkableNodes.get(wx).get(wy).parent = null;
				walkableNodes.get(wx).get(wy).costFromStart = 0;
				walkableNodes.get(wx).get(wy).predictedTotalCost = 0;
				walkableNodes.get(wx).get(wy).walkable = true;
			}
		}
		// Avoid cliffs
		for (int wx = 0; wx < game.getMap().getWidth() * 4; wx++) {
			for (int wy = 0; wy < game.getMap().getHeight() * 4; wy++) {
				if (!game.getMap().isWalkable(wx, wy)) {
					for (int iwx = Integer.max(wx - 3, 0); iwx < Integer.min(
							wx + 3, mapWalkWidth); iwx++) {
						for (int iwy = Integer.max(wy - 3, 0); iwy < Integer
								.min(wy + 3, mapWalkHeight); iwy++) {
							walkableNodes.get(iwx).get(iwy).walkable = false;
						}
					}
				}
			}
		}
		// Avoid buildings
		for (Unit u : game.getAllUnits()) {
			UnitType utype = game.getUnitType(u.getTypeID());
			if (!utype.isCanMove()) {
				int uwidth = utype.getTileWidth();
				int uheight = utype.getTileHeight();
				for (int wx = Integer.max(u.getTileX() * 4 - 3, 0); wx < Integer
						.min((u.getTileX() + uwidth) * 4 + 3, mapWalkWidth); wx++) {
					for (int wy = Integer.max(u.getTileY() * 4 - 3, 0); wy < Integer
							.min((u.getTileY() + uheight) * 4 + 3,
									mapWalkHeight); wy++) {
						walkableNodes.get(wx).get(wy).walkable = false;
					}
				}
			}
		}

		NodeSet openSet = new NodeSet();
		walkableNodes.get(startWx).get(startWy).costFromStart = 0;
		walkableNodes.get(startWx).get(startWy).predictedTotalCost = walkableNodes
				.get(startWx).get(startWy).costFromStart
				+ Point.distance(startWx, startWy, endWx, endWy);
		openSet.add(walkableNodes.get(startWx).get(startWy));
		Set<Node> closedSet = new HashSet<Node>();

		// Iterate
		while (openSet.size() > 0) {
			Node currentNode = openSet.getNext();
			// Base case
			if (currentNode.x == endWx && currentNode.y == endWy) {
				Deque<Point> path = new ArrayDeque<Point>();
				reconstructPath(path, currentNode);
				return path;
			}
			// Move the node from the open set to the closed set
			openSet.remove(currentNode);
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(walkableNodes, currentNode.x,
					currentNode.y)) {
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

			// Length
			if (currentNode.costFromStart > length) {
				Deque<Point> path = new ArrayDeque<Point>();
				reconstructPath(path, currentNode);
				return path;
			}
		}

		// throw noPathException!?
		return new ArrayDeque<Point>();
	}

	private List<Node> getNeighbors(List<ArrayList<Node>> allNodes, int x, int y) {
		List<Node> neighbors = new ArrayList<Node>();

		// NORTH
		if (y + 1 < allNodes.get(x).size()) {
			neighbors.add(allNodes.get(x).get(y + 1));
			// NORTH-EAST
			if (x + 1 < allNodes.size()) {
				neighbors.add(allNodes.get(x + 1).get(y + 1));
			}
			// NORTH-WEST
			if (x - 1 >= 0) {
				neighbors.add(allNodes.get(x - 1).get(y + 1));
			}
		}
		// EAST
		if (x + 1 < allNodes.size()) {
			neighbors.add(allNodes.get(x + 1).get(y));
		}
		// SOUTH
		if (y - 1 >= 0) {
			neighbors.add(allNodes.get(x).get(y - 1));
			// SOUTH-EAST
			if (x + 1 < allNodes.size()) {
				neighbors.add(allNodes.get(x + 1).get(y - 1));
			}
			// SOUTH-WEST
			if (x - 1 >= 0) {
				neighbors.add(allNodes.get(x - 1).get(y - 1));
			}
		}
		// WEST
		if (x - 1 >= 0) {
			neighbors.add(allNodes.get(x - 1).get(y));
		}

		return neighbors;
	}

	private Deque<Point> reconstructPath(Deque<Point> path, Node finalNode) {
		path.push(new Point(finalNode.x * 8, finalNode.y * 8));

		// Base case
		if (finalNode.parent == null) {
			return path;
		}
		return reconstructPath(path, finalNode.parent);
	}

	public void registerDebugFunctions(GameHandler g) {
		// Label all chokes
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				int i = 0;
				for (ChokePoint choke : game.getMap().getChokePoints()) {
					engine.drawText(choke.getCenterX() - 10,
							choke.getCenterY() - 20, "Choke " + i, false);
					engine.drawText(choke.getCenterX() - 10,
							choke.getCenterY() - 10,
							"Radius " + choke.getRadius(), false);
					i++;
				}
			}
		});
		// // Draw path from choke into main
		// g.registerDebugFunction(new DebugModule() {
		// @Override
		// public void draw(DebugEngine engine) {
		// for (Point location : pathIntoMain) {
		// engine.drawBox(location.x + 1, location.y + 1,
		// location.x + 6, location.y + 6, BWColor.GREY,
		// false, false);
		// }
		// engine.drawBox(topOfRamp.x + 1, topOfRamp.y + 1,
		// topOfRamp.x + 6, topOfRamp.y + 6, BWColor.RED, false,
		// false);
		// }
		// });
		// // Highlight ramp walk tiles
		// g.registerDebugFunction(new DebugModule() {
		// @Override
		// public void draw(DebugEngine engine) {
		// for (Point location : chokeRampWalkTiles) {
		// engine.drawBox(location.x * 8, location.y * 8,
		// location.x * 8 + 8, location.y * 8 + 8,
		// BWColor.GREEN, false, false);
		// }
		// }
		// });
	}
}
