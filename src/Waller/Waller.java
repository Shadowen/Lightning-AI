package Waller;

import java.util.Queue;

import bwapi.Color;
import bwapi.Position;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.Chokepoint;
import datastructure.BaseManager;
import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;
import pathfinder.NoPathFoundException;
import pathfinder.PathingManager;

public final class Waller {
	private static Queue<Position> pathToNat;
	private static Queue<Position> pathToEnemy;
	private static Chokepoint firstChoke;

	public static void init() {
		System.out.print("Starting Waller... ");
		try {
			findPaths();
			firstChoke = findFirstChokeAlongPath(pathToEnemy);
		} catch (Exception e) {
			System.err.println("Waller error");
		}

		registerDebugFunctions();
		System.out.println("Success!");
	}

	/** This constructor should never be used. */
	private Waller() {
	}

	private static void findPaths() throws Exception {
		pathToNat = PathingManager.findGroundPath(BaseManager.main.getLocation().getPoint(),
				BaseManager.natural.getLocation().getPoint(), UnitType.Zerg_Zergling);
		pathToEnemy = PathingManager.findGroundPath(BaseManager.main.getLocation().getPoint(),
				BaseManager.getBases().stream().filter(b -> b.getPlayer() == GameHandler.getEnemyPlayer()).findAny()
						.get().getLocation().getPoint(),
				UnitType.Zerg_Zergling);
	}

	private static Chokepoint findFirstChokeAlongPath(Queue<Position> path) {
		for (Position w : path) {
			for (Chokepoint choke : BWTA.getChokepoints()) {
				if (w.getDistance(choke.getPoint()) < choke.getWidth()) {
					return choke;
				}
			}
		}
		return null;
	}

	private static void registerDebugFunctions() {
		// DebugModule chokeDM = DebugManager.createDebugModule("choke");
		// // Label all chokes
		// chokeDM.addSubmodule("draw").setDraw(() -> {
		// int i = 0;
		// for (Chokepoint choke : BWTA.getChokepoints()) {
		// DrawEngine.drawCircleMap(choke.getCenter().getX(),
		// choke.getCenter().getY(), (int) choke.getWidth(),
		// Color.Yellow, false);
		// DrawEngine.drawTextMap(choke.getCenter().getX() - 10,
		// choke.getCenter().getY() - 20, "Choke " + i);
		// DrawEngine.drawTextMap(choke.getCenter().getX() - 10,
		// choke.getCenter().getY() - 10,
		// "Width: " + choke.getWidth());
		// i++;
		// }
		// if (firstChoke != null) {
		// DrawEngine.drawTextMap(firstChoke.getCenter().getX() - 10,
		// firstChoke.getCenter().getY() + 20,
		// "Wall me!");
		// }
		// });
		//
		// DebugModule mainPath =
		// DebugManager.createDebugModule("mainpath").setDraw(() -> {
		// DrawEngine.drawPath(pathToNat);
		// DrawEngine.drawPath(pathToEnemy);
		// });
	}

}
