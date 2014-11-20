package javabot.datastructure;

import java.util.HashMap;
import java.util.Map.Entry;
import javabot.gamestructure.DebugEngine;
import javabot.gamestructure.DebugModule;
import javabot.gamestructure.GameHandler;
import javabot.model.*;
import javabot.util.BWColor;

public class Base {
	private GameHandler game;

	public java.util.Map<Integer, Resource> minerals;
	public java.util.Map<Integer, Resource> gas;
	private java.util.Map<Integer, Worker> workers;
	public Unit commandCenter;
	public BaseLocation location;

	public Base(GameHandler g, BaseLocation l) {
		game = g;

		workers = new HashMap<Integer, Worker>();
		location = l;

		minerals = new HashMap<Integer, Resource>();
		gas = new HashMap<Integer, Resource>();

	}

	public int getOwner() {
		if (commandCenter == null) {
			return -1;
		}
		return commandCenter.getPlayerID();
	}

	public int getMineralCount() {
		return minerals.size();
	}

	public void gatherResources() {
		// Idle workers
		for (Entry<Integer, Worker> worker : workers.entrySet()) {
			if (worker.getValue().isIdle()) {
				boolean workerAssigned = worker.getValue().isGathering();
				if (workerAssigned) {
					worker.getValue().mine();
					break;
				}

				// Try to assign one worker to each mineral first
				Resource mineral = null;
				double distance = 0;

				// This variable is the loop counter
				// It only allows maxMiners to gather each resource patch each
				// loop.
				int maxMiners = 1;
				while (!workerAssigned) {
					if (maxMiners > 3) {
						// supersaturated - can't find a suitable resource patch
						throw new StackOverflowError();
					}

					for (Entry<Integer, Resource> m : minerals.entrySet()) {
						if (m.getValue().getNumGatherers() < maxMiners) {
							// Find closest mineral patch
							double newDistance = game.distance(
									worker.getValue(), m.getValue());
							if (mineral == null || newDistance < distance) {
								mineral = m.getValue();
								distance = newDistance;
								workerAssigned = true;
							}
						}
					}

					maxMiners++;
				}

				// Actually issue the order
				worker.getValue().mine(mineral);
			}
		}
	}

	public Worker getBuilder() {
		Worker u = null;
		for (Entry<Integer, Worker> w : workers.entrySet()) {
			if (!w.getValue().isBuilding()) {
				u = w.getValue();
			}
		}
		return u;
	}

	public int getWorkerCount() {
		return workers.size();
	}

	public void addWorker(int unitID, Unit unit) {
		workers.put(unitID, new Worker(game, unit));
	}

	public boolean removeWorker(int unitID) {
		// That worker actually does belong to this base!
		if (workers.containsKey(unitID)) {
			workers.get(unitID).stopMining();
			workers.remove(unitID);
			return true;
		}
		// That worker does not belong to this base
		return false;
	}
}