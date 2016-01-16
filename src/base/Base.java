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