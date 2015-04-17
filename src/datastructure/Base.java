package datastructure;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import bwapi.Unit;
import bwta.BaseLocation;
import gamestructure.GameHandler;

public class Base {
	private GameHandler game;

	public List<MineralResource> minerals;
	public List<GasResource> gas;
	public List<Worker> workers;
	public Unit commandCenter;
	private BaseLocation location;

	private BaseStatus status;
	// When the base was last scouted, in game frames
	private long lastScouted;

	public Base(GameHandler g, BaseLocation l) {
		game = g;

		workers = new ArrayList<Worker>();
		location = l;

		minerals = new ArrayList<MineralResource>();
		gas = new ArrayList<GasResource>();

		status = BaseStatus.UNOCCUPIED;
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
			WorkerTask currentTask = worker.getTask();
			if (worker.isIdle()
					&& (currentTask == WorkerTask.Mining_Minerals || currentTask == WorkerTask.Mining_Gas)) {
				game.sendText("Idle worker detected!");
				// Get back to work
				if (worker.getCurrentResource() != null) {
					worker.gather(worker.getCurrentResource());
					continue;
				}

				// Try to assign one worker to each mineral first
				Resource mineral = null;
				double distance = 0;

				// This variable is the loop counter
				// It only allows maxMiners to gather each resource patch each
				// loop.
				int maxMiners = 1;
				boolean workerAssigned = false;
				while (!workerAssigned && maxMiners < 3) {
					for (MineralResource m : minerals) {
						if (m.getNumGatherers() < maxMiners) {
							// Find closest mineral patch
							double newDistance = Point.distance(worker.getX(),
									worker.getY(), m.getX(), m.getY());
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
			}
		}
	}

	public Worker getBuilder() {
		for (Worker w : workers) {
			if (w.getTask() == WorkerTask.Mining_Minerals) {
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
			if (w.getTask() == WorkerTask.Mining_Minerals) {
				i++;
			}
		}
		return i;
	}

	public void addWorker(Unit unit) {
		Worker w = new Worker(game, unit);
		w.setBase(this);
		w.setTask(WorkerTask.Mining_Minerals, null);
		workers.add(w); // TODO figure out what to do with unitID
	}

	public boolean removeWorker(final Unit unit) {
		return workers.removeIf(new Predicate<Worker>() {
			@Override
			public boolean test(Worker t) {
				if (t.getUnit() == unit) {
					t.unitDestroyed();
					return true;
				}
				return false;
			}
		});
	}

	public BaseLocation getLocation() {
		return location;
	}

	public void setStatus(BaseStatus s) {
		status = s;
		lastScouted = game.getFrameCount();
	}

	public BaseStatus getStatus() {
		return status;
	}

	public long getLastScouted() {
		return lastScouted;
	}
}