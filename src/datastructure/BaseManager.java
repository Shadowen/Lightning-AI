package datastructure;

import static gamestructure.debug.DebugManager.debugManager;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.InvalidCommandException;
import gamestructure.debug.ShapeOverflowException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public final class BaseManager {
	private Map<BaseLocation, Base> bases;
	public Base main;
	/**
	 * The radius the bot looks around a potential base location to determine if
	 * it is occupied
	 **/
	private int baseRadius = 300;
	private static BaseManager theInstance;

	public static BaseManager baseManager() {
		if (theInstance == null) {
			theInstance = new BaseManager();
		}
		return theInstance;
	}

	private BaseManager() {
		bases = new HashMap<>();
		// Add bases
		for (BaseLocation location : BWTA.getBaseLocations()) {
			Base b = new Base(location);
			bases.put(location, b);
			GameHandler.getClosestUnit(location.getX(), location.getY(), 50,
					UnitType.Terran_Command_Center).ifPresent(
					cc -> resourceDepotShown(cc));
		}
		// Sort resources
		for (Unit u : GameHandler.getNeutralUnits()) {
			UnitType type = u.getType();
			Optional<Base> closestBase = getClosestBase(u.getPosition(),
					baseRadius);
			if (closestBase.isPresent()) {
				if (type.isMineralField()) {
					closestBase.get().minerals.add(new MineralResource(u));
				} else if (type.equals(UnitType.Resource_Vespene_Geyser)) {
					closestBase.get().gas.add(new GasResource(u));
				}
			}
		}
		// First base is main
		main = getMyBases().stream().findAny().get();

		registerDebugFunctions();
	}

	public void gatherResources() {
		for (Base b : bases.values()) {
			b.gatherResources();
		}
	}

	public Set<Base> getMyBases() {
		Set<Base> myBases = new HashSet<Base>();
		bases.forEach((k, v) -> {
			if (v.getPlayer() == GameHandler.getSelfPlayer()) {
				myBases.add(v);
			}
		});
		return myBases;
	}

	private Optional<Base> getClosestBase(Position position, int maxDistance) {
		Base closest = null;
		for (Base b : bases.values()) {
			if (closest == null
					|| b.getLocation().getPosition().getDistance(position) < closest
							.getLocation().getPosition().getDistance(position)) {
				closest = b;
			}
		}
		return Optional.ofNullable(closest);
	}

	public Optional<Worker> getBuilder() {
		for (Base b : bases.values()) {
			Worker u = b.getBuilder();
			if (u != null) {
				return Optional.of(u);
			}
		}
		return Optional.empty();
	}

	public void unitDestroyed(Unit unit) {
		UnitType type = unit.getType();
		if (type == UnitType.Terran_SCV) {
			for (Base b : bases.values()) {
				if (b.removeWorker(unit)) {
					break;
				}
			}
		} else if (type.isMineralField()) {
			for (Base b : bases.values()) {
				if (b.minerals.remove(unit)) {
					break;
				}
			}
		} else if (type.equals(UnitType.Resource_Vespene_Geyser)
				|| type.isRefinery()) {
			for (Base b : bases.values()) {
				if (b.gas.remove(unit)) {
					break;
				}
			}
		}
	}

	public Collection<Base> getBases() {
		return bases.values();
	}

	public Optional<Worker> getWorker(Unit u) {
		// Find the right worker
		int uid = u.getID();
		for (Base b : bases.values()) {
			Worker w = b.workers.get(uid);
			if (w != null) {
				return Optional.of(w);
			}
		}
		return Optional.empty();
	}

	public void refineryComplete(Unit u) {
		getClosestBase(u.getPosition(), baseRadius).ifPresent(
				b -> b.gas.add(new GasResource(u)));
	}

	public Optional<Resource> getResource(Unit u) {
		if (u.getType().isMineralField()) {
			return bases.values().stream().flatMap(b -> b.minerals.stream())
					.filter(m -> m.getUnit() == u).findAny().map(m -> m);
		} else if (u.getType().isRefinery()) {
			return bases.values().stream().flatMap(b -> b.gas.stream())
					.filter(g -> g.getUnit() == u).findAny().map(g -> g);
		}
		System.err
				.println("Searching for a resource corresponding to a unit that is not a resource...");
		return Optional.empty();
	}

	public void resourceDepotShown(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(b -> {
			b.commandCenter = Optional.of(unit);
			b.setPlayer(unit.getPlayer());
		});
	}

	public void resourceDepotDestroyed(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(b -> {
			b.commandCenter = Optional.empty();
			b.setPlayer(GameHandler.getNeutralPlayer());
		});
	}

	public void resourceDepotHidden(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(
				b -> b.setLastScouted());
	}

	public void workerComplete(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(
				b -> b.addWorker(unit));
	}

	public void registerDebugFunctions() {
		DebugModule basesModule = debugManager().createDebugModule("bases");
		basesModule.addSubmodule("main").setDraw(() -> {
			if (main != null) {
				DrawEngine.drawTextMap(main.getX(), main.getY(), "Main");
			}
		});
		basesModule.addSubmodule("status")
				.setDraw(
						() -> {
							for (Base b : bases.values()) {
								// Status
								DrawEngine.drawCircleMap(b.getX(), b.getY(),
										100, Color.Teal, false);
								DrawEngine.drawTextMap(b.getX() + 5,
										b.getY() + 5, "Status: "
												+ b.getPlayer().toString()
												+ " @ " + b.getLastScouted());
							}
						});
		try {
			basesModule
					.addSubmodule("commandcenter")
					.setDraw(() -> {
						for (Base b : bases.values()) {
							// Command center
							if (b.commandCenter.isPresent()) {
								Unit c = b.commandCenter.get();
								int tx = c.getTilePosition().getX();
								int ty = c.getTilePosition().getY();
								DrawEngine.drawBoxMap(tx * 32, ty * 32,
										(tx + 4) * 32, (ty + 3) * 32,
										Color.Teal, false);
							}
						}
					}).addAlias("cc");
		} catch (InvalidCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		basesModule.addSubmodule("resources").setDraw(() -> {
			for (Base b : bases.values()) {
				// Minerals
				for (MineralResource r : b.minerals) {
					DrawEngine.drawTextMap(r.getX() - 8, r.getY() - 8,
							String.valueOf(r.getNumGatherers()));
				}
				// Gas
				for (GasResource r : b.gas) {
					if (!r.gasTaken()) {
						DrawEngine.drawTextMap(r.getX(), r.getY(), "Geyser");
					} else {
						DrawEngine.drawTextMap(r.getX(), r.getY(), "Refinery");
					}
				}
			}
		});
		try {
			basesModule
					.addSubmodule("workers")
					.setDraw(() -> {
						for (Base b : bases.values()) {
							// Miner counts
							DrawEngine.drawTextMap(
									b.getX() + 5,
									b.getY() + 15,
									"Mineral Miners: "
											+ b.getMineralWorkerCount());
							DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 25,
									"Mineral Fields: " + b.minerals.size());

							// Workers
							for (Worker w : b.workers) {
								if (w.getTask() == WorkerTask.Mining_Minerals) {
									DrawEngine.drawCircleMap(w.getX(),
											w.getY(), 3, Color.Blue, true);
								} else if (w.getTask() == WorkerTask.Mining_Gas) {
									DrawEngine.drawCircleMap(w.getX(),
											w.getY(), 3, Color.Green, true);
								} else if (w.getTask() == WorkerTask.Constructing_Building) {
									DrawEngine.drawCircleMap(w.getX(),
											w.getY(), 3, Color.Orange, true);
								}
							}
						}
					}).addAlias("miners");
		} catch (InvalidCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
