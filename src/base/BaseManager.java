package base;

import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;
import micro.MicroManager;
import micro.UnitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import build.BuildManager;

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
	private static Set<Unit> enemyBuildings;
	public static Base main;
	public static Base natural;
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
			Optional<Base> closestBase = getClosestBase(u.getPosition(), baseRadius);
			if (closestBase.isPresent()) {
				if (type.isMineralField()) {
					closestBase.get().minerals.add(new MineralResource(u));
				} else if (type.equals(UnitType.Resource_Vespene_Geyser)) {
					closestBase.get().gas.add(new GasResource(u));
				} else if (type.isResourceDepot()) {
					closestBase.get().commandCenter = Optional.of(u);
					closestBase.get().setPlayer(u.getPlayer());
				}
			}
		}
		System.out.print("Searching for main... ");
		// First base is main
		try {
			main = getMyBases().stream().findAny().orElseThrow(() -> new NoMainFoundException());
			natural = bases.values().stream().filter(b -> main != b)
					.sorted((b1,
							b2) -> (int) (b1.getLocation().getGroundDistance(main.getLocation())
									- b2.getLocation().getGroundDistance(main.getLocation())) * 1000)
					.findFirst().orElseThrow(() -> new NoNaturalFoundException());
		} catch (NoMainFoundException e) {
			e.printStackTrace();
		} catch (NoNaturalFoundException e) {
			e.printStackTrace();
		}

		enemyBuildings = new HashSet<>();

		registerDebugFunctions();
		System.out.println("Success!");
	}

	/** This constructor should never be called. */
	private BaseManager() {
	}

	/** Checks the occupancy of bases */
	public static void checkBaseOccupancy() {
		for (Entry<BaseLocation, Base> e : bases.entrySet()) {
			BaseLocation bl = e.getKey();
			Base b = e.getValue();
			if (GameHandler.isVisible(bl.getTilePosition())) {
				b.setLastScouted();
			}
		}
	}

	public static void onFrame() {
		checkBaseOccupancy();

		for (Base b : getMyBases()) {
			// Refinery
			if (b.getMineralWorkerCount() >= 1.5 * b.getMineralCount() && b.gas.stream().anyMatch(r -> !r.gasTaken())) {
				try {
					b.takeGas();
				} catch (NoGeyserAvailableException e) {
					e.printStackTrace();
				}
			}
		}
		bases.values().stream().filter(b -> b.workers.size() < b.minerals.size() * 2).map(b -> b.commandCenter)
				.filter(o -> o.isPresent()).map(o -> o.get()).filter(c -> !c.isTraining()).forEach(c -> {
					if (GameHandler.getSelfPlayer().minerals() >= 50)
						c.train(UnitType.Terran_SCV);
				});
	}

	public static void expand() {
		// Expand to the base that is closest by ground
		bases.values().stream().filter(b -> b.getPlayer() == GameHandler.getNeutralPlayer()).map(b -> b.getLocation())
				.sorted((b1, b2) -> BWTA.getGroundDistance2(main.getLocation().getTilePosition(), b2.getTilePosition())
						- BWTA.getGroundDistance2(main.getLocation().getTilePosition(), b1.getTilePosition()))
				.findFirst()
				.ifPresent(l -> BuildManager.addBuilding(l.getTilePosition(), UnitType.Terran_Command_Center));
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
			int distance = b.getLocation().getPosition().getApproxDistance(position);
			if (distance < closestDistance) {
				closestBase = b;
				closestDistance = distance;
			}
		}
		return Optional.ofNullable(closestBase);
	}

	public static Optional<Base> getClosestBase(Position position, int maxDistance) {
		int closestDistance = Integer.MAX_VALUE;
		Base closestBase = null;
		for (Base b : bases.values()) {
			int distance = b.getLocation().getPosition().getApproxDistance(position);
			if (distance < maxDistance && distance < closestDistance) {
				closestBase = b;
				closestDistance = distance;
			}
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
		if (unit.getType().isResourceDepot()) {
			getClosestBase(unit.getPosition()).ifPresent(b -> {
				b.commandCenter = Optional.of(unit);
				b.setPlayer(unit.getPlayer());
			});
		}
	}

	public static void onUnitShow(Unit unit) {
		if (unit.getPlayer() == GameHandler.getEnemyPlayer() && unit.getType().isBuilding()) {
			enemyBuildings.add(unit);
		}

		if (unit.getType().isResourceDepot()) {
			getClosestBase(unit.getPosition()).ifPresent(b -> {
				b.commandCenter = Optional.of(unit);
				b.setPlayer(unit.getPlayer());
			});
		}
	}

	public static void unitConstructed(Unit u) {
		if (u.getPlayer() == GameHandler.getSelfPlayer()) {
			if (u.getType().isRefinery()) {
				getResource(u).ifPresent(r -> getClosestBase(u.getPosition()).ifPresent(b -> {
					GasResource gr = (GasResource) r;
					while (gr.getNumGatherers() < 3) {
						b.getFreeWorker().setTaskMiningGas(gr);
					}
				}));
			}
		}
	}

	public static void unitDestroyed(Unit unit) {
		UnitType type = unit.getType();
		if (type.isWorker()) {
			Worker w = (Worker) MicroManager.getAgentForUnit(unit);
			w.unitDestroyed();
			w.getBase().removeWorker(w);
		} else if (type.isMineralField()) {
			for (Base b : bases.values()) {
				if (b.minerals.remove(unit)) {
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

	public static Optional<Resource> getResource(Unit u) {
		if (u.getType().isMineralField()) {
			return bases.values().stream().flatMap(b -> b.minerals.stream()).filter(m -> m.getUnit() == u).findAny()
					.map(m -> m);
		} else if (u.getType().isRefinery()) {
			return bases.values().stream().flatMap(b -> b.gas.stream()).filter(g -> g.getUnit() == u).findAny()
					.map(g -> g);
		}
		System.err.println("Searching for a resource corresponding to a unit that is not a resource...");
		return Optional.empty();
	}

	public static void registerDebugFunctions() {
		DebugModule bases = DebugManager.createDebugModule("bases");
		bases.addSubmodule("main").setDraw(() -> {
			if (main != null) {
				DrawEngine.drawTextMap(main.getX() - 50, main.getY() - 50, "Main");
			}
			if (natural != null) {
				DrawEngine.drawTextMap(natural.getX() - 50, natural.getY() - 50, "Natural");
			}
		});
		bases.addSubmodule("status").setDraw(() -> {
			for (Base b : BaseManager.bases.values()) {
				// Status
				DrawEngine.drawCircleMap(b.getX(), b.getY(), baseRadius, Color.Teal, false);
				String playerType;
				if (b.getPlayer() == GameHandler.getSelfPlayer()) {
					playerType = "SELF";
				} else if (b.getPlayer() == GameHandler.getNeutralPlayer()) {
					playerType = "NEUTRAL";
				} else if (b.getPlayer() == GameHandler.getEnemyPlayer()) {
					playerType = "ENEMY";
				} else {
					playerType = "UNKNOWN";
				}

				DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 5,
						"Status: " + playerType + " @ " + b.getLastScouted());
				if (b.getLocation().isStartLocation()) {
					DrawEngine.drawTextMap(b.getX() + 5, b.getY() - 5, "Starting Location");
				}
			}
		});
		bases.addSubmodule("commandcenter").setDraw(() -> {
			for (Base b : BaseManager.bases.values()) {
				// Command center
				if (b.commandCenter.isPresent()) {
					Unit c = b.commandCenter.get();
					int tx = c.getTilePosition().getX();
					int ty = c.getTilePosition().getY();
					DrawEngine.drawBoxMap(tx * 32, ty * 32, (tx + 4) * 32, (ty + 3) * 32, Color.Teal, false);
				}
			}
		}).addAlias("cc");
		bases.addSubmodule("resources").setDraw(() -> {
			for (Base b : BaseManager.bases.values()) {
				// Minerals
				for (MineralResource r : b.minerals) {
					DrawEngine.drawTextMap(r.getX() - 8, r.getY() - 8, String.valueOf(r.getNumGatherers()));
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
				DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 15, "Mineral Miners: " + b.getMineralWorkerCount());
				DrawEngine.drawTextMap(b.getX() + 5, b.getY() + 25, "Mineral Fields: " + b.minerals.size());
				for (Worker w : b.workers) {
					DrawEngine.drawLineMap(b.getX(), b.getY(), w.unit.getX(), w.unit.getY(), Color.Brown);
				}
			}
			long idleWorkers = BaseManager.bases.values().stream().flatMap(b -> b.workers.stream())
					.filter(w -> w.unit.isIdle()).count();
			DrawEngine.drawTextScreen(550, 50, "Idle workers: " + idleWorkers);
		});
	}
}
