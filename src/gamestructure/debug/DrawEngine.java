package gamestructure.debug;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Queue;

import bwapi.Color;
import bwapi.Game;
import bwapi.Position;

public class DrawEngine {
	/** The game being acted on **/
	private static Game game;
	/**
	 * Current coordinates of the screen. Only draw things currently inside the
	 * viewport.
	 */
	private static Rectangle viewport = new Rectangle(640, 480);

	/**
	 * The number of shapes drawn so far in the current frame. If
	 * {@link #MAX_SHAPES} is exceeded, a {@link ShapeOverflowException} is
	 * thrown.
	 **/
	private static int shapeCount = 0;
	private static int shapesRejected = 0;
	/**
	 * The maximum amount of shapes that can be drawn by {@link DebugManager}
	 * before errors start occurring. This should be between 26000 and 39000
	 * depending on the type of shapes being drawn.
	 **/
	private static final int MAX_SHAPES = 26000;

	public static void init(Game igame) {
		System.out.print("Starting DrawEngine... ");
		game = igame;
		// Debugger debugger
		DebugManager.createDebugModule("shapecount").setDraw(() -> {
			DrawEngine.drawTextScreen(400, 100, "Debug Shapes: " + String.valueOf(shapeCount + 1) + "/" + MAX_SHAPES
					+ " (" + String.valueOf(shapesRejected) + " rejected)");
			// Reset the shapecount
			shapeCount = 0;
			shapesRejected = 0;
		}).setActive(true);
		System.out.println("Success!");
	}

	/**
	 * This constructor should never be used.
	 */
	private DrawEngine() {
	}

	/**
	 * Updates the screen coordinates of the {@link DrawEngine}. Must be called
	 * once per frame for optimal performance.
	 * 
	 */
	public static void updateScreenPosition() {
		Position screenPosition = game.getScreenPosition();
		viewport.setLocation(screenPosition.getX(), screenPosition.getY());
	}

