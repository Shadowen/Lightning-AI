package eaglesWings.pathfinder;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javabot.model.ChokePoint;
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

	// A list of tiles detailing a path into the main from the choke
	private List<Point> pathIntoMain;
	private Point topOfRamp;
	private List<Point> chokeRampWalkTiles;

	public PathingManager(GameHandler g, BaseManager bm) {
		game = g;
		baseManager = bm;
	}

	public void findChokeToMain() {
		for (ChokePoint choke : game.getMap().getChokePoints()) {
			// Find choke to main base
			if (choke.getFirstRegionID() == baseManager.main.location
					.getRegionID()
					|| choke.getSecondRegionID() == baseManager.main.location
							.getRegionID()) {
				// Find the path into the main
				pathIntoMain = findGroundPath(choke.getCenterX(),
						choke.getCenterY(), baseManager.main.location.getX(),
						baseManager.main.location.getY());

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

	public List<Point> findGroundPath(int startx, int starty, int endx, int endy) {
		int startWx = startx / 8;
		int startWy = starty / 8;
		int endWx = endx / 8;
		int endWy = endy / 8;

		// Init walkable map
		List<ArrayList<Node>> nodes = new ArrayList<ArrayList<Node>>();
		for (int wx = 0; wx < game.getMap().getWidth() * 4; wx++) {
			nodes.add(new ArrayList<Node>());
			for (int wy = 0; wy < game.getMap().getHeight() * 4; wy++) {
				nodes.get(wx).add(
						new Node(wx, wy, game.getMap().isWalkable(wx, wy)));
			}
		}

		// Initialize
		NodeSet openSet = new NodeSet();
		nodes.get(endWx).get(endWy).costFromStart = 0;
		nodes.get(endWx).get(endWy).predictedTotalCost = nodes.get(endWx).get(
				endWy).costFromStart
				+ Point.distance(endWx, endWy, startWx, startWy);
		openSet.add(nodes.get(endWx).get(endWy));
		NodeSet closedSet = new NodeSet();

		// Iterate
		while (openSet.size() > 0) {
			Node currentNode = openSet.getNext();
			// Base case
			if (currentNode.x == startWx && currentNode.y == startWy) {
				List<Point> path = new ArrayList<Point>();
				reconstructPath(path, currentNode);
				return path;
			}
			// Move the node from the open set to the closed set
			openSet.remove(currentNode);
			closedSet.add(currentNode);
			// Add all neigbors to the open set
			for (Node neighbor : getNeighbors(nodes, currentNode.x,
					currentNode.y)) {
				if (closedSet.contains(neighbor) || !neighbor.walkable) {
					continue;
				}

				double tentative_g_score = currentNode.costFromStart + 1;
				if (!openSet.contains(neighbor)
						|| tentative_g_score < neighbor.costFromStart) {
					neighbor.parent = currentNode;
					neighbor.costFromStart = tentative_g_score;
					neighbor.predictedTotalCost = tentative_g_score
							+ Point.distance(neighbor.x, neighbor.y, startWx,
									startWy);
					openSet.add(neighbor);
				}
			}
		}

		// throw noPathException!?
		return new ArrayList<Point>();
	}

	private List<Node> getNeighbors(List<ArrayList<Node>> allNodes, int x, int y) {
		List<Node> neighbors = new ArrayList<Node>();
		if (x - 1 >= 0) {
			neighbors.add(allNodes.get(x - 1).get(y));
		}
		if (x + 1 < allNodes.size()) {
			neighbors.add(allNodes.get(x + 1).get(y));
		}
		if (y - 1 >= 0) {
			neighbors.add(allNodes.get(x).get(y - 1));
		}
		if (y + 1 < allNodes.get(x).size()) {
			neighbors.add(allNodes.get(x).get(y + 1));
		}
		return neighbors;
	}

	private List<Point> reconstructPath(List<Point> path, Node finalNode) {
		path.add(new Point(finalNode.x * 8, finalNode.y * 8));

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
		// Draw path from choke into main
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				for (Point location : pathIntoMain) {
					engine.drawBox(location.x + 1, location.y + 1,
							location.x + 6, location.y + 6, BWColor.GREY,
							false, false);
				}
				engine.drawBox(topOfRamp.x + 1, topOfRamp.y + 1,
						topOfRamp.x + 6, topOfRamp.y + 6, BWColor.RED, false,
						false);
			}
		});
		// Highlight ramp walk tiles
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				for (Point location : chokeRampWalkTiles) {
					engine.drawBox(location.x * 8, location.y * 8,
							location.x * 8 + 8, location.y * 8 + 8,
							BWColor.GREEN, false, false);
				}
			}
		});
	}
}
