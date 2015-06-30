package gamestructure;

import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.InvalidCommandException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pathfinder.PathingManager;
import micromanager.MicroManager;
import botstate.BotState;
import botstate.FirstFrameState;
import bwapi.BWEventListener;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.Utils;
import bwta.BWTA;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.BuildManager;
import datastructure.Resource;

public class JavaBot implements BWEventListener {
	private Mirror mirror = new Mirror();

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

			GameHandler.init(mirror.getGame());
			DebugManager.init();
			DrawEngine.init(mirror.getGame());
			BaseManager.init();
			BuildManager.init();
			MicroManager.init();
			PathingManager.init();
			botState = new FirstFrameState();
			// Initialize
			unitsUnderConstruction = new HashSet<Unit>();

			// Start all the modules
			registerDebugFunctions();

			System.out.println("Init complete!");
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
			unitsUnderConstruction.removeIf(unit -> {
				if (unit.isCompleted()) {
					onUnitConstructed(unit);
					return true;
				}
				return false;
			});

			BaseManager.onFrame();
			// Allow the bot to act
			// Bot state updates
			botState = botState.act();
			// BuildManager check build order
			BuildManager.checkMinimums();
			// Micro units
			MicroManager.act();

			// Auto economy
			BaseManager.gatherResources();

			BaseManager.getMyBases().stream()
					.filter(b -> b.workers.size() < b.minerals.size() * 2)
					.map(b -> b.commandCenter).filter(o -> o.isPresent())
					.map(o -> o.get()).filter(c -> !c.isTraining())
					.forEach(c -> {
						if (GameHandler.getSelfPlayer().minerals() >= 50)
							c.train(UnitType.Terran_SCV);
					});
			// OLD Implementation
			for (Base b : BaseManager.getMyBases()) {
				b.commandCenter.ifPresent(c -> {
					// Train SCVS if necessary
					// TODO This can't go in the build queue since it is
					// specific to
					// a command center!
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
			BuildManager.buildingQueue
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
							BaseManager.getBuilder().ifPresent(w -> w.build(b));
						}
					});
			// Auto train
			BuildManager.unitQueue.stream()
					.forEach(
							toTrain -> GameHandler
									.getMyUnits()
									.stream()
									.filter(u -> u.getType() == toTrain
											.whatBuilds().first)
									.filter(u -> !u.isTraining()).findFirst()
									.ifPresent(u -> u.train(toTrain)));

			// Draw debug information on screen
			DebugManager.draw();
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
				BaseManager.unitShown(unit);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitHide(Unit unit) {
		try {
			BaseManager.unitHidden(unit);
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

			// Remove workers from the BaseManager
			BaseManager.unitDestroyed(unit);
			// Deletes units from microManager
			MicroManager.unitDestroyed(unit);

			// Allow the bot state to act
			botState = botState.unitDestroyed(unit);
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
		GameHandler.sendText("Unit complete: " + unit.getType());
	}

	public void onUnitConstructed(Unit unit) {
		try {
			UnitType type = unit.getType();
			if (unit.getPlayer().equals(GameHandler.getSelfPlayer())) {
				GameHandler.sendText("Unit constructed: " + type.toString());

				BuildManager.buildingComplete(unit);
			}
			BaseManager.onUnitConstructed(unit);
			botState = botState.unitComplete(unit);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSendText(String s) {
		try {
			if (s.startsWith("/")) {
				List<String> command = new ArrayList<>(Arrays.asList(s
						.substring(1).split(" ")));
				try {
					DebugManager.onReceiveCommand(command);
				} catch (InvalidCommandException e) {
					GameHandler.sendText(e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Register my own debug functions to the debugEngine.
	 */
	private void registerDebugFunctions() {
		DebugModule stats = DebugManager.createDebugModule("stats");
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
		DebugManager.createDebugModule("botstate").setDraw(
				() -> {
					DrawEngine.drawTextScreen(5, 5, "Bot state: "
							+ botState.getClass().toString());
				});
		DebugManager.createDebugModule("construction").setDraw(
				() -> {
					String uucString = "";
					for (Unit u : unitsUnderConstruction) {
						uucString += u.getType().toString() + ", ";
					}
					DrawEngine.drawTextScreen(5, 60, "unitsUnderConstruction: "
							+ uucString);
				});
		DebugManager.createDebugModule("supply").setDraw(
				() -> {
					DrawEngine.drawTextScreen(550, 15, "Supply: "
							+ GameHandler.getSelfPlayer().supplyUsed() + "/"
							+ GameHandler.getSelfPlayer().supplyTotal());
				});
	}

	@Override
	public void onReceiveText(Player player, String text) {
	}

	@Override
	public void onPlayerLeft(Player player) {
	}

	@Override
	public void onSaveGame(String gameName) {
	}

	@Override
	public void onPlayerDropped(Player player) {
	}
}
