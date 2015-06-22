package datastructure;

import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;

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
	private static Map<BaseLocation, Base> bases;
	public static Base main;
	/**
	 * The radius the bot looks around a potential base location to determine if
	 * it is occupied
	 **/
	private static int baseRadius = 300;

	public static void init() {
		System.out.print("Starting BaseManager... ");
		bases = new HashMap<>();
		// Add bases
		for (BaseLocation location : BWTA.getBaseLocations()) {
			Base b = new Base(location);
			bases.put(location, b);
		}
		// Sort resources
		for (Unit u : GameHandler.getAllUnits()) {
			UnitType type = u.getType();
			Optional<Base> closestBase = getClosestBase(u.getPosition());
			System.out.println(type.isResourceDepot());
			if (closestBase.isPresent()) {
				if (type.isMineralField()) {
					closestBase.get().minerals.add(new MineralResource(u));
				} else if (type.equals(UnitType.Resource_Vespene_Geyser)) {
					closestBase.get().gas.add(new GasResource(u));
				} else if (type.isResourceDepot()) {
					System.out.println("Found resource depot!");

					closestBase.get().setPlayer(u.getPlayer());
					System.out.println("Found a base for " + u.getPlayer());
				}
			}
		}
		System.out.print("Searching for main... ");
		// First base is main
		BaseManager.main = BaseManager.getMyBases().stream().findAny().get();

		registerDebugFunctions();
		System.out.println("Success!");
	}

	/** This constructor should never be called. */
	private BaseManager() {
	}

	public static void gatherResources() {
		for (Base b : bases.values()) {
			b.gatherResources();
		}
	}

	public static Set<Base> getMyBases() {
		Set<Base> myBases = new HashSet<Base>();
		bases.forEach((k, v) -> {
			if (v.getPlayer() == GameHandler.getSelfPlayer()) {
				myBases.add(v);
			}
		});
		return myBases;
	}

	public static Optional<Base> getClosestBase(Position position) {
		int closestDistance = Integer.MAX_VALUE;
		Base closestBase = null;
		for (Base b : bases.values()) {
			int distance = b.getLocation().getPosition()
					.getApproxDistance(position);
			if (distance < closestDistance) {
				closestBase = b;
				closestDistance = distance;
			}
		}

		return Optional.ofNullable(closestBase);
	}

	public static Optional<Worker> getBuilder() {
		for (Base b : bases.values()) {
			Worker u = b.getBuilder();
			if (u != null) {
				return Optional.of(u);
			}
		}
		return Optional.empty();
	}

	public static void unitDestroyed(Unit unit) {
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

	public static Collection<Base> getBases() {
		return bases.values();
	}

	public static Optional<Worker> getWorker(Unit u) {
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

	public static void refineryComplete(Unit u) {
		getClosestBase(u.getPosition()).ifPresent(
				b -> b.gas.add(new GasResource(u)));
	}

	public static Optional<Resource> getResource(Unit u) {
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

	public static void resourceDepotShown(Unit unit) {
		getClosestBase(unit.getPosition()).ifPresent(b -> {
			b.commandCenter = Optional.of(unit);
			b.setPlayer(unit.getPlayer());
		});
	}

	public static void resourceDepotDestroyed(Unit unit) {
		getClosestBase(unit.getPosition()).ifPresent(b -> {
			b.commandCenter = Optional.empty();
			b.setPlayer(GameHandler.getNeutralPlayer());
		});
	}

	public static void resourceDepotHidden(Unit unit) {
		getClosestBase(unit.getPosition()).ifPresent(b -> b.setLastScouted());
	}

	public static void workerComplete(Unit unit) {
		getClosestBase(unit.getPosition()).ifPresent(b -> b.addWorker(unit));
	}

	public static void registerDebugFunctions() {
		DebugModule bases = DebugManager.createDebugModule("bases");
		bases.addSubmodule("main").setDraw(() -> {
			if (main != null) {
				DrawEngine.drawTextMap(main.getX(), main.getY(), "Main");
			}
		});
		bases.addSubmodule("status")
				.setDraw(
						() -> {
							for (Base b : BaseManager.bases.values()) {
								// Status
								DrawEngine.drawCircleMap(b.getX(), b.getY(),
										100, Color.Teal, false);
								DrawEngine.drawTextMap(b.getX() + 5,
										b.getY() + 5, "Status: "
												+ b.getPlayer().toString()
												+ " @ " + b.getLastScouted());
							}
						});
		bases.addSubmodule("commandcenter")
				.setDraw(() -> {
					for (Base b : BaseManager.bases.values()) {
						// Command center
						if (b.commandCenter.isPresent()) {
							Unit c = b.commandCenter.get();
							int tx = c.getTilePosition().getX();
							int ty = c.getTilePosition().getY();
							DrawEngine.drawBoxMap(tx * 32, ty * 32,
									(tx + 4) * 32, (ty + 3) * 32, Color.Teal,
									false);
						}
					}
				}).addAlias("cc");
		bases.addSubmodule("resources").setDraw(() -> {
			for (Base b : BaseManager.bases.values()) {
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
		bases.addSubmodule("workers")
				.setDraw(() -> {
					for (Base b : BaseManager.bases.values()) {
						// Miner counts
						DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 15,
								"Mineral Miners: " + b.getMineralWorkerCount());
						DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 25,
								"Mineral Fields: " + b.minerals.size());

						// Workers
						for (Worker w : b.workers) {
							if (w.getTask() == WorkerTask.Mining_Minerals) {
								DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
										Color.Blue, true);
							} else if (w.getTask() == WorkerTask.Mining_Gas) {
								DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
										Color.Green, true);
							} else if (w.getTask() == WorkerTask.Constructing_Building) {
								DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
										Color.Orange, true);
							}
						}
					}
				}).addAlias("miners");
	}
}
