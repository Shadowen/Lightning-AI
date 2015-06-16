package gamestructure.debug;

/**
 * This exception is thrown when the {@linkplain DebugManager} attempts to draw a
 * total of more than {@link DebugManager#MAX_SHAPES}.
 * 
 */
public class ShapeOverflowException extends Exception {
	@Override
	public String getMessage() {
		return getStackTrace()[1].getClassName()
				+ " tried to draw too many shapes!";
	}
}
