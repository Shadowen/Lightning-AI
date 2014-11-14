package javabot.datastructure;

import java.awt.Point;

import javabot.gamestructure.GameHandler;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class Worker {

	private Unit unit;

	private Resource currentResource;
	private Base base;
	private GameHandler game;

	public Worker(GameHandler g, Unit u, Base b) {
		game = g;
		unit = u;
		base = b;
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
		base.workers.remove(this);
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

	public boolean isBuilding() {
		return unit.isConstructing();
	}

}
