package micromanager;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.sun.javafx.geom.Shape;

import bwapi.Color;
import bwapi.Game;
import bwapi.Position;
import bwapi.PositionOrUnit;
import bwapi.Unit;
import bwapi.UnitType;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.Worker;
import datastructure.WorkerTask;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;
import pathfinder.NoPathFoundException;
import pathfinder.PathingManager;

public final class MicroManager {
	/** The width of the map in build tiles */
	private static int mapWidth;
	/** The height of the map in build tiles */
	private static int mapHeight;
	private static double[][] targetMap;
	private static double[][] threatMap;
	private static Point[][] movementMap;

	private static Optional<Position> scoutingTarget;
	private static Optional<UnitAgent> scoutingUnit;

	public static Map<UnitType, HashSet<UnitAgent>> unitsByType;
	private static Map<Unit, UnitAgent> unitAgents;

	public static void init() {
		System.out.print("Starting MicroManager... ");
		mapWidth = GameHandler.getMapWidth();
		mapHeight = GameHandler.getMapHeight();
		targetMap = new double[mapWidth + 1][mapHeight + 1];
		threatMap = new double[mapWidth + 1][mapHeight + 1];
		movementMap = new Point[mapWidth + 1][mapHeight + 1];
		for (int x = 0; x < movementMap.length; x++) {
			for (int y = 0; y < movementMap[x].length; y++) {
				movementMap[x][y] = new Point(0, 0);
			}
		}

		unitsByType = new HashMap<UnitType, HashSet<UnitAgent>>();
		unitsByType.put(UnitType.Terran_Wraith, new HashSet<UnitAgent>());
		unitsByType.put(UnitType.Terran_Barracks, new HashSet<UnitAgent>());
		unitsByType.put(UnitType.Terran_Factory, new HashSet<UnitAgent>());
		unitsByType.put(UnitType.Terran_Vulture, new HashSet<UnitAgent>());
		unitAgents = new HashMap<Unit, UnitAgent>();

		scoutingTarget = Optional.empty();
		scoutingUnit = Optional.empty();

		registerDebugFunctions();
		System.out.println("Wraith range: " + UnitType.Terran_Wraith.groundWeapon().maxRange());
		System.out.println("Wraith size: " + UnitType.Terran_Wraith.width() + ", " + UnitType.Terran_Wraith.height());
		System.out.println("Marine range: " + UnitType.Terran_Marine.airWeapon().maxRange());
		System.out.println("Marine: " + UnitType.Terran_Marine.width() + ", " + UnitType.Terran_Marine.height());
		System.out.println("Success!");
	}

	/** This constructor should never be called. */
	private MicroManager() {
	}

	public static void act() {
		updateMap();
		// Move attacking units
		micro();
		// Move scouting unit(s)
		if (!scoutingTarget.isPresent()
				|| GameHandler.isVisible(scoutingTarget.get().getX() / 32, scoutingTarget.get().getY() / 32)) {
			// GameHandler.sendText("Looking for new scouting target");
			// scoutingTarget = getScoutingTarget(); TODO
		}
		if (scoutingUnit.isPresent() && scoutingTarget.isPresent()) {
			// Acquire a target if necessary
			scout(scoutingUnit.get(), scoutingTarget.get());
		}
	}

	private static void updateMap() {
		// Update threat map
		// Reset target and threat counter
		for (int x = 0; x < mapWidth; x++) {
			for (int y = 0; y < mapHeight; y++) {
				targetMap[x][y] = 0;
				threatMap[x][y] = 0;
			}
		}

		// Loop through enemy units
		for (Unit u : GameHandler.getEnemyUnits()) {
			UnitType unitType = u.getType();
			// Get the x and y grid point coordinates
			int x = u.getX() / 32;
			int y = u.getY() / 32;

			// Update target map
			double targetValue = 0;
			if (unitType.isWorker()) {
				targetValue = 1;
			}
			/** Attack range in build tiles */
			int radius = 10;
			int startX = Math.max(x - radius, 0);
			int endX = Math.min(x + radius, mapWidth);
			for (int cx = startX; cx < endX; cx++) {
				int remainingRadius = radius - Math.abs(cx - x);
				int startY = Math.max(y - remainingRadius, 0);
				int endY = Math.min(y + remainingRadius, mapHeight);
				for (int cy = startY; cy < endY; cy++) {
					targetMap[cx][cy] += targetValue / (Point.distance(x, y, cx, cy) + 1);
				}
			}

			// Update threat map
			double threatValue = 20;
			radius = u.getType().airWeapon().maxRange() / 32 + 10;
			startX = Math.max(x - radius, 0);
			endX = Math.min(x + radius, mapWidth);
			for (int cx = startX; cx < endX; cx++) {
				int remainingRadius = radius - Math.abs(cx - x);
				int startY = Math.max(y - remainingRadius, 0);
				int endY = Math.min(y + remainingRadius, mapHeight);
				for (int cy = startY; cy < endY; cy++) {
					threatMap[cx][cy] += threatValue * Math.max(1 - Point.distance(x, y, cx, cy) / radius, 0);
				}
			}
		}

		movementMap = new Point[mapWidth + 1][mapHeight + 1];
		for (int x = 1; x < mapWidth - 1; x++) {
			for (int y = 1; y < mapHeight - 1; y++) {
				movementMap[x][y] = new Point((int) (threatMap[x + 1][y] - threatMap[x - 1][y]),
						(int) (threatMap[x][y + 1] - threatMap[x][y - 1]));
			}
		}
	}