	/**
	 * Draws a rectangular box on the map using the {@link javabot.JNIBWAPI}
	 * native methods. Coordinates are relative to the top left corner of the
	 * map. The shape is only drawn if it falls inside the current viewport.
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawBoxMap(int left, int top, int right, int bottom, Color color, boolean fill)
			throws ShapeOverflowException {
		if (viewport.intersects(left, top, right - left, bottom - top)) {
			game.drawBoxMap(left, top, right, bottom, color, fill);
			shapeCount++;
			if (shapeCount > MAX_SHAPES) {
				throw new ShapeOverflowException(shapeCount);
			}
		} else {
			shapesRejected++;
		}
	}

	/**
	 * Draws a rectangular box on the screen using the {@link javabot.JNIBWAPI}
	 * native methods. Coordinates are relative to the top left corner of the
	 * screen.
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawBoxScreen(int left, int top, int right, int bottom, Color color, boolean fill)
			throws ShapeOverflowException {
		game.drawBoxScreen(left, top, right, bottom, color, fill);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws a circle on the map using the {@link javabot.JNIBWAPI} native
	 * methods. Coordinates are relative to the top left corner of the map. The
	 * shape is only drawn if it falls inside the current viewport.
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawCircleMap(int x, int y, int radius, Color color, boolean fill)
			throws ShapeOverflowException {
		if (viewport.intersects(x - radius, y - radius, 2 * radius, 2 * radius)) {
			game.drawCircleMap(x, y, radius, color, fill);
			shapeCount++;
			if (shapeCount > MAX_SHAPES) {
				throw new ShapeOverflowException(shapeCount);
			}
		} else {
			shapesRejected++;
		}
	}

	/**
	 * Draws a circle on the screen using the {@link javabot.JNIBWAPI} native
	 * methods. Coordinates are relative to the top left corner of the screen.
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawCircleScreen(int x, int y, int radius, Color color, boolean fill)
			throws ShapeOverflowException {
		game.drawCircleScreen(x, y, radius, color, fill);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws a dot on the map using the {@link Game.drawDotMap} native methods.
	 * 
	 * @param x
	 *            The x coordinate in pixels.
	 * @param y
	 *            The y coordinate in pixels.
	 * @param color
	 *            The colour as a {@link bwapi.Color}.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawDotMap(int x, int y, Color color) throws ShapeOverflowException {
		game.drawDotMap(x, y, color);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws a dot on the screen using the {@link Game.drawDotMap} native
	 * methods.
	 * 
	 * @param x
	 *            The x coordinate in pixels.
	 * @param y
	 *            The y coordinate in pixels.
	 * @param color
	 *            The colour as a {@link bwapi.Color}.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawDotScreen(int x, int y, Color color) throws ShapeOverflowException {
		game.drawDotScreen(x, y, color);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws an ellipse on the map using the {@link javabot.JNIBWAPI} native
	 * methods. Coordinates are relative to the top left corner of the map.
	 * 
	 * @param x
	 *            The x coordinate of the center of the ellipse in pixels.
	 * @param y
	 *            The y coordinate of the center of the ellipse in pixels.
	 * @param xrad
	 *            The horizontal radius of the ellipse in pixels.
	 * @param yrad
	 *            The vertical radius of the ellipse in pixels.
	 * @param color
	 *            The color as a {@link BWColor}.
	 * @param fill
	 *            If true, fill the ellipse with the same color.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawEllipseMap(int x, int y, int xrad, int yrad, Color color, boolean fill)
			throws ShapeOverflowException {
		game.drawEllipseMap(x, y, xrad, yrad, color, fill);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws an ellipse on the screen using the {@link javabot.JNIBWAPI} native
	 * methods. Coordinates are relative to the top left corner of the screen.
	 * 
	 * @param x
	 *            The x coordinate of the center of the ellipse in pixels.
	 * @param y
	 *            The y coordinate of the center of the ellipse in pixels.
	 * @param xrad
	 *            The horizontal radius of the ellipse.
	 * @param yrad
	 *            The vertical radius of the ellipse.
	 * @param color
	 *            The color as a {@link BWColor}.
	 * @param fill
	 *            If true, fill the ellipse with the same color.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawEllipseScreen(int x, int y, int xrad, int yrad, Color color, boolean fill)
			throws ShapeOverflowException {
		game.drawEllipseScreen(x, y, xrad, yrad, color, fill);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws a line on the map using the {@link javabot.JNIBWAPI} native
	 * methods. Coordinates are relative to the top left corner of the map.
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawLineMap(int x1, int y1, int x2, int y2, Color color) throws ShapeOverflowException {
		game.drawLineMap(x1, y1, x2, y2, color);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws a line on the screen using the {@link javabot.JNIBWAPI} native
	 * methods. Coordinates are relative to the top left corner of the screen.
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawLineScreen(int x1, int y1, int x2, int y2, Color color) throws ShapeOverflowException {
		game.drawLineScreen(x1, y1, x2, y2, color);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
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
	 *            The {@link Object} to be displayed.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawTextScreen(int x, int y, Object message) throws ShapeOverflowException {
		game.drawTextScreen(x, y, message.toString());
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws text on the map using the {@link javabot.JNIBWAPI} native methods.
	 * The shape is only drawn if it is inside the current viewport.
	 * 
	 * @param x
	 *            The x coordinate to start drawing from in pixels.
	 * @param y
	 *            The y coordinate of the bottom of the line of text in pixels.
	 * @param message
	 *            The message to be displayed.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawTextMap(int x, int y, String message) throws ShapeOverflowException {
		// TODO find a better bounding box
		if (viewport.intersects(x, y, 15 * message.length(), 15)) {
			game.drawTextMap(x, y, message);
			shapeCount++;
			if (shapeCount > MAX_SHAPES) {
				throw new ShapeOverflowException(shapeCount);
			}
		} else {
			shapesRejected++;
		}
	}

	/**
	 * Draws text on the map using the {@link javabot.JNIBWAPI} native methods.
	 * The shape is only drawn if it is inside the current viewport.
	 * 
	 * @param x
	 *            The x coordinate to start drawing from in pixels.
	 * @param y
	 *            The y coordinate of the bottom of the line of text in pixels.
	 * @param message
	 *            The {@link Object} to be displayed.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawTextMap(int x, int y, Object message) throws ShapeOverflowException {
		drawTextMap(x, y, message.toString());
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawTextScreen(int x, int y, String message) throws ShapeOverflowException {
		game.drawTextScreen(x, y, message);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException(shapeCount);
		}
	}

	/**
	 * Draws an arrow on the map using the {@link javabot.JNIBWAPI} native
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugManager} tries to draw too many
	 *             shapes.
	 */
	public static void drawArrowMap(int x1, int y1, int x2, int y2, Color color) throws ShapeOverflowException {
		// Math at:
		// http://stackoverflow.com/questions/10316180/how-to-calculate-the-coordinates-of-a-arrowhead-based-on-the-arrow

		final double length = Point.distance(x1, y1, x2, y2);
		// Handle null lines
		if (length == 0) {
			drawDotMap(x1, y1, color);
			return;
		}

		final double arrowheadLength = length / 3;
		final double arrowheadHalfWidth = length / 3;
		drawLineMap(x1, y1, x2, y2, color);

		// Calculate the arrowhead
		// u is collinear
		final double ux = (x2 - x1) / length;
		final double uy = (y2 - y1) / length;
		// v is perpendicular
		final double vx = -uy;
		final double vy = ux;
		// The first line
		final int a1x = (int) Math.round(x2 - arrowheadLength * ux + arrowheadHalfWidth * vx);
		final int a1y = (int) Math.round(y2 - arrowheadLength * uy + arrowheadHalfWidth * vy);
		drawLineMap(x2, y2, a1x, a1y, color);
		// The second line
		final int a2x = (int) Math.round(x2 - arrowheadLength * ux - arrowheadHalfWidth * vx);
		final int a2y = (int) Math.round(y2 - arrowheadLength * uy - arrowheadHalfWidth * vy);
		drawLineMap(x2, y2, a2x, a2y, color);
	}

	public static void drawXMap(int x, int y, Color colour) throws ShapeOverflowException {
		drawLineMap(x - 10, y - 10, x + 10, y + 10, colour);
		drawLineMap(x + 10, y - 10, x - 10, y + 10, colour);
	}

	public static void drawPath(Queue<Position> path) throws ShapeOverflowException {
		final Iterator<Position> it = path.iterator();
		Position previous = it.hasNext() ? it.next() : null;
		while (it.hasNext()) {
			final Position current = it.next();
			drawArrowMap(previous.getX(), previous.getY(), current.getX(), current.getY(), Color.Yellow);
			previous = current;
		}
	}
}
