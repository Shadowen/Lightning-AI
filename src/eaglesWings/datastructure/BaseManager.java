package eaglesWings.datastructure;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import eaglesWings.gamestructure.DebugEngine;
import eaglesWings.gamestructure.DebugModule;
import eaglesWings.gamestructure.Debuggable;
import eaglesWings.gamestructure.GameHandler;
import javabot.model.Unit;
import javabot.util.BWColor;

public class BaseManager implements Iterable<Base>, Debuggable {
	private Set<Base> bases;
	public Base main;

	private int selfPlayerID;

	public BaseManager(GameHandler game) {
		bases = new HashSet<Base>();

		selfPlayerID = game.getSelf().getID();
	}

	public void addBase(Base b) {
		bases.add(b);
	}

	public Set<Base> getMyBases() {
		Set<Base> myBases = new HashSet<Base>();
		for (Base b : bases) {
			if (b.getOwner() == selfPlayerID) {
				myBases.add(b);
			}
		}
		return myBases;
	}

	// Gets the closest base from a given (x, y) position
	public Base getClosestBase(int x, int y) {
		Base closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Base b : bases) {
			double distance = Point.distance(x, y, b.location.getX(),
					b.location.getY());
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = b;
			}
		}
		return closest;
	}

	public Worker getBuilder() {
		for (Base b : bases) {
			Worker u = b.getBuilder();
			if (u != null) {
				return u;
			}
		}
		return null;
	}

	public void removeWorker(int unitID) {
		for (Base b : bases) {
			if (b.removeWorker(unitID)) {
				break;
			}
		}
	}

	// Returns an iterator over all the bases
	@Override
	public Iterator<Base> iterator() {
		return bases.iterator();
	}

	public Worker getWorker(Unit u) {
		for (Base b : bases) {
			for (Entry<Integer, Worker> w : b.workers.entrySet()) {
				if (w.getValue().getID() == u.getID()) {
					return w.getValue();
				}
			}
		}
		return null;
	}

	public Resource getResource(Unit u) {
		for (Base b : bases) {
			for (Entry<Integer, Resource> mineral : b.minerals.entrySet()) {
				if (mineral.getValue().getID() == u.getID()) {
					return mineral.getValue();
				}
			}
			for (Entry<Integer, GasResource> gas : b.gas.entrySet()) {
				if (gas.getValue().getID() == u.getID()) {
					return gas.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public void registerDebugFunctions(GameHandler g) {
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				if (main != null) {
					engine.drawText(main.location.getX(), main.location.getY(),
							"Main", false);
				}

				for (Base b : bases) {
					if (b.commandCenter != null) {
						int x = b.commandCenter.getTileX() * 32;
						int y = b.commandCenter.getTileY() * 32;
						engine.drawBox(x, y, x + 32 * 4, y + 32 * 3,
								BWColor.TEAL, false, false);
						engine.drawText(x + 5, y + 5,
								"Workers: " + b.getWorkerCount(), false);
						engine.drawText(x + 5, y + 15, "Mineral Fields: "
								+ b.minerals.size(), false);
					}
					// Minerals
					for (Resource r : b.minerals.values()) {
						engine.drawText(r.getX() - 8, r.getY() - 8,
								String.valueOf(r.getNumGatherers()), false);
					}
					// Worker
					for (Worker w : b.workers.values()) {
						if (w.getCurrentTask() == WorkerTask.Mining_Minerals) {
							engine.drawCircle(w.getX(), w.getY(), 3,
									BWColor.BLUE, true, false);
						} else if (w.getCurrentTask() == WorkerTask.Mining_Gas) {
							engine.drawCircle(w.getX(), w.getY(), 3,
									BWColor.GREEN, true, false);
						} else if (w.getCurrentTask() == WorkerTask.Constructing_Building) {
							engine.drawCircle(w.getX(), w.getY(), 3,
									BWColor.ORANGE, true, false);
						}
					}
				}
			}
		});
	}
}
