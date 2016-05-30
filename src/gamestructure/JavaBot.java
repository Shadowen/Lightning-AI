package gamestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import base.Base;
import base.BaseManager;
import base.Resource;
import base.Worker;
import build.BuildManager;
import bwapi.BWEventListener;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DebugModule;
import gamestructure.debug.DrawEngine;
import gamestructure.debug.InvalidCommandException;
import memory.MemoryManager;
import micro.MicroManager;
import pathing.NoPathFoundException;
import pathing.PathFinder;
import state.BotState;
import state.FirstFrameState;
import walling.Waller;

public class JavaBot implements BWEventListener {
	private Mirror mirror = new Mirror();

	private BotState botState;

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
			PathFinder.init();
			MemoryManager.init();
			botState = new FirstFrameState();
			Waller.init();

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
			MemoryManager.onFrame();
			// Check if any units have completed
			BuildManager.unitsUnderConstruction.removeIf(unit -> {
				if (unit.isCompleted()) {
					onUnitConstructed(unit);
					return true;
				}
				return false;
			});

			// Allow the bot to act
			// Bot state updates
			botState = botState.onFrame();
			// BuildManager check build order
			BuildManager.checkMinimums();
			// Micro units
			MicroManager.onFrame();

			// Auto economy
			BaseManager.onFrame();

