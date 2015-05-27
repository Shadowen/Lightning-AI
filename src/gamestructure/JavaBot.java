package gamestructure;

import java.util.ArrayList;
import java.util.List;
import micromanager.MicroManager;
import pathfinder.PathingManager;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.BuildManager;
import datastructure.Resource;
import botstate.BotState;
import botstate.FirstFrameState;
import bwapi.DefaultBWListener;
import bwapi.Mirror;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class JavaBot extends DefaultBWListener {
	private Mirror mirror = new Mirror();
	private GameHandler game;
	private DebugEngine debugEngine;
	// Only contains my units under construction
	private List<Unit> unitsUnderConstruction;

	private BotState botState;
	private BaseManager baseManager;
	private BuildManager buildManager;
	private MicroManager microManager;
	private PathingManager pathingManager;

	public static void main(String[] args) {
		new JavaBot();
	}

	public JavaBot() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	/**
	 * Method called at the beginning of the game.
	 */
	@Override
	public void onStart() {
		game = new GameHandler(mirror.getGame());
		debugEngine = new DebugEngine(mirror.getGame());
		try {
			game = new GameHandler(mirror.getGame());
			debugEngine = new DebugEngine(mirror.getGame());
			// Use BWTA to analyze map
			// This may take a few minutes if the map is processed first time!
			System.out.println("Analyzing map...");
			BWTA.readMap();
			BWTA.analyze();
			System.out.println("Map data ready");

			// Initialize
			unitsUnderConstruction = new ArrayList<Unit>();

			// Start all the modules
			baseManager = new BaseManager(game, debugEngine);
			buildManager = new BuildManager(game, baseManager, debugEngine);
			pathingManager = new PathingManager(game, baseManager, debugEngine);
			microManager = new MicroManager(game, baseManager, pathingManager,
					debugEngine);
			botState = new FirstFrameState(game, baseManager, buildManager,
					microManager, pathingManager);

			registerDebugFunctions(debugEngine);
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
			// Allow the bot to act
			// Bot state updates
			botState = botState.act();
			// BuildManager check build order
			buildManager.checkMinimums();
			// Micro units
			// microManager.act();

			// Auto economy
			baseManager.gatherResources();
			for (Base b : baseManager.getMyBases()) {
				b.commandCenter.ifPresent(c -> {
					// Train SCVS if necessary
					// TODO This can't go in the build queue since it is
					// specific to
					// a command center!
						if (game.getSelfPlayer().minerals() >= 50
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
			buildManager.buildingQueue
					.stream()
					.filter(b -> game.getSelfPlayer().minerals() >= b.getType()
							.mineralPrice())
					.filter(b -> game.getSelfPlayer().gas() >= b.getType()
							.gasPrice()).forEach(b -> {
						if (b.hasBuilder()) {
							// Has builder already
							if (!b.builder.getUnit().isConstructing()) {
								b.builder.build(b);
							}
						} else {
							// If it isn't being built yet
							baseManager.getBuilder().build(b);
						}
					});
			// Auto train
			buildManager.unitQueue.stream()
					.forEach(
							toTrain -> game
									.getAllUnits()
									.stream()
									.filter(u -> u.getType() == toTrain
											.whatBuilds().first)
									.filter(u -> !u.isTraining()).findFirst()
									.ifPresent(u -> u.train(toTrain)));

			// Draw debug information on screen
			debugEngine.draw();
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
				baseManager.resourceDepotShown(unit);
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
				baseManager.resourceDepotHidden(unit);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitCreate(Unit unit) {
		try {
			if (unit.getPlayer() == game.getSelfPlayer()) {
				unitsUnderConstruction.add(unit);
				microManager.unitCreate(unit);
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
				baseManager.resourceDepotDestroyed(unit);
			}
			// Remove workers from the baseManager
			baseManager.unitDestroyed(unit);
			// Deletes units from microManager
			microManager.unitDestroyed(unit);

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

	public void onUnitConstructed(Unit unit) {
		try {
			UnitType type = unit.getType();
			if (unit.getPlayer().equals(game.getSelfPlayer())) {
				System.out.println("Unit complete: " + type.toString());

				buildManager.buildingComplete(unit);

				if (type == UnitType.Terran_SCV) {
					// Add new workers to nearest base
					baseManager.workerComplete(unit);
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
			String[] command = s.substring(1).split(" ");
			debugEngine.onReceiveCommand(command);
		}
	}

	/**
	 * Register my own debug functions to the debugEngine.
	 * 
	 * @param de
	 *            The debugEngine to be used. Should usually be its own
	 *            debugEngine.
	 */
	private void registerDebugFunctions(DebugEngine de) {
		de.registerDebugModule(new DebugModule("fps") {
			private static final int yBottom = 285;

			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawTextScreen(10, yBottom - 15 * 2,
						"Frame: " + game.getFrameCount());
				engine.drawTextScreen(10, yBottom - 15, "FPS: " + game.getFPS());
				engine.drawTextScreen(10, yBottom, "APM: " + game.getAPM());
			}
		});
		de.registerDebugModule(new DebugModule("botstate") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawTextScreen(5, 5, "Bot state: "
						+ botState.getClass().toString());
			}
		});
		de.registerDebugModule(new DebugModule("construction") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				String uucString = "";
				for (Unit u : unitsUnderConstruction) {
					uucString += u.getType().toString() + ", ";
				}
				engine.drawTextScreen(5, 60, "unitsUnderConstruction: "
						+ uucString);
			}
		});
		de.registerDebugModule(new DebugModule("supply") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawTextScreen(550, 15, "Supply: "
						+ game.getSelfPlayer().supplyUsed() + "/"
						+ game.getSelfPlayer().supplyTotal());
			}
		});
	}

}
