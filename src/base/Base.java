package base;

import gamestructure.GameHandler;
import micro.UnitTask;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import build.BuildManager;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BaseLocation;

public class Base {
	public List<MineralResource> minerals;
	public List<GasResource> gas;
	public List<Worker> workers;
	public Optional<Unit> commandCenter;
	private BaseLocation location;

	private Player player;
	// When the base was last scouted, in game frames
	private int lastScouted;

	public Base(BaseLocation l) {
		workers = new ArrayList<Worker>();
		location = l;

		minerals = new ArrayList<MineralResource>();
		gas = new ArrayList<GasResource>();

		commandCenter = Optional.empty();

		setPlayer(GameHandler.getNeutralPlayer());
		lastScouted = 0;
	}

	public int getX() {
		return location.getX();
	}

	public int getY() {
		return location.getY();
	}

	public int getMineralCount() {
		return minerals.size();
	}

	public void gatherResources() {
		// Idle workers
		for (Worker worker : workers) {
			UnitTask currentTask = worker.getTask();
			if (worker.isIdle()) {
				if (currentTask == UnitTask.IDLE) {
					worker.setTask(UnitTask.MINERALS);
				}
				if (currentTask == UnitTask.MINERALS || currentTask == UnitTask.GAS) {
					// Get back to work
					if (worker.getCurrentResource() != null) {
						Resource r = worker.getCurrentResource();
						worker.gather(r);
						continue;
					}

					// Try to assign one worker to each mineral first
					Resource mineral = null;
					double distance = 0;

					// This variable is the loop counter
					// It only allows maxMiners to gather each resource patch
					// each
					// loop.
					int maxMiners = 1;
					boolean workerAssigned = false;
					while (!workerAssigned && maxMiners <= 2) {
						for (MineralResource m : minerals) {
							if (m.getNumGatherers() < maxMiners) {
								// Find closest mineral patch
								double newDistance = Point.distance(worker.unit.getX(), worker.unit.getY(), m.getX(),
										m.getY());
								if (mineral == null || newDistance < distance) {
									mineral = m;
									distance = newDistance;
									worker.gather(mineral);
									workerAssigned = true;
								}
							}
						}

						maxMiners++;
					}

					// Worker could not be assigned a patch as the base is
					// supersaturated
					if (!workerAssigned) {
						GameHandler.sendText("Warning: Base is supersaturated!");
					}
				}
			}
		}
	}

	public Worker getFreeWorker() {
		for (Worker w : workers) {
			if (w.getTask() == UnitTask.IDLE) {
				return w;
			}
		}
		for (Worker w : workers) {
			if (w.getTask() == UnitTask.MINERALS) {
				return w;
			}
		}
		return null;
	}

	public int getWorkerCount() {
		return workers.size();
	}

	public int getMineralWorkerCount() {
		int i = 0;
		for (Worker w : workers) {
			if (w.getTask() == UnitTask.MINERALS) {
				i++;
			}
		}
		return i;
	}

	public void addWorker(Worker w) {
		w.setBase(this);
		workers.add(w);
	}

	public boolean removeWorker(Worker w) {
		return workers.remove(w);
	}

	public BaseLocation getLocation() {
		return location;
	}

	public void setPlayer(Player p) {
		player = p;
		lastScouted = GameHandler.getFrameCount();
	}

	public Player getPlayer() {
		return player;
	}

	/**
	 * Set the last scouted timer to the current time in frames.
	 */
	public void setLastScouted() {
		lastScouted = GameHandler.getFrameCount();
	}

	/**
	 * Get the time this base was last scouted. Will update the lastScouted time
	 * if the base is still visible.
	 * 
	 * @return The time the base was last scouted, in frames.
	 */
	public int getLastScouted() {
		TilePosition tp = location.getTilePosition();
		if (GameHandler.isVisible(tp.getX(), tp.getY())) {
			lastScouted = GameHandler.getFrameCount();
		}
		return lastScouted;
	}

	/**
	 * Take a geyser at this base
	 * 
	 * @throws NoGeyserAvailableException
	 */
	public void takeGas() throws NoGeyserAvailableException {
		TilePosition gasTilePosition = gas.stream().filter(r -> r.gasTaken() == false).findAny()
				.orElseThrow(() -> new NoGeyserAvailableException()).getUnit().getTilePosition();
		if (!BuildManager.buildingPlannedForLocation(gasTilePosition)) {
			BuildManager.addBuilding(gasTilePosition, UnitType.Terran_Refinery);
		}
	}
}