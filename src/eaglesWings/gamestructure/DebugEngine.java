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
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawText(400, 100,
						"Debug Shapes: " + String.valueOf(shapeCount + 1) + "/"
								+ MAX_SHAPES, true);
			}
		});
	}

	/***
	 * The number of shapes drawn so far in the current frame. If
	 * {@link #MAX_SHAPES} is exceeded, a {@link ShapeOverflowException} is
	 * thrown.
	 ***/
	private int shapeCount = 0;
	/**
	 * The maximum amount of shapes that can be drawn by {@link DebugEngine}
	 * before errors start occuring. This should be between 26000 and 39000
	 * depending on the type of shapes being drawn.
	 **/
	private static final int MAX_SHAPES = 26000;

	/**
	 * Iterate through the {@link #activeDebugModules} and tell each one to
	 * {@link DebugModule#draw}.
	 */
	public void draw() {
		// Try to draw all of the debugModules. If we are interrupted by too
		// many objects attempting to draw, then print the stack trace.
		shapeCount = 0;
		try {
			for (DebugModule d : activeDebugModules) {
				d.draw(this);
			}
		} catch (ShapeOverflowException soe) {
			// Someone attempted to draw a lot of shapes!
			soe.printStackTrace();
			// TODO get the actual debugModule that caused the overflow and
			// print it.
		}
	}

	/**
	 * Draws a dot on the screen using the {@link javabot.JNIBWAPI} native
	 * methods.
	 * 
	 * @param x
	 *            The x coordinate in pixels.
	 * @param y
	 *            The y coordinate in pixels.
	 * @param color
	 *            The colour as a {@link BWColor}.
	 * @param screenCoords
	 *            If true, draw in coordinates relative to the screen. If false,
	 *            draw relative to the map.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawDot(int x, int y, int color, boolean screenCoords)
			throws ShapeOverflowException {
		game.drawDot(x, y, color, screenCoords);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws a circle on the screen using the {@link javabot.JNIBWAPI} native
	 * methods.
	 * 
	 * @param x
	 *            The x coordinate of the center of the circle in pixels.
	 * @param y
	 *            The y coordinate of the center of the circle in pixels.
	 * @param radius
	 *            The radius of the circle in pixels.
	 * @param color
	 *            The color as a {@link BWColor}.
	 * @param fill
	 *            If true, fill the circle with the same color.
	 * @param screenCoords
	 *            If true, draw in coordinates relative to the screen. If false,
	 *            draw relative to the map.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawCircle(int x, int y, int radius, int color, boolean fill,
			boolean screenCoords) throws ShapeOverflowException {
		game.drawCircle(x, y, radius, color, fill, screenCoords);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws a line on the screen using the {@link javabot.JNIBWAPI} native
	 * methods.
	 * 
	 * @param x1
	 *            The start x coordinate in pixels.
	 * @param y1
	 *            The start y coordinate in pixels.
	 * @param x2
	 *            The end x coordinate in pixels.
	 * @param y2
	 *            The end y coordinate in pixels.
	 * @param color
	 *            The colour as a {@link BWColor}.
	 * @param screenCoords
	 *            If true, draw in coordinates relative to the screen. If false,
	 *            draw relative to the map.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawLine(int x1, int y1, int x2, int y2, int color,
			boolean screenCoords) throws ShapeOverflowException {
		game.drawLine(x1, y1, x2, y2, color, screenCoords);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws a rectangular box on the screen using the {@link javabot.JNIBWAPI}
	 * native methods.
	 * 
	 * @param left
	 *            The x coordinate of the left side in pixels.
	 * @param top
	 *            The y coordinate of the top side in pixels.
	 * @param right
	 *            The x coordinate of the right side in pixels.
	 * @param bottom
	 *            The y coordinate of the bottom side in pixels.
	 * @param fill
	 *            If true, fill the box with the same color.
	 * @param color
	 *            The colour as a {@link BWColor}.
	 * @param screenCoords
	 *            If true, draw in coordinates relative to the screen. If false,
	 *            draw relative to the map.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawBox(int left, int top, int right, int bottom, int color,
			boolean fill, boolean screenCoords) throws ShapeOverflowException {
		game.drawBox(left, top, right, bottom, color, fill, screenCoords);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws text on the screen using the {@link javabot.JNIBWAPI} native
	 * methods.
	 * 
	 * @param x
	 *            The x coordinate to start drawing from in pixels.
	 * @param y
	 *            The y coordinate of the bottom of the line of text in pixels.
	 * @param message
	 *            The message to be displayed.
	 * @param fill
	 *            If true, fill the box with the same color.
	 * @param screenCoords
	 *            If true, draw in coordinates relative to the screen. If false,
	 *            draw relative to the map.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawText(int x, int y, String message, boolean screenCoords)
			throws ShapeOverflowException {
		game.drawText(x, y, message, screenCoords);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws an arrow on the screen using the {@link javabot.JNIBWAPI} native
	 * methods. The arrow points from (x1, y1) to (x2, y2).
	 * 
	 * @param x1
	 *            The start x coordinate in pixels.
	 * @param y1
	 *            The start y coordinate in pixels.
	 * @param x2
	 *            The end x coordinate in pixels.
	 * @param y2
	 *            The end y coordinate in pixels.
	 * @param color
	 *            The colour as a {@link BWColor}.
	 * @param screenCoords
	 *            If true, draw in coordinates relative to the screen. If false,
	 *            draw relative to the map.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawArrow(int x1, int y1, int x2, int y2, int color,
			boolean screenCoords) throws ShapeOverflowException {
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
}
