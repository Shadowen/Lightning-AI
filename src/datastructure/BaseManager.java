package datastructure;

import gamestructure.GameHandler;
import gamestructure.debug.DebugEngine;

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

	static {
		bases = new HashMap<>();
		// Add bases
		for (BaseLocation location : BWTA.getBaseLocations()) {
			Base b = new Base(location);
			bases.put(location, b);
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
		BaseManager.main = BaseManager.getMyBases().iterator().next();

		registerDebugFunctions();
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

	private static Optional<Base> getClosestBase(Position position,
			int maxDistance) {
		return Optional.ofNullable(bases.get(BWTA
				.getNearestBaseLocation(position)));
	}

	public static Optional<Worker> getBuilder() {
		// TODO Java 8 stream style implementation?
		// bases.values()
		// .stream()
		// .flatMap(
		// b -> b.workers.stream().filter(
		// w -> w.getTask() == WorkerTask.Mining_Minerals))
		// .findAny();
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

	public static Worker getWorker(Unit u) {
		// Null check
		if (u == null) {
			return null;
		}

		// Find the right worker
		int uid = u.getID();
		for (Base b : bases.values()) {
			Worker w = b.workers.get(uid);
			if (w != null) {
				return w;
			}
		}
		return null;
	}

	public static void refineryComplete(Unit u) {
		getClosestBase(u.getPosition(), baseRadius).ifPresent(
				b -> b.gas.add(new GasResource(u)));
	}

	public static Resource getResource(Unit u) {
		for (Base b : bases.values()) {
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

	public static void resourceDepotShown(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(b -> {
			b.commandCenter = Optional.of(unit);
			b.setPlayer(unit.getPlayer());
		});
	}

	public static void resourceDepotDestroyed(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(b -> {
			b.commandCenter = Optional.empty();
			b.setPlayer(GameHandler.getNeutralPlayer());
		});
	}

	public static void resourceDepotHidden(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(
				b -> b.setLastScouted());
	}

	public static void workerComplete(Unit unit) {
		getClosestBase(unit.getPosition(), baseRadius).ifPresent(
				b -> b.addWorker(unit));
	}

	public static void registerDebugFunctions() {
		DebugEngine
				.createDebugModule("bases")
				.setDraw(
						() -> {
							if (main != null) {
								DebugEngine.drawTextMap(main.getX(),
										main.getY(), "Main");
							}

							for (Base b : bases.values()) {
								// Status
								DebugEngine.drawCircleMap(b.getX(), b.getY(),
										100, Color.Teal, false);
								DebugEngine.drawTextMap(b.getX() + 5,
										b.getY() + 5, "Status: "
												+ b.getPlayer().toString()
												+ " @ " + b.getLastScouted());

								// Command center
								if (b.commandCenter.isPresent()) {
									Unit c = b.commandCenter.get();
									int tx = c.getTilePosition().getX();
									int ty = c.getTilePosition().getY();
									DebugEngine.drawBoxMap(tx * 32, ty * 32,
											(tx + 4) * 32, (ty + 3) * 32,
											Color.Teal, false);
								}

								// Minerals
								for (MineralResource r : b.minerals) {
									DebugEngine.drawTextMap(r.getX() - 8,
											r.getY() - 8,
											String.valueOf(r.getNumGatherers()));
								}
								// Gas
								for (GasResource r : b.gas) {
									if (!r.gasTaken()) {
										DebugEngine.drawTextMap(r.getX(),
												r.getY(), "Geyser");
									} else {
										DebugEngine.drawTextMap(r.getX(),
												r.getY(), "Refinery");
									}
								}
								// Miner counts
								DebugEngine.drawTextMap(
										b.getX() + 5,
										b.getY() + 15,
										"Mineral Miners: "
												+ b.getMineralWorkerCount());
								DebugEngine.drawTextMap(b.getX() + 5,
										b.getY() + 25, "Mineral Fields: "
												+ b.minerals.size());

								// Workers
								for (Worker w : b.workers) {
									if (w.getTask() == WorkerTask.Mining_Minerals) {
										DebugEngine.drawCircleMap(w.getX(),
												w.getY(), 3, Color.Blue, true);
									} else if (w.getTask() == WorkerTask.Mining_Gas) {
										DebugEngine.drawCircleMap(w.getX(),
												w.getY(), 3, Color.Green, true);
									} else if (w.getTask() == WorkerTask.Constructing_Building) {
										DebugEngine.drawCircleMap(w.getX(),
												w.getY(), 3, Color.Orange, true);
									}
								}
							}
						});
	}
}
