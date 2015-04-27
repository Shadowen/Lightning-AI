package datastructure;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import bwapi.Color;
import bwapi.Unit;
import gamestructure.DebugEngine;
import gamestructure.DebugModule;
import gamestructure.Debuggable;
import gamestructure.GameHandler;
import gamestructure.ShapeOverflowException;

public class BaseManager implements Iterable<Base>, Debuggable {
	private Set<Base> bases;
	public Base main;

	public BaseManager(GameHandler game, DebugEngine debugEngine) {
		bases = new HashSet<Base>();
		registerDebugFunctions(debugEngine);
	}

	public void addBase(Base b) {
		bases.add(b);
	}

	public Set<Base> getMyBases() {
		Set<Base> myBases = new HashSet<Base>();
		for (Base b : bases) {
			if (b.getStatus() == BaseStatus.OCCUPIED_SELF) {
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
			double distance = Point.distance(x, y, b.getX(), b.getY());
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

	public void removeWorker(Unit unit) {
		for (Base b : bases) {
			if (b.removeWorker(unit)) {
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
		// Null check
		if (u == null) {
			return null;
		}

		// Find the right worker
		int uid = u.getID();
		for (Base b : bases) {
			Worker w = b.workers.get(uid);
			if (w != null) {
				return w;
			}
		}
		return null;
	}

	public Resource getResource(Unit u) {
		for (Base b : bases) {
			for (MineralResource mineral : b.minerals) {
				if (mineral.getUnit() == u) {
					return mineral;
				}
			}
			for (GasResource gas : b.gas) {
				if (gas.getUnit() == u) {
					return gas;
				}
			}
		}
		return null;
	}

	@Override
	public void registerDebugFunctions(DebugEngine debugEngine) {
		debugEngine.registerDebugFunction(new DebugModule("bases") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				if (main != null) {
					engine.drawTextMap(main.getX(), main.getY(), "Main");
				}

				for (Base b : bases) {
					// Status
					engine.drawCircleMap(b.getX(), b.getY(), 100, Color.Teal,
							false);
					engine.drawTextMap(
							b.getX() + 5,
							b.getY() + 5,
							"Status: " + b.getStatus().toString() + " @ "
									+ b.getLastScouted());

					// Command center
					if (b.commandCenter != null) {
						int tx = b.commandCenter.getTilePosition().getX();
						int ty = b.commandCenter.getTilePosition().getY();
						engine.drawBoxMap(tx * 32, ty * 32, (tx + 4) * 32,
								(ty + 3) * 32, Color.Teal, false);
					}

					// Minerals
					for (MineralResource r : b.minerals) {
						engine.drawTextMap(r.getX() - 8, r.getY() - 8,
								String.valueOf(r.getNumGatherers()));
					}

					// Miner counts
					engine.drawTextMap(b.getX() + 5, b.getY() + 15,
							"Mineral Miners: " + b.getMineralWorkerCount());
					engine.drawTextMap(b.getX() + 5, b.getY() + 25,
							"Mineral Fields: " + b.minerals.size());

					// Workers
					for (Worker w : b.workers) {
						if (w.getTask() == WorkerTask.Mining_Minerals) {
							engine.drawCircleMap(w.getX(), w.getY(), 3,
									Color.Blue, true);
						} else if (w.getTask() == WorkerTask.Mining_Gas) {
							engine.drawCircleMap(w.getX(), w.getY(), 3,
									Color.Green, true);
						} else if (w.getTask() == WorkerTask.Constructing_Building) {
							engine.drawCircleMap(w.getX(), w.getY(), 3,
									Color.Orange, true);
						}
					}
				}
			}
		});
	}
}
