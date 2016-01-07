package utils;

public final class Utils {
	public static final int positiveModulus(int a, int b) {
		return (a % b + b) % b;
	}
}
