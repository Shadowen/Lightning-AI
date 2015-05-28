package gamestructure.debug;

/**
 * This exception is thrown when the {@linkplain DebugEngine} attempts to draw a
 * total of more than {@link DebugEngine#MAX_SHAPES}.
 * 
 */
public class ShapeOverflowException extends Exception {
	@Override
	public String getMessage() {
		return getStackTrace()[1].getClassName()
				+ " tried to draw too many shapes!";
	}
}
