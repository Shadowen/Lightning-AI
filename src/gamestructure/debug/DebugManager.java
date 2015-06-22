package gamestructure.debug;

import gamestructure.GameHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugManager {
	/** The game being acted on **/
	private static Map<String, DebugModule> debugModules;

	/**
	 * Construct a DebugEngine for the specified game.
	 * 
	 * @param igame
	 *            The game to debug for.
	 */
	public static void init() {
		debugModules = new HashMap<String, DebugModule>();

		// Debugger help
		createDebugModule("help")
				.addCommand(
						null,
						(c) -> {
							GameHandler
									.sendText("Type \"/help modules\" for a complete list of modules.");
							GameHandler
									.sendText("Type \"/help <name>\" for more information on a specific module.");
						})
				.addAlias("help")
				.addCommand(
						"modules",
						(c) -> debugModules.forEach((k, v) -> GameHandler
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
				GameHandler.sendText(d.name);
				break;
			}
		}
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
