package javabot.gamestructure;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import javabot.util.BWColor;

public class DebugEngine {
	private GameHandler game;
	private List<DebugModule> debugModules;

	public DebugEngine(GameHandler igame) {
		game = igame;
		debugModules = new ArrayList<DebugModule>();
	}

	public void draw() {
		for (DebugModule d : debugModules) {
			d.draw(this);
		}
	}

	public void registerDebugFunction(DebugModule m) {
		debugModules.add(m);
	}

	public void drawCircle(int x, int y, int radius, int color, boolean fill,
			boolean screenCoords) {
		game.drawCircle(x, y, radius, color, fill, screenCoords);
	}

	public void drawLine(int x1, int y1, int x2, int y2, int color,
			boolean screenCoords) {
		game.drawLine(x1, y1, x2, y2, color, screenCoords);
	}

	public void drawBox(int left, int top, int right, int bottom, int color,
			boolean fill, boolean screenCoords) {
		game.drawBox(left, top, right, bottom, color, fill, screenCoords);
	}

	public void drawText(int x, int y, String message, boolean screenCoordinates) {
		game.drawText(x, y, message, screenCoordinates);
	}

}
