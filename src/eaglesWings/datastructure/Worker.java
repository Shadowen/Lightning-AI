package eaglesWings.datastructure;

import eaglesWings.gamestructure.GameHandler;
import javabot.model.Unit;

public class Worker {

	private Unit unit;

	private Resource currentResource;
	private GameHandler game;

	private WorkerTask currentTask;

	public Worker(GameHandler g, Unit u) {
		game = g;
		unit = u;
	}

	public boolean isIdle() {
		return unit.isIdle();
	}

	public void gather() {
		gather(currentResource);
	}

	public void gather(Resource r) {
		if (currentResource != null) {
			currentResource.removeGatherer();
		}

		game.gather(unit.getID(), r.getID());
		currentResource = r;
		r.addGatherer();

		if (r instanceof MineralResource) {
			currentTask = WorkerTask.Mining_Minerals;
		} else if (r instanceof GasResource) {
			currentTask = WorkerTask.Mining_Gas;
		}
	}

	public void build(BuildingPlan toBuild) {
		game.build(unit.getID(), toBuild);
		toBuild.setBuilder(this);
		currentTask = WorkerTask.Constructing_Building;
	}

	public int getX() {
		return unit.getX();
	}

	public int getY() {
		return unit.getY();
	}

	public WorkerTask getCurrentTask() {
		return currentTask;
	}

	public int getID() {
		return unit.getID();
	}

	public void unitDestroyed() {
		if (currentResource != null) {
			currentResource.removeGatherer();
		}
	}

}