	private static void micro() {
		for (UnitAgent ua : unitsByType.get(UnitType.Terran_Wraith)) {
			Unit u = ua.unit;
			int x = (int) (u.getX() + u.getVelocityX());
			int y = (int) (u.getY() + u.getVelocityY());
			Position predictedPosition = new Position(x, y);

			switch (ua.task) {
			case IDLE:
				ua.task = UnitTask.SCOUTING;
				break;
			case SCOUTING:
				// Scout the base...
				if (scoutingTarget.isPresent()) {
					scout(ua, scoutingTarget.get());
				}
				// Go aggro when threshold is reached
				if (unitsByType.get(UnitType.Terran_Wraith).size() > 0) {
					ua.task = UnitTask.ATTACK_RUN;
				}
				break;
			case ATTACK_RUN:
				GameHandler.getEnemyUnits().stream()
						.sorted((u1,
								u2) -> (int) (u1.getPosition().getDistance(u.getPosition())
										- u2.getPosition().getDistance(u.getPosition())) * 1000)
						.findFirst().ifPresent(e -> {
							ua.target = e;
							u.move(e.getPosition());
							if (predictedPosition.getDistance(e.getPosition()) <= 205) {
								System.out.println(GameHandler.getFrameCount() + ":" + "Firing initiated: "
										+ predictedPosition.getDistance(ua.target.getPosition()));
								ua.task = UnitTask.FIRING;
							}
						});
				break;
			case FIRING:
				// System.out.println("Firing @ " +
				// GameHandler.getFrameCount());
				u.attack(new PositionOrUnit(ua.target));
				System.out.println(GameHandler.getFrameCount() + ":" + "Firing at: "
						+ predictedPosition.getDistance(ua.target.getPosition()));
				ua.task = UnitTask.RETREATING;
				ua.timeout = 3;
				break;
			case RETREATING:
				// System.out.println("Retreating @ " +
				// GameHandler.getFrameCount());
				// ua.path = PathingManager.findSafeAirPath(u.getX(), u.getY(),
				// threatMap, 20);
				// ua.followPath();
				// System.out.println(GameHandler.getFrameCount() + ":" +
				// "Retreat begun at "
				// + u.getPosition().getDistance(ua.target.getPosition()));
				int dx = 10 * (ua.unit.getX() - ua.target.getX());
				int dy = 10 * (ua.unit.getY() - ua.target.getY()) + 500;
				ua.unit.move(new Position(ua.unit.getX() + dx, ua.unit.getY() + dy));
				ua.timeout--;
				// Go safe when threshold is reached
				if (ua.timeout <= 0 && u.getGroundWeaponCooldown() <= 0) {
					ua.task = UnitTask.ATTACK_RUN;
				}
				break;
			default:
				break;
			}
		}
	}

	private static void scout(UnitAgent ua, Position target) {
		ua.task = UnitTask.SCOUTING;
		try {
			if (ua.path.size() < 15) {
				// Path planned is short
				ua.path = PathingManager.findGroundPath(ua.unit.getX(), ua.unit.getY(), target.getX(), target.getY(),
						ua.unit.getType(), 256);
				if (ua.path.size() == 0) {
					System.out.println("Pathfinder failed to find a path...");
				}
			}

			ua.followPath();
		} catch (NoPathFoundException e) {
			GameHandler.sendText("Failed to find a ground path!");
		}
	}

	private static Optional<Position> getScoutingTarget() {
		Base target = null;
		for (Base b : BaseManager.getBases()) {
			if (b.getLocation().isStartLocation() && b.getPlayer() == GameHandler.getNeutralPlayer()
					&& (target == null || b.getLastScouted() < target.getLastScouted())) {
				target = b;
			}
		}
		if (target == null) {
			for (Base b : BaseManager.getBases()) {
				if (target == null || b.getLastScouted() < target.getLastScouted()) {
					target = b;
				}
			}
		}
		return Optional.ofNullable(target).map(t -> t.getLocation().getPosition());
	}

	public static void setScoutingUnit(Unit unit) {
		scoutingUnit = Optional.of(unitAgents.get(unit));

		Optional<Worker> ow = BaseManager.getWorker(unit);
		ow.ifPresent(w -> w.setTask(WorkerTask.SCOUTING, null));
	}

	public static boolean isScouting() {
		return scoutingUnit.isPresent();
	}

	public static void unitConstructed(Unit unit) {
		UnitType type = unit.getType();
		UnitAgent ua = new UnitAgent(unit);
		unitsByType.putIfAbsent(type, new HashSet<UnitAgent>());
		unitsByType.get(type).add(ua);
		unitAgents.put(unit, ua);
	}

	public static void unitDestroyed(Unit unit) {
		UnitAgent ua = unitAgents.get(unit);
		// unitsByType.get(unit.getType()).remove(ua);
		// if (scoutingUnit.filter(su -> su.equals(unit)).isPresent()) {
		// scoutingUnit = Optional.empty();
		// }
	}

	public static void registerDebugFunctions() {
		DebugManager.createDebugModule("tasks").setDraw(() -> {
			for (UnitAgent ua : unitAgents.values()) {
				DrawEngine.drawTextMap(ua.unit.getX(), ua.unit.getY(), ua.task);
			}
		});
		// Enemy units
		DebugManager.createDebugModule("enemies").setDraw(() -> {
			for (Unit e : GameHandler.getEnemyUnits()) {
				DrawEngine.drawCircleMap(e.getX(), e.getY(), 3, Color.Red, true);
			}
		});
		// // Threat map
		// DebugManager.createDebugModule("threats").setDraw(() -> {
		// // Actually draw
		// final double scalingFactor = 1;
		// for (int x = 0; x < mapWidth; x++) {
		// for (int y = 0; y < mapHeight; y++) {
		// DrawEngine.drawBoxMap(x * 32 + 16, y * 32 + 16,
		// x * 32 + 16 + (int) (threatMap[x][y] * scalingFactor),
		// y * 32 + 16 + (int) (threatMap[x][y] * scalingFactor), Color.Red,
		// true);
		// }
		// }
		// });
		// Wraith firing range
		DebugManager.createDebugModule("wraithrange").setDraw(() -> {
			for (UnitAgent a : unitsByType.get(UnitType.Terran_Wraith)) {
				DrawEngine.drawCircleMap(a.unit.getX(), a.unit.getY(), 180, Color.Orange, false);
				if (a.target != null) {
					DrawEngine.drawLineMap(a.unit.getX(), a.unit.getY(), a.target.getX(), a.target.getY(), Color.Red);
				}
			}
		});
		// // Movement map
		// DebugManager.createDebugModule("movement").setDraw(
		// () -> {
		// for (int x = 1; x < mapWidth; x++) {
		// for (int y = 1; y < mapHeight; y++) {
		// DrawEngine.drawArrowMap(x, y, x
		// + movementMap[x][y].x, y
		// + movementMap[x][y].y, Color.Green);
		// }
		// }
		// });
		// Target map
		// DebugManager.createDebugModule("targets")
		// .setDraw(
		// () -> {
		// for (int tx = 1; tx < mapWidth - 1; tx++) {
		// int x = tx * 32;
		// for (int ty = 1; ty < mapHeight - 1; ty++) {
		// int y = ty * 32;
		//
		// int north = (int) Math
		// .round(targetMap[tx][ty - 1]);
		// int east = (int) Math
		// .round(targetMap[tx + 1][ty]);
		// int south = (int) Math
		// .round(targetMap[tx][ty - 1]);
		// int west = (int) Math
		// .round(targetMap[tx - 1][ty]);
		// DrawEngine.drawArrowMap(x, y, x
		// + (east - west) * 5, y
		// + (north - south) * 5, Color.Teal);
		// }
		// }
		// });
		// Weapon cooldown bars
		DebugManager.createDebugModule("cooldowns").setDraw(() -> {
			for (UnitAgent ua : unitAgents.values()) {
				Unit u = ua.unit;
				UnitType unitType = u.getType();
				int cooldownBarSize = 20;
				int cooldownRemaining = u.getGroundWeaponCooldown();
				int maxCooldown = unitType.groundWeapon().damageCooldown();
				if (maxCooldown > 0) {
					DrawEngine.drawLineMap(u.getX(), u.getY(), u.getX() + cooldownBarSize, u.getY(), Color.Green);
					DrawEngine.drawLineMap(u.getX(), u.getY(),
							u.getX() + cooldownRemaining * cooldownBarSize / maxCooldown, u.getY(), Color.Red);
				}
			}
		});
		// Scouting Target
		DebugManager.createDebugModule("scouting").setDraw(() -> {
			if (scoutingTarget.isPresent()) {
				int x = scoutingTarget.get().getX();
				int y = scoutingTarget.get().getY();
				DrawEngine.drawLineMap(x - 10, y - 10, x + 10, y + 10, Color.Red);
				DrawEngine.drawLineMap(x + 10, y - 10, x - 10, y + 10, Color.Red);
			}
		});
		// Pathing
		DebugManager.createDebugModule("pathing").setDraw(() -> {
			for (UnitAgent ua : unitAgents.values()) {
				final Iterator<Position> it = ua.path.iterator();
				Position previous = it.hasNext() ? it.next() : null;
				while (it.hasNext()) {
					final Position current = it.next();
					DrawEngine.drawArrowMap(previous.getX(), previous.getY(), current.getX(), current.getY(),
							Color.Yellow);
					previous = current;
				}
			}
		});
	}
}
