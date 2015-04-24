package datastructure;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;
import gamestructure.DebugEngine;
import gamestructure.DebugModule;
import gamestructure.Debuggable;
import gamestructure.GameHandler;
import gamestructure.ShapeOverflowException;

public class BaseManager implements Iterable<Base>, Debuggable {
	private GameHandler game;
	private Set<Base> bases;
	public Base main;

	public BaseManager(GameHandler g) {
		game = g;
		bases = new HashSet<Base>();
		for (BaseLocation location : BWTA.getBaseLocations()) {
			Base b = new Base(game, location);
			bases.add(b);
		}
		for (Unit u : game.getNeutralUnits()) {
			UnitType type = u.getType();
			Base closestBase = getClosestBase(u.getPosition());
			if (type == UnitType.Resource_Mineral_Field
					|| type == UnitType.Resource_Mineral_Field_Type_2
					|| type == UnitType.Resource_Mineral_Field_Type_3) {
				closestBase.minerals.add(new MineralResource(u));
			} else if (type == UnitType.Resource_Vespene_Geyser) {
				closestBase.gas.add(new GasResource(u));
			}
		}
	}

	public Set<Base> getMyBases() {
		Set<Base> myBases = new HashSet<Base>();
		for (Base b : bases) {
			if (b.getPlayer() == game.getSelfPlayer()) {
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

	private Base getClosestBase(Position position) {
		return getClosestBase(position.getX(), position.getY());
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

	public void resourceDepotShown(Unit unit) {
		Base b = getClosestBase(unit.getPosition());
		b.commandCenter = unit;
		b.setPlayer(unit.getPlayer());
	}

	public void resourceDepotDestroyed(Unit unit) {
		Base b = getClosestBase(unit.getPosition());
		b.commandCenter = null;
		b.setPlayer(game.getNeutralPlayer());
	}

	public void resourceDepotHidden(Unit unit) {
		Base b = getClosestBase(unit.getPosition());
		b.setLastScouted();
	}

	@Override
	public void registerDebugFunctions(DebugEngine debugEngine) {
		debugEngine.registerDebugFunction(new DebugModule("bases") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				if (main != null) {
					engine.drawText(main.getX(), main.getY(), "Main", false);
				}

				for (Base b : bases) {
					// Status
					engine.drawCircle(b.getX(), b.getY(), 100, Color.Teal,
							false, false);

					engine.drawText(
							b.getX() + 5,
							b.getY() + 5,
							"Status: " + b.getPlayer().getName() + " @ "
									+ b.getLastScouted(), false);

					// Command center
					if (b.commandCenter != null) {
						int tx = b.commandCenter.getTilePosition().getX();
						int ty = b.commandCenter.getTilePosition().getY();
						engine.drawBox(tx * 32, ty * 32, (tx + 4) * 32,
								(ty + 3) * 32, Color.Teal, false, false);
					}

					// Minerals
					for (MineralResource r : b.minerals) {
						engine.drawText(r.getX() - 8, r.getY() - 8,
								String.valueOf(r.getNumGatherers()), false);
					}

					// Miner counts
					int bx = b.getX() - 60;
					int by = b.getY() - 48;
					engine.drawText(bx, by,
							"Mineral Miners: " + b.getMineralWorkerCount(),
							false);
					engine.drawText(bx, by + 10,
							"Mineral Miners: " + b.getMineralWorkerCount(),
							false);
					engine.drawText(bx, by + 20, "Mineral Fields: "
							+ b.minerals.size(), false);
					engine.drawText(bx, by + 30, "Gas Miners: " + "TODO", false); // TODO
					engine.drawText(bx, by + 40,
							"Gas Geysers: " + b.gas.size(), false);

					// Workers
					for (Worker w : b.workers) {
						if (w.getTask() == WorkerTask.Mining_Minerals) {
							engine.drawCircle(w.getX(), w.getY(), 3,
									Color.Blue, true, false);
						} else if (w.getTask() == WorkerTask.Mining_Gas) {
							engine.drawCircle(w.getX(), w.getY(), 3,
									Color.Green, true, false);
						} else if (w.getTask() == WorkerTask.Constructing_Building) {
							engine.drawCircle(w.getX(), w.getY(), 3,
									Color.Orange, true, false);
						}
					}
				}
			}
		});
	}
}
