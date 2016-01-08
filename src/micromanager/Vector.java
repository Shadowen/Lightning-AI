package micromanager;

import bwapi.Position;

public class Vector {
	private double x;
	private double y;

	public Vector(double ix, double iy) {
		x = ix;
		y = iy;
	}

	public Vector(Position from, Position to) {
		x = to.getX() - from.getX();
		y = to.getY() - from.getY();
	}

	public double getX() {
		return x;
	}

	public Vector setX(double x) {
		this.x = x;
		return this;
	}

	public double getY() {
		return y;
	}

	public Vector setY(double y) {
		this.y = y;
		return this;
	}

	public double getLength() {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
	}

	public Vector normalize() {
		double l = getLength();
		return this.setX(x / l).setY(y / l);
	}

	public Vector scalarMultiply(double s) {
		return this.setX(x * s).setY(y * s);
	}

	public Position toPosition() {
		return new Position((int) getX(), (int) getY());
	}

	public static Vector fromAngle(double angle) {
		return new Vector(Math.cos(angle), Math.sin(angle));
	}

	public static Vector add(Vector... vs) {
		double nx = 0;
		double ny = 0;
		for (Vector v : vs) {
			nx += v.x;
			ny += v.y;
		}
		return new Vector(nx, ny);
	}

	public static Vector perpendicular(Vector v) {
		return new Vector(-v.y, v.x);
	}

	public static double dotProduct(Vector v1, Vector v2) {
		return v1.x * v2.x + v1.y * v2.y;
	}

	public static double angleBetween(Vector v1, Vector v2) {
		return Math.acos(Vector.dotProduct(v1, v2) / v1.getLength() / v2.getLength());
	}
}
