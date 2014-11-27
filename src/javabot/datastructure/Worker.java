package javabot.datastructure;

import javabot.gamestructure.GameHandler;
import javabot.model.Unit;

public class Worker {

	private Unit unit;

	private Resource currentResource;
	private GameHandler game;

	public Worker(GameHandler g, Unit u) {
		game = g;
		unit = u;
	}

	public boolean isIdle() {
		return unit.isIdle();
	}

	public void mine() {
		mine(currentResource);
	}

	public void mine(Resource r) {
		if (currentResource != null) {
			currentResource.removeGatherer();
		}
		game.rightClick(unit.getID(), r.getID());
		currentResource = r;
		r.addGatherer();
	}

	public void build(BuildingPlan toBuild) {
		game.build(unit.getID(), toBuild);
		toBuild.setBuilder(this);
	}

	public int getX() {
		return unit.getX();
	}

	public int getY() {
		return unit.getY();
	}

	public boolean isGathering() {
		return currentResource != null;
	}

	public int getID() {
		return unit.getID();
	}

	public void stopMining() {
		currentResource.removeGatherer();
	}

	public boolean isConstructing() {
		return unit.isConstructing();
	}

}
