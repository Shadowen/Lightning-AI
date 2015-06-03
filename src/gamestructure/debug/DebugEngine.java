package gamestructure.debug;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bwapi.Color;
import bwapi.Game;

public class DebugEngine {
	/** The game being acted on **/
	private static Game game;
	private static Map<String, DebugModule> debugModules;

	/***
	 * The number of shapes drawn so far in the current frame. If
	 * {@link #MAX_SHAPES} is exceeded, a {@link ShapeOverflowException} is
	 * thrown.
	 ***/
	private static int shapeCount = 0;
	/**
	 * The maximum amount of shapes that can be drawn by {@link DebugEngine}
	 * before errors start occurring. This should be between 26000 and 39000
	 * depending on the type of shapes being drawn.
	 **/
	private static final int MAX_SHAPES = 26000;

	/**
	 * Construct a DebugEngine for the specified game.
	 * 
	 * @param igame
	 *            The game to debug for.
	 */
	public DebugEngine(Game igame) {
		game = igame;
		debugModules = new HashMap<String, DebugModule>();

		// Debugger debugger
		createDebugModule("shapecount")
				.setDraw(
						() -> {
							DebugEngine.drawTextScreen(
									400,
									100,
									"Debug Shapes: "
											+ String.valueOf(shapeCount + 1)
											+ "/" + MAX_SHAPES);
							// Reset the shapecount
							shapeCount = 0;
						});
		// Debugger help
		createDebugModule("help")
				.addCommand(
						null,
						(c) -> {
							DebugEngine
									.sendText("Type \"/help modules\" for a complete list of modules.");
							DebugEngine
									.sendText("Type \"/help <name>\" for more information on a specific module.");
						})
				.addAlias("help")
				.addCommand(
						"modules",
						(c) -> debugModules.forEach((k, v) -> DebugEngine
								.sendText(k)));
	}

	/**
	 * Add a debugModule to the debugEngine.
	 * 
	 * @param debugModule
	 *            The module to be added.
	 */
	public static DebugModule createDebugModule(String name) {
		DebugModule dm = new DebugModule(name);
		debugModules.put(name, dm);
		return dm;
	}

	/**
	 * Iterate through the {@link #debugModules} and tell each one to
	 * {@link DebugModule#draw}.
	 */
	public static void draw() {
		// Try to draw all of the debugModules. If we are interrupted by too
		// many objects attempting to draw, then print the stack trace.
		for (DebugModule d : debugModules.values()) {
			try {
				d.drawIfActive();
			} catch (ShapeOverflowException soe) {
				// Someone attempted to draw a lot of shapes!
				soe.printStackTrace();
				// Get the actual debugModule that caused the overflow and
				// print it.
				game.sendText(d.name);
				break;
			}
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
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawDotMap(int x, int y, Color color)
			throws ShapeOverflowException {
		game.drawDotMap(x, y, color);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
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
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawDotScreen(int x, int y, Color color)
			throws ShapeOverflowException {
		game.drawDotScreen(x, y, color);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws a circle on the map using the {@link javabot.JNIBWAPI} native
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public static void drawCircleMap(int x, int y, int radius, Color color,
			boolean fill) throws ShapeOverflowException {
		game.drawCircleMap(x, y, radius, color, fill);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws a line on the map using the {@link javabot.JNIBWAPI} native
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public static void drawLineMap(int x1, int y1, int x2, int y2, Color color)
			throws ShapeOverflowException {
		game.drawLineMap(x1, y1, x2, y2, color);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws a rectangular box on the map using the {@link javabot.JNIBWAPI}
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public static void drawBoxMap(int left, int top, int right, int bottom,
			Color color, boolean fill) throws ShapeOverflowException {
		game.drawBoxMap(left, top, right, bottom, color, fill);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
		}
	}

	/**
	 * Draws text on the map using the {@link javabot.JNIBWAPI} native methods.
	 * 
	 * @param x
	 *            The x coordinate to start drawing from in pixels.
	 * @param y
	 *            The y coordinate of the bottom of the line of text in pixels.
	 * @param message
	 *            The message to be displayed.
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public static void drawTextMap(int x, int y, String message)
			throws ShapeOverflowException {
		game.drawTextMap(x, y, message);
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
	 * @throws ShapeOverflowException
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public static void drawTextScreen(int x, int y, String message)
			throws ShapeOverflowException {
		game.drawTextScreen(x, y, message);
		shapeCount++;
		if (shapeCount > MAX_SHAPES) {
			throw new ShapeOverflowException();
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
	 *             Thrown if the {@link DebugEngine} tries to draw too many
	 *             shapes.
	 */
	public void drawArrowMap(int x1, int y1, int x2, int y2, Color color)
			throws ShapeOverflowException {
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
		final int a1x = (int) Math.round(x2 - arrowheadLength * ux
				+ arrowheadHalfWidth * vx);
		final int a1y = (int) Math.round(y2 - arrowheadLength * uy
				+ arrowheadHalfWidth * vy);
		drawLineMap(x2, y2, a1x, a1y, color);
		// The second line
		final int a2x = (int) Math.round(x2 - arrowheadLength * ux
				- arrowheadHalfWidth * vx);
		final int a2y = (int) Math.round(y2 - arrowheadLength * uy
				- arrowheadHalfWidth * vy);
		drawLineMap(x2, y2, a2x, a2y, color);
	}

	/**
	 * Send the given text in chat.
	 * 
	 * @param string
	 *            the text to send
	 */
	public static void sendText(String string) {
		game.sendText(string);
	}

	/**
	 * Process a command meant for a {@link #DebugModule} and search through
	 * {@link #debugModules} for the correct one to forward it to.
	 * 
	 * @param command
	 *            The command being parsed, split by whitespace.
	 * @throws InvalidCommandException
	 *             if the command cannot be parsed
	 */
	public static void onReceiveCommand(List<String> command)
			throws InvalidCommandException {
		String first = command.get(0);

		if (first.equalsIgnoreCase("all")) {
			for (DebugModule v : debugModules.values()) {
				v.onReceiveCommand(command.subList(1, command.size()));
			}
		} else {
			if (debugModules.containsKey(first)) {
				debugModules.get(first).onReceiveCommand(
						command.subList(1, command.size()));
			}
		}
	}
}
