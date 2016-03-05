package micro;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import pathing.InvalidStartNodeException;
import pathing.NoPathFoundException;
import pathing.Node;
import pathing.PathFinder;

public abstract class GroundAgent extends UnitAgent {
	public GroundAgent(Unit u) {
		super(u);
	}

	public void findPath(Position toWhere, int length) throws NoPathFoundException {
		pathTargetBox = null;
		// If we already have a decent path
		if (pathTarget != null && pathTarget.equals(toWhere)
				&& (path.size() >= 1.0 / 3 * length || pathOriginalSize <= 1.0 / 3 * length)) {
			return;
		}
		// Every 500 frames make the pathfinder work harder
		length *= ((GameHandler.getFrameCount() - pathStartFrame) / PATHING_TIMEOUT_FRAMES + 1);
		// Otherwise make a new path
		try {
			// TODO this still needs work
			path = PathFinder.findGroundPath(unit, toWhere, length);
			pathTarget = toWhere;
			pathOriginalSize = path.size();
			pathStartFrame = GameHandler.getFrameCount();
		} catch (InvalidStartNodeException e) {
			e.printStackTrace();
		}
	}

	public void findPath(Rectangle toWhere, int length) throws NoPathFoundException {
		pathTarget = null;
		// If we already have a decent path
		if (pathTargetBox != null && pathTargetBox.equals(toWhere)
				&& (path.size() >= 1.0 / 3 * length || pathOriginalSize <= 1.0 / 3 * length)) {
			return;
		}
		// Every 500 frames make the pathfinder work harder
		length *= ((GameHandler.getFrameCount() - pathStartFrame) / PATHING_TIMEOUT_FRAMES + 1);
		// Otherwise make a new path
		try {
			path = PathFinder.findGroundPath(unit, toWhere, length);
			pathTargetBox = toWhere;
			pathOriginalSize = path.size();
			pathStartFrame = GameHandler.getFrameCount();
		} catch (InvalidStartNodeException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Deque<Position> findPathAwayFrom(Position fromWhere, int length)
			throws InvalidStartNodeException, NoPathFoundException {
		int fromWhereWx = fromWhere.getX() / 8;
		int fromWhereWy = fromWhere.getY() / 8;

		Queue<Node> openSet = new PriorityQueue<Node>(1, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return (int) Math.round(
						(MicroManager.threatMap[n1.wx / 4][n1.wy / 4] - MicroManager.threatMap[n2.wx / 4][n2.wy / 4])
								* 1000);
			}
		});
		// Find the closest walkable node
		Node startNode = PathFinder.findClosestWalkableNode(fromWhereWx, fromWhereWy, unit.getType());
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
			for (Node neighbor : PathFinder.getNeighbors(currentNode.wx, currentNode.wy)) {
				double tentative_g_score = currentNode.costFromStart
						+ Point.distance(currentNode.wx, currentNode.wy, neighbor.wx, neighbor.wy);
				// Base case
				if (tentative_g_score > length) {
					Deque<Position> path = new ArrayDeque<>();
					PathFinder.reconstructPath(path, currentNode, unit.getType());
					return path;
				}

				if (closedSet.contains(neighbor) || PathFinder.unitDoesNotFit(unit.getType(), neighbor.clearance)) {
					continue;
				}

				if (!openSet.contains(neighbor) || tentative_g_score < neighbor.costFromStart) {
					neighbor.parent = currentNode;
					neighbor.costFromStart = tentative_g_score;
					neighbor.predictedTotalCost = tentative_g_score
							+ Point.distance(neighbor.wx, neighbor.wy, fromWhereWx, fromWhereWy);
					openSet.add(neighbor);
				}
			}
		}
		throw new NoPathFoundException();
	}
}
