package gamestructure.debug;

/**
 * A functional interface allowing a user to create a draw function for a
 * {@link DebugModule} with a lambda expression. This is meant to be used in the
 * following fashion:<br>
 * <code>
 * DebugEngine.createDebugModule("name").setDraw(<b>() -> DebugEngine.drawXXX()</b>);
 * </code>
 * 
 * @author wesley
 *
 */
@FunctionalInterface
public interface DrawFunction {
	/**
	 * 
	 * @throws ShapeOverflowException
	 *             Calls to {@link DebugEngine}'s drawing methods may throw an
	 *             exception. If so, it should be passed through here.
	 */
	public void run() throws ShapeOverflowException;
}
