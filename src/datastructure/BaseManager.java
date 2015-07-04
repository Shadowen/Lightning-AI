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
	private static Map<Unit, Worker> workers;
	private static Set<Unit> enemyBuildings;
	public static Base main;
	/**
	 * The radius the bot looks around a potential base location to determine if
	 * it is occupied
	 **/
	private static int baseRadius = 300;

	public static void init() {
		System.out.print("Starting BaseManager... ");
		workers = new HashMap<>();
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
			if (closestBase.isPresent()) {
				if (type.isMineralField()) {
					closestBase.get().minerals.add(new MineralResource(u));
				} else if (type.equals(UnitType.Resource_Vespene_Geyser)) {
					closestBase.get().gas.add(new GasResource(u));
				} else if (type.isResourceDepot()) {
					System.out.println("Found resource depot!");
					closestBase.get().commandCenter = Optional.of(u);
					closestBase.get().setPlayer(u.getPlayer());
					System.out.println("Found a base for Player "
							+ u.getPlayer().getID());
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

	public static void onFrame() {
		bases.forEach((bl, b) -> b.commandCenter.map(c -> c.getPosition())
				.flatMap(p -> getClosestBase(p))
				.ifPresent(bs -> bs.setLastScouted()));
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
		if (closestBase == null) {
			System.out.println("No base found!");
		}
		return Optional.ofNullable(closestBase);
	}

	public static Optional<Worker> getFreeWorker() {
		for (Base b : bases.values()) {
			Worker u = b.getFreeWorker();
			if (u != null) {
				return Optional.of(u);
			}
		}
		return Optional.empty();
	}

	public static void unitCreated(Unit unit) {
		if (unit.getType().isWorker()
				&& unit.getPlayer() == GameHandler.getSelfPlayer()) {
			workers.put(unit, new Worker(unit));
		} else if (unit.getType().isResourceDepot()) {
			getClosestBase(unit.getPosition()).ifPresent(b -> {
				b.commandCenter = Optional.of(unit);
				b.setPlayer(unit.getPlayer());
			});
		}
	}

	public static void unitShown(Unit unit) {
		if (unit.getPlayer() == GameHandler.getEnemyPlayer()
				&& unit.getType().isBuilding()) {
			enemyBuildings.add(unit);
		}

		if (unit.getType().isResourceDepot()) {
			getClosestBase(unit.getPosition()).ifPresent(b -> {
				b.commandCenter = Optional.of(unit);
				b.setPlayer(unit.getPlayer());
			});
		}
	}

	public static void unitComplete(Unit u) {
		if (u.getPlayer() == GameHandler.getSelfPlayer()) {
			if (u.getType().isWorker()) {
				Worker w = workers.get(u);
				getClosestBase(u.getPosition()).ifPresent(b -> b.addWorker(w));
				w.setTask(WorkerTask.MINERALS, null);
			}
		}
	}

	public static void unitDestroyed(Unit unit) {
		UnitType type = unit.getType();
		if (type.isWorker()) {
			for (Base b : bases.values()) {
				if (b.removeWorker(workers.get(unit))) {
					workers.remove(unit);
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
		} else if (type.isResourceDepot()) {
			getClosestBase(unit.getPosition()).ifPresent(b -> {
				b.commandCenter = Optional.empty();
				b.setPlayer(GameHandler.getNeutralPlayer());
			});
		}
	}

	public static Collection<Base> getBases() {
		return bases.values();
	}

	public static Optional<Worker> getWorker(Unit u) {
		return Optional.ofNullable(workers.get(u));
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
		bases.addSubmodule("miners").setDraw(() -> {
			for (Base b : BaseManager.bases.values()) {
				// Miner counts
				DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 15,
						"Mineral Miners: " + b.getMineralWorkerCount());
				DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 25,
						"Mineral Fields: " + b.minerals.size());
			}
		});
		bases.addSubmodule("workers").setDraw(() -> {
			// Workers
				for (Worker w : workers.values()) {
					switch (w.getTask()) {
					case CONSTRUCTING:
						DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
								Color.Orange, true);
						break;
					case GAS:
						DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
								Color.Green, true);
						DrawEngine.drawLineMap(w.getX(), w.getY(), w
								.getCurrentResource().getX(), w
								.getCurrentResource().getY(), Color.Green);
						break;
					case MINERALS:
						DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
								Color.Blue, true);
						DrawEngine.drawLineMap(w.getX(), w.getY(), w
								.getCurrentResource().getX(), w
								.getCurrentResource().getY(), Color.Blue);
						break;
					case SCOUTING:
						DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
								Color.White, true);
						break;
					case TRAINING:
						DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
								Color.Teal, true);
						break;
					default:
						DrawEngine.drawCircleMap(w.getX(), w.getY(), 3,
								Color.Black, true);
						break;
					}
				}
			});
	}
}
