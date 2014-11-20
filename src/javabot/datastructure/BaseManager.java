package javabot.datastructure;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javabot.gamestructure.DebugEngine;
import javabot.gamestructure.DebugModule;
import javabot.gamestructure.Debuggable;
import javabot.gamestructure.GameHandler;
import javabot.model.Unit;
import javabot.util.BWColor;

public class BaseManager implements Iterable<Base>, Debuggable {
	private Set<Base> bases;
	private Base main;

	private int selfPlayerID;

	public BaseManager(int myPlayerID, GameHandler game) {
		bases = new HashSet<Base>();

		selfPlayerID = myPlayerID;
	}

	public void addBase(Base b) {
		bases.add(b);
	}

	public Set<Base> getMyBases() {
		Set<Base> myBases = new HashSet<Base>();
		for (Base b : bases) {
			if (b.getOwner() == selfPlayerID) {
				myBases.add(b);
			}
		}
		return myBases;
	}

	public Set<Base> getEnemyBases() {
		Set<Base> myBases = new HashSet<Base>();
		for (Base b : bases) {
			// TODO make this work with allies?
			if (b.getOwner() != selfPlayerID) {
				myBases.add(b);
			}
		}
		return myBases;
	}

	// Gets the closest base from a given (x, y) position
	public Base getClosestBase(int x, int y) {
		Base closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Base b : bases) {
			double distance = Point.distance(x, y, b.location.getX(),
					b.location.getY());
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = b;
			}
		}
		return closest;
	}

	public Worker getBuilder() {
		for (Base b : bases) {
			Worker u = b.getBuilder();
			if (u != null) {
				return u;
			}
		}
		return null;
	}

	// Returns an iterator over all the bases
	@Override
	public Iterator<Base> iterator() {
		return bases.iterator();
	}

	public void setMain(Base b) {
		main = b;
	}

	public Base getMain() {
		return main;
	}

	@Override
	public void registerDebugFunctions(GameHandler g) {
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				engine.drawText(main.location.getX(), main.location.getY(),
						"Main", false);

				for (Base b : bases) {
					if (b.commandCenter != null) {
						int x = b.commandCenter.getX() - 32 * 2;
						int y = b.commandCenter.getY() - 32 * 2;
						engine.drawBox(x, y, x + 32 * 4, y + 32 * 4,
								BWColor.TEAL, false, false);
						engine.drawText(x + 5, y + 5,
								"Workers: " + b.getWorkerCount(), false);
						engine.drawText(x + 5, y + 15, "Mineral Fields: "
								+ b.minerals.size(), false);
					}
					for (Resource r : b.minerals.values()) {
						engine.drawText(r.getX() - 8, r.getY() - 8,
								String.valueOf(r.getNumGatherers()), false);
					}
				}
			}
		});
	}
}
