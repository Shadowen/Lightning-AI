package gamestructure;

import java.util.HashSet;
import java.util.Set;

import micromanager.MicroManager;
import botstate.BotState;
import botstate.FirstFrameState;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.BuildManager;
import datastructure.Resource;

public final class JavaBot extends DefaultBWListener {
	private static Mirror mirror = new Mirror();
	protected static Game game = mirror.getGame();
	private static BotState botState;

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
	 * Method called at the beginning of the game.
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

			// Initialize
			unitsUnderConstruction = new HashSet<Unit>();

			// Start all the modules
			botState = new FirstFrameState();

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
			BuildManager.checkMinimums();
			// Micro units
			// MicroManager.act();

			// Auto economy
			BaseManager.gatherResources();
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
									.getAllUnits()
									.stream()
									.filter(u -> u.getType() == toTrain
											.whatBuilds().first)
									.filter(u -> !u.isTraining()).findFirst()
									.ifPresent(u -> u.train(toTrain)));

			// Draw debug information on screen
			DebugEngine.draw();
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
				BaseManager.resourceDepotShown(unit);
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
				BaseManager.resourceDepotHidden(unit);
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
				BaseManager.resourceDepotDestroyed(unit);
			}
			// Remove workers from the BaseManager
			BaseManager.unitDestroyed(unit);
			// Deletes units from MicroManager
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

	public void onUnitConstructed(Unit unit) {
		try {
			UnitType type = unit.getType();
			if (unit.getPlayer().equals(GameHandler.getSelfPlayer())) {
				System.out.println("Unit complete: " + type.toString());

				BuildManager.buildingComplete(unit);

				if (type == UnitType.Terran_SCV) {
					// Add new workers to nearest base
					BaseManager.workerComplete(unit);
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
			DebugEngine.onReceiveCommand(command);
		}
	}

	/**
	 * Register my own debug functions to the DebugEngine.
	 * 
	 * @param de
	 *            The DebugEngine to be used. Should usually be its own
	 *            DebugEngine.
	 */
	private void registerDebugFunctions() {
		DebugEngine.registerDebugModule(new DebugModule("fps") {
			private static final int yBottom = 285;

			@Override
			public void draw() throws ShapeOverflowException {
				DebugEngine.drawTextScreen(10, yBottom - 15 * 2, "Frame: "
						+ GameHandler.getFrameCount());
				DebugEngine.drawTextScreen(10, yBottom - 15, "FPS: "
						+ GameHandler.getFPS());
				DebugEngine.drawTextScreen(10, yBottom,
						"APM: " + GameHandler.getAPM());
			}
		});
		DebugEngine.registerDebugModule(new DebugModule("botstate") {
			@Override
			public void draw() throws ShapeOverflowException {
				DebugEngine.drawTextScreen(5, 5, "Bot state: "
						+ botState.getClass().toString());
			}
		});
		DebugEngine.registerDebugModule(new DebugModule("construction") {
			@Override
			public void draw() throws ShapeOverflowException {
				String uucString = "";
				for (Unit u : unitsUnderConstruction) {
					uucString += u.getType().toString() + ", ";
				}
				DebugEngine.drawTextScreen(5, 60, "unitsUnderConstruction: "
						+ uucString);
			}
		});
		DebugEngine.registerDebugModule(new DebugModule("supply") {
			@Override
			public void draw() throws ShapeOverflowException {
				DebugEngine.drawTextScreen(550, 15, "Supply: "
						+ GameHandler.getSelfPlayer().supplyUsed() + "/"
						+ GameHandler.getSelfPlayer().supplyTotal());
			}
		});
	}

}
