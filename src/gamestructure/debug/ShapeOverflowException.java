package gamestructure.debug;

/**
 * This exception is thrown when the {@linkplain DebugManager} attempts to draw
 * a total of more than {@link DebugManager#MAX_SHAPES}.
 * 
 */
public class ShapeOverflowException extends Exception {
	private int numShapes = -1;

	public ShapeOverflowException(int inumShapes) {
		numShapes = inumShapes;
	}

	public int getNumShapes() {
		return numShapes;
	}

	@Override
	public String getMessage() {
		if (numShapes > 0) {
			return getStackTrace()[1].getClassName()
					+ " tried to draw too many shapes [ " + numShapes + "]!";
		}
		return getStackTrace()[1].getClassName()
				+ " tried to draw too many shapes!";
	}
}
