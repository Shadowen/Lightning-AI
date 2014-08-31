package javabot.botstate;

import java.awt.Point;

import javabot.JavaBot;
import javabot.datastructure.Base;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class OpeningBuildState extends BotState {

	public OpeningBuildState(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		for (Base b : bases) {
			// Train SCVS if necessary
			if (b.workers.size() < b.getMineralCount() * 2) {
				if (b.commandCenter.getTrainingQueueSize() < 1
						&& game.getSelf().getMinerals() >= 50) {
					game.train(b.commandCenter.getID(),
							UnitTypes.Terran_SCV.ordinal());
				}
			}
		}

		// Build supply depots if necessary
		if (game.getSelf().getSupplyUsed() > game.getSelf().getSupplyTotal() - 3) {
			Unit builder = bases.get(0).getBuilder();
			Point buildLocation = JavaBot.getBuildTile(game, builder.getID(),
					UnitTypes.Terran_Supply_Depot.ordinal(), builder.getX(),
					builder.getY());
			game.build(builder.getID(), buildLocation.x, buildLocation.y,
					UnitTypes.Terran_Supply_Depot.ordinal());
		}
		return this;
	}

}
