package gamestructure;

import static gamestructure.debug.DebugManager.debugManager;
import static datastructure.BaseManager.baseManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.InvalidCommandException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import micromanager.MicroManager;
import botstate.BotState;
import botstate.FirstFrameState;
import bwapi.BWEventListener;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import datastructure.Base;
import datastructure.Resource;
import static datastructure.BuildManager.buildManager;

public class JavaBot implements BWEventListener {
	public static Mirror mirror = new Mirror();

	private BotState botState;
	// Only contains my units under construction
	private Set<Unit> unitsUnderConstruction;

	public static void main(String[] args) {
		new JavaBot();
	}

	public JavaBot() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	/**
	 * Method called at the beginning of the GameHandler.
	 */
	@Override
	public void onStart() {
		try {
			// Use BWTA to analyze map
			// This may take a few minutes if the map is processed first time!
			System.out.println("Analyzing map...");
			BWTA.readMap();
			BWTA.analyze();
			System.out.println("Map data ready");

			botState = new FirstFrameState();
			// Initialize
			unitsUnderConstruction = new HashSet<Unit>();

			// Start all the modules
			registerDebugFunctions();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method called on every logical frame
	 */
	@Override
	public void onFrame() {
		try {
			// Check if any units have completed

			for (Iterator<Unit> it = unitsUnderConstruction.iterator(); it
					.hasNext();) {
				Unit unit = it.next();
				if (unit.isCompleted()) {
					it.remove();
					onUnitConstructed(unit);
				}
			}
			// Allow the bot to act
			// Bot state updates
			botState = botState.act();
			// BuildManager check build order
			buildManager().checkMinimums();
			// Micro units
			// microManager.act();

			// Auto economy
			baseManager().gatherResources();

			baseManager().getMyBases().stream()
					.filter(b -> b.workers.size() < b.minerals.size() * 2)
					.map(b -> b.commandCenter).filter(o -> o.isPresent())
					.map(o -> o.get()).filter(c -> !c.isTraining())
					.forEach(c -> {
						if (GameHandler.getSelfPlayer().minerals() >= 50)
							c.train(UnitType.Terran_SCV);
					});
			for (Base b : baseManager().getMyBases()) {
				b.commandCenter.ifPresent(c -> {
					// Train SCVS if necessary
					// TODO This can't go in the build queue since it is
					// specific to a command center!
						if (GameHandler.getSelfPlayer().minerals() >= 50
								&& !c.isTraining()) {
							for (Resource mineral : b.minerals) {
								if (mineral.getNumGatherers() < 2) {
									// Do training
									c.train(UnitType.Terran_SCV);
									break;
								}
							}
						}
					});
			}

			// Auto build
			buildManager().buildingQueue
					.stream()
					.filter(b -> GameHandler.getSelfPlayer().minerals() >= b
							.getType().mineralPrice())
					.filter(b -> GameHandler.getSelfPlayer().gas() >= b
							.getType().gasPrice())
					.forEach(b -> {
						if (b.hasBuilder()) {
							// Has builder already
							if (!b.builder.getUnit().isConstructing()) {
								b.builder.build(b);
							}
						} else {
							// If it isn't being built yet
							baseManager().getBuilder().ifPresent(
									w -> w.build(b));
						}
					});
			// Auto train
			buildManager().unitQueue.stream()
					.forEach(
							toTrain -> GameHandler
									.getAllUnits()
									.stream()
									.filter(u -> u.getType() == toTrain
											.whatBuilds().first)
									.filter(u -> !u.isTraining()).findFirst()
									.ifPresent(u -> u.train(toTrain)));

			// Draw debug information on screen
			debugManager().draw();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEnd(boolean winner) {
	}

	public void onNukeDetect(Position p) {
	}

	@Override
	public void onUnitDiscover(Unit unit) {
		try {
			// Update botstate
			botState = botState.unitDiscover(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitEvade(Unit unit) {
	}

	@Override
	public void onUnitShow(Unit unit) {
		try {
			// Base occupation detection
			if (unit.getType().isResourceDepot()) {
				baseManager().resourceDepotShown(unit);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitHide(Unit unit) {
		try {
			// Base occupation detection
			if (unit.getType().isResourceDepot()) {
				baseManager().resourceDepotHidden(unit);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitCreate(Unit unit) {
		try {
			if (unit.getPlayer() == GameHandler.getSelfPlayer()) {
				unitsUnderConstruction.add(unit);
				MicroManager.unitCreate(unit);
			}
			botState = botState.unitCreate(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitDestroy(Unit unit) {
		try {
			// If a unit is canceled from a build queue or building is cancelled
			// under construction
			unitsUnderConstruction.remove(unit);

			// Base occupation detection
			if (unit.getType().isResourceDepot()) {
				baseManager().resourceDepotDestroyed(unit);
			}
			// Remove workers from the BaseManager
			baseManager().unitDestroyed(unit);
			// Deletes units from microManager
			MicroManager.unitDestroyed(unit);

			// Allow the bot state to act
			botState = botState.unitDestroy(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitMorph(Unit unit) {
		onUnitCreate(unit);
	}

	@Override
	public void onUnitRenegade(Unit unit) {

	}

	@Override
	public void onUnitComplete(Unit unit) {
		// GameHandler.sendText("Unit complete: " + unit.getType());
	}

	public void onUnitConstructed(Unit unit) {
		try {
			UnitType type = unit.getType();
			if (unit.getPlayer().equals(GameHandler.getSelfPlayer())) {
				// GameHandler.sendText("Unit constructed: " + type.toString());
				buildManager().buildingComplete(unit);

				if (type == UnitType.Terran_SCV) {
					// Add new workers to nearest base
					baseManager().workerComplete(unit);
				}

				botState = botState.unitComplete(unit);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSendText(String s) {
		if (s.startsWith("/")) {
			List<String> command = new ArrayList<>(Arrays.asList(s.substring(1)
					.split(" ")));
			try {
				debugManager().onReceiveCommand(command);
			} catch (InvalidCommandException e) {
				GameHandler.sendText(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Register my own debug functions to the debugEngine.
	 */
	private void registerDebugFunctions() {
		DebugModule stats = debugManager().createDebugModule("stats");
		final int yBottom = 285;
		stats.addSubmodule("apm").setDraw(
				() -> {
					DrawEngine.drawTextScreen(10, yBottom, "APM: "
							+ GameHandler.getAPM());
					DrawEngine.drawTextScreen(10, yBottom - 15, "APMs: "
							+ GameHandler.getAPM(true));
				});
		stats.addSubmodule("fps").setDraw(
				() -> {
					DrawEngine.drawTextScreen(10, yBottom - 15 * 4, "Frame: "
							+ GameHandler.getFrameCount());
					DrawEngine.drawTextScreen(10, yBottom - 15 * 3, "aFPS: "
							+ GameHandler.getAverageFPS());
					DrawEngine.drawTextScreen(10, yBottom - 15 * 2, "FPS: "
							+ GameHandler.getFPS());
				});
		debugManager().createDebugModule("botstate").setDraw(
				() -> {
					DrawEngine.drawTextScreen(5, 5, "Bot state: "
							+ botState.getClass().toString());
				});
		debugManager().createDebugModule("construction").setDraw(
				() -> {
					String uucString = "";
					for (Unit u : unitsUnderConstruction) {
						uucString += u.getType().toString() + ", ";
					}
					DrawEngine.drawTextScreen(5, 60, "unitsUnderConstruction: "
							+ uucString);
				});
		debugManager().createDebugModule("supply").setDraw(
				() -> {
					DrawEngine.drawTextScreen(550, 15, "Supply: "
							+ GameHandler.getSelfPlayer().supplyUsed() + "/"
							+ GameHandler.getSelfPlayer().supplyTotal());
				});
	}

	@Override
	public void onReceiveText(Player player, String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPlayerLeft(Player player) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSaveGame(String gameName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPlayerDropped(Player player) {
		// TODO Auto-generated method stub

	}
}