			// OLD Implementation
			for (Base b : BaseManager.getMyBases()) {
				b.commandCenter.ifPresent(c -> {
					// Train SCVS if necessary
					// TODO This can't go in the build queue since it is
					// specific to
					// a command center!
					if (GameHandler.getSelfPlayer().minerals() >= 50 && !c.isTraining()) {
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
			// TODO move this into Worker?
			BuildManager.buildingQueue.stream()
					.filter(b -> GameHandler.getSelfPlayer().minerals() >= b.getType().mineralPrice())
					.filter(b -> GameHandler.getSelfPlayer().gas() >= b.getType().gasPrice()).forEach(b -> {
						if (!b.hasBuilder()) {
							// If it isn't being built yet
							Worker w = BaseManager.getFreeWorker().orElse(null);
							if (w != null) {
								w.build(b);
							}
						}
						if (b.hasBuilder()) {
							// Has builder already
							if (!b.builder.unit.isConstructing()) {
								// Not already building it
								Position p = b.builder.unit.getPosition();
								if (p.getDistance((int) b.getBoundingBox().getCenterX(),
										(int) b.getBoundingBox().getCenterY()) < 4 * 32) {
									// Build it
									b.builder.unit.build(b.getType(), b.getTilePosition());
								} else {
									// Move closer
									try {
										b.builder.findPath(b.getBoundingBox(), 128);
										b.builder.followPath();
									} catch (NoPathFoundException e) {
										System.err.println(
												"Worker failed to find a path to build a " + b.getType().toString());
									}
								}
							}
						}
					});
			// Auto train
			Iterator<UnitType> it = BuildManager.unitQueue.iterator();
			for (UnitType toTrain; it.hasNext();) {
				toTrain = it.next();
				UnitType whatBuilds = toTrain.whatBuilds().first;
				for (Unit u : GameHandler.getMyUnits()) {
					// TODO find better way of comparing
					if (u.getType().toString().equals(whatBuilds.toString())) {
						if (GameHandler.getSelfPlayer().minerals() >= toTrain.mineralPrice()
								&& GameHandler.getSelfPlayer().gas() >= toTrain.gasPrice()
								&& GameHandler.getSelfPlayer().supplyTotal()
										- GameHandler.getSelfPlayer().supplyUsed() >= toTrain.supplyRequired()) {
							if (!u.isTraining() && u.isCompleted()) {
								u.train(toTrain);
								it.remove();
								break;
							}
						}
					}
				}
			}
			// Draw debug information on screen
			DebugManager.draw();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitDiscover(Unit unit) {
		try {
			// Update botstate
			botState = botState.onUnitDiscover(unit);
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
			BaseManager.onUnitShow(unit);
			MemoryManager.onUnitShow(unit);
			botState.unitShown(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitHide(Unit unit) {
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when the unit enters the game (under construction, training queue,
	 * etc).
	 */
	@Override
	public void onUnitCreate(Unit unit) {
		if (unit.getPlayer() == GameHandler.getSelfPlayer()) {
			BuildManager.unitsUnderConstruction.add(unit);
			BuildManager.unitQueue.remove(unit.getType());
		}
		BaseManager.unitCreated(unit);
		if (unit.getType().isBuilding()) {
			PathFinder.onBuildingCreate(unit);
		}
	}

	@Override
	public void onUnitDestroy(Unit unit) {
		try {
			// If a unit is canceled from a build queue or building is
			// cancelled
			// under construction
			BuildManager.unitsUnderConstruction.remove(unit);

			// Remove workers from the BaseManager
			BaseManager.unitDestroyed(unit);
			// Deletes units from microManager
			MicroManager.unitDestroyed(unit);

			MemoryManager.onUnitDestroy(unit);

			// Allow the bot state to act
			botState = botState.unitDestroyed(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when one unit morphs into another. Refineries being started
	 * counts!
	 * 
	 * @param unit
	 *            The type of the unit is the type AFTER the morph has started
	 */
	@Override
	public void onUnitMorph(Unit unit) {
		UnitType type = unit.getType();
		if (type.isRefinery()) {
			onUnitCreate(unit);
		}
		MemoryManager.onUnitMorph(unit);
	}

	@Override
	public void onUnitRenegade(Unit unit) {

	}

	@Deprecated
	@Override
	public void onUnitComplete(Unit unit) {
		GameHandler.sendText(unit.getType().toString() + " complete");
	}

	/**
	 * Called when the unit finishes training or construction.
	 * 
	 * @param unit
	 */
	public void onUnitConstructed(Unit unit) {
		try {
			if (unit.getPlayer().equals(GameHandler.getSelfPlayer())) {
				BuildManager.unitConstructed(unit);
				MicroManager.unitConstructed(unit);
			}
			BaseManager.unitConstructed(unit);
			botState = botState.unitConstructed(unit);
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
	public void onSendText(String s) {
		try {
			if (s.startsWith("/")) {
				List<String> command = new ArrayList<>(Arrays.asList(s.substring(1).split(" ")));
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
		stats.addSubmodule("apm").setDraw(() -> {
			DrawEngine.drawTextScreen(10, yBottom, "APM: " + GameHandler.getAPM());
			DrawEngine.drawTextScreen(10, yBottom - 15, "APMs: " + GameHandler.getAPM(true));
		});
		stats.addSubmodule("fps").setDraw(() -> {
			DrawEngine.drawTextScreen(10, yBottom - 15 * 4, "Frame: " + GameHandler.getFrameCount());
			DrawEngine.drawTextScreen(10, yBottom - 15 * 3, "aFPS: " + GameHandler.getAverageFPS());
			DrawEngine.drawTextScreen(10, yBottom - 15 * 2, "FPS: " + GameHandler.getFPS());
		});
		DebugManager.createDebugModule("botstate").setDraw(() -> {
			DrawEngine.drawTextScreen(5, 5, "Bot state: " + botState.getClass().toString());
		});
		DebugManager.createDebugModule("construction").setDraw(() -> {
			String uucString = "";
			for (Unit u : BuildManager.unitsUnderConstruction) {
				uucString += u.getType().toString() + ", ";
			}
			DrawEngine.drawTextScreen(5, 60, "unitsUnderConstruction: " + uucString);
		});
		DebugManager.createDebugModule("supply").setDraw(() -> {
			DrawEngine.drawTextScreen(550, 15, "Supply: " + GameHandler.getSelfPlayer().supplyUsed() + "/"
					+ GameHandler.getSelfPlayer().supplyTotal());
		});

		DebugManager.createDebugModule("orders").setDraw(() -> {
			for (Unit u : GameHandler.getMyUnits()) {
				if (u.getOrderTargetPosition().getX() != 0 && u.getOrderTargetPosition().getY() != 0) {
					DrawEngine.drawLineMap(u.getPosition().getX(), u.getPosition().getY(),
							u.getOrderTargetPosition().getX(), u.getOrderTargetPosition().getY(), bwapi.Color.Black);
				}
			}
		}).setActive(true);
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
