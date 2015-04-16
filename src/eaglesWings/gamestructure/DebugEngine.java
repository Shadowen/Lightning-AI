package eaglesWings.gamestructure;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.sun.prism.paint.Color;

import javabot.util.BWColor;

public class DebugEngine {
	private GameHandler game;
	public List<DebugModule> activeDebugModules;

	public DebugEngine(GameHandler igame) {
		game = igame;
		activeDebugModules = new ArrayList<DebugModule>();

		// Debugger debugger
		activeDebugModules.add(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				engine.drawText(100, 100,
						"Debug Shapes: " + String.valueOf(shapeCount + 1) + "/"
								+ MAX_SHAPES, true);

			}
		});
	}

	private int shapeCount = 0;
	private static final int MAX_SHAPES = 26000;

	public void draw() {
		shapeCount = 0;
		for (DebugModule d : activeDebugModules) {
			try {
				d.draw(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void drawDot(int x, int y, int color, boolean screenCoords) {
		if (shapeCount < MAX_SHAPES) {
			game.drawDot(x, y, color, screenCoords);
			shapeCount++;
		}
	}

	public void drawCircle(int x, int y, int radius, int color, boolean fill,
			boolean screenCoords) {
		if (shapeCount < MAX_SHAPES) {
			game.drawCircle(x, y, radius, color, fill, screenCoords);
			shapeCount++;
		}
	}

	public void drawLine(int x1, int y1, int x2, int y2, int color,
			boolean screenCoords) {
		if (shapeCount < MAX_SHAPES) {
			game.drawLine(x1, y1, x2, y2, color, screenCoords);
			shapeCount++;
		}
	}

	public void drawArrow(int x1, int y1, int x2, int y2, int color,
			boolean screenCoords) {
		// Math at:
		// http://stackoverflow.com/questions/10316180/how-to-calculate-the-coordinates-of-a-arrowhead-based-on-the-arrow

		double length = Point.distance(x1, y1, x2, y2);
		// Handle null lines
		if (length == 0) {
			drawDot(x1, y1, color, false);
			return;
		}

		double arrowheadLength = length / 3;
		double arrowheadHalfWidth = length / 3;
		drawLine(x1, y1, x2, y2, color, screenCoords);

		// Calculate the arrowhead
		// u is collinear
		double ux = (x2 - x1) / length;
		double uy = (y2 - y1) / length;
		// v is perpendicular
		double vx = -uy;
		double vy = ux;
		// The first line
		int a1x = (int) Math.round(x2 - arrowheadLength * ux
				+ arrowheadHalfWidth * vx);
		int a1y = (int) Math.round(y2 - arrowheadLength * uy
				+ arrowheadHalfWidth * vy);
		drawLine(x2, y2, a1x, a1y, color, screenCoords);
		// The second line
		int a2x = (int) Math.round(x2 - arrowheadLength * ux
				- arrowheadHalfWidth * vx);
		int a2y = (int) Math.round(y2 - arrowheadLength * uy
				- arrowheadHalfWidth * vy);
		drawLine(x2, y2, a2x, a2y, color, screenCoords);
	}

	public void drawBox(int left, int top, int right, int bottom, int color,
			boolean fill, boolean screenCoords) {
		if (shapeCount < MAX_SHAPES) {
			game.drawBox(left, top, right, bottom, color, fill, screenCoords);
			shapeCount++;
		}
	}

	public void drawText(int x, int y, String message, boolean screenCoordinates) {
		if (shapeCount < MAX_SHAPES) {
			game.drawText(x, y, message, screenCoordinates);
			shapeCount++;
		}
	}

}
