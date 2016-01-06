package pathfinder;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * One node of the Quadtree implementation
 * 
 * @author wesley
 *
 * @param <T>
 *            The type of element stored in the tree.
 */
// /http://gamedev.stackexchange.com/questions/31021/quadtree-store-only-points-or-regions
public class QTNode<T extends Shape> {
	/**
	 * Minimum number of objects in a QTNode before it is collapsed into a
	 * higher level node
	 */
	private int MIN_OBJECTS = 0;
	/**
	 * Maximum number of objects in a QTNode before it is decomposed into
	 * smaller nodes
	 */
	private int MAX_OBJECTS = 1;
	/** Maximum depth of the tree */
	private int MAX_DEPTH = 20;

	/** The number of levels above this node to the root. */
	private final int depth;
	/** Objects contained at the leaf level of the Quadtree */
	public Set<T> objects;
	public Rectangle bounds;
	public QTNode<T> parent;
	public Set<QTNode<T>> nodes;

	/**
	 * Create a root level QuadtreeNode. It has no parent by definition. This is
	 * the only constructor that should be called externally.
	 * 
	 * @param pBounds
	 *            The bounding box of this level of quadtree. Any objects
	 *            outside this area will be ignored.
	 */
	public QTNode(Rectangle pBounds) {
		this(null, pBounds, 0);
	}

	/**
	 * Create an internal QuadtreeNode.
	 * 
	 * @param pparent
	 *            The parent Quadtree. This should never be null except for the
	 *            root level.
	 * @param pBounds
	 *            The bounds of the quadtree level. The children of a node must
	 *            completely cover its area.
	 * @param idepth
	 *            The depth of the new node relative to the root.
	 */
	private QTNode(QTNode<T> pparent, Rectangle pBounds, int idepth) {
		parent = pparent;
		objects = new HashSet<>();
		nodes = new HashSet<>();
		bounds = pBounds;
		depth = idepth;
	}

	private void split() {
		int subWidth = (int) (bounds.getWidth() / 2);
		int subHeight = (int) (bounds.getHeight() / 2);
		int x = (int) bounds.getX();
		int y = (int) bounds.getY();

		nodes.add(new QTNode<T>(this, new Rectangle(x, y, subWidth, subHeight), depth + 1));
		nodes.add(new QTNode<T>(this, new Rectangle(x + subWidth, y, subWidth, subHeight), depth + 1));
		nodes.add(new QTNode<T>(this, new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight), depth + 1));
		nodes.add(new QTNode<T>(this, new Rectangle(x, y + subHeight, subWidth, subHeight), depth + 1));

		// Reinsert all shapes into children
		for (T o : objects) {
			insert(o);
		}
		objects.clear();
	}

	/**
	 * Insert a new object into the quadtree.
	 * 
	 * @param e
	 *            The bounding shape of the object
	 * @return <b>true</b> if the insertion was successful.<br>
	 *         <b>false</b> if the insertion failed because the object does not
	 *         belong in this level of the quad tree.
	 */
	public boolean insert(T t) {
		// Does the shape fit into this level?
		if (!bounds.intersects(t.getBounds())) {
			return false;
		}

		// No split, add to existing children
		if (!nodes.isEmpty()) {
			for (QTNode<T> st : nodes) {
				st.insert(t);
			}
			return true;
		}

		// Add to current node (leaf)
		objects.add(t);
		// Split
		if (objects.size() > MAX_OBJECTS && depth < MAX_DEPTH) {
			split();
		}

		return true;
	}

	public boolean remove(T t) {
		// Does the shape fit into this level?
		if (!t.getBounds().intersects(bounds)) {
			return false;
		}

		// Find it
		if (!nodes.isEmpty()) {
			for (QTNode<T> st : nodes) {
				st.remove(t);
			}
			// Merge
			if (objects.size() < MIN_OBJECTS) {
				merge();
			}
			return true;
		}

		objects.remove(t);
		return true;
	}

	/**
	 * Merge the children of this node, performing the given reduction operation
	 * on the objects found.
	 * 
	 * @param collector
	 */
	public void merge(Collector<? super QTNode<T>, ?, ?> collector) {
		if (collector != null) {
			for (QTNode<T> n : nodes) {
				nodes.stream().collect(collector);
			}
		}
		nodes.clear();
	}

	public void merge() {
		merge(null);
	}

	/**
	 * Gets the depth of this node in the quadtree structure. The depth is
	 * defined as the number of levels of nodes above this node to the root.<br>
	 * This function returns a stored value, so there are no performance
	 * implications.
	 * 
	 * @return the depth of this node
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Gets the greatest number of levels in the quadtree below this node.
	 * 
	 * @return the number of levels to the deepest part of the tree
	 */
	public int getDepthBelow() {
		int ret = 0;
		for (QTNode<T> st : nodes) {
			ret = Math.max(ret, st.getDepthBelow() + 1);
		}
		return ret;
	}

	/**
	 * Applies a consumer to the tree in pre-order.
	 * 
	 * @param consumer
	 *            the consumer to apply
	 */
	public void processNodes(Consumer<QTNode<T>> consumer) {
		consumer.accept(this);
		for (QTNode<T> node : nodes) {
			node.processNodes(consumer);
		}
	}

	/**
	 * Applies a consumer to the tree from leaves to root. Each node
	 * additionally accepts a list of its children's results.
	 * 
	 * @param function
	 *            the function to apply
	 */
	public <R> R recurseFromLeaves(BiFunction<QTNode<T>, List<R>, R> function) {
		List<R> results = new ArrayList<>(4);
		for (QTNode<T> node : nodes) {
			results.add(node.recurseFromLeaves(function));
		}
		return function.apply(this, results);
	}

	public Rectangle getBounds() {
		return (Rectangle) bounds.clone();
	}

	public Optional<T> getAt(Point point) {
		// Does the shape fit into this level?
		if (!bounds.contains(point)) {
			return Optional.empty();
		}

		// Find it
		for (QTNode<T> st : nodes) {
			Optional<T> sto = st.getAt(point);
			if (sto.isPresent()) {
				return sto;
			}
		}
		for (T e : objects) {
			if (e.contains(point)) {
				return Optional.of(e);
			}
		}

		return Optional.empty();
	}

	public Stream<T> stream() {
		return Stream.concat(objects.stream(), nodes.stream().flatMap(n -> n.stream()));
	}
}
