package org.powerbot.script.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.powerbot.script.methods.MethodContext;
import org.powerbot.script.methods.MethodProvider;

/**
 * An abstract implementation of a chaining query-based data set filter which is thread safe.
 *
 * @param <T> the super class
 * @param <K> the subject type
 * @author Paris
 */
public abstract class AbstractQuery<T extends AbstractQuery<T, K>, K> extends MethodProvider implements Iterable<K>, Nillable<K> {
	private final ThreadLocal<List<K>> items;
	private final Method set;
	private final AtomicBoolean mark;

	/**
	 * Creates a base {@link AbstractQuery}.
	 *
	 * @param factory the {@link MethodContext} to associate with
	 */
	public AbstractQuery(final MethodContext factory) {
		super(factory);

		items = new ThreadLocal<List<K>>() {
			@Override
			protected List<K> initialValue() {
				return new CopyOnWriteArrayList<>(AbstractQuery.this.get());
			}
		};

		Method set = null;
		try {
			set = CopyOnWriteArrayList.class.getMethod("setArray", Object[].class);
		} catch (final NoSuchMethodException ignored) {
		}
		this.set = set;

		mark = new AtomicBoolean(false);
	}

	/**
	 * Returns {@code this}.
	 *
	 * @return must always return {@code this}
	 */
	protected abstract T getThis();

	/**
	 * Returns a fresh data set.
	 *
	 * @return a new data set for subsequent queries
	 */
	protected abstract List<K> get();

	/**
	 * Selects a fresh data set into the query cache.
	 *
	 * @return {@code this} for the purpose of chaining
	 */
	public T select() {
		mark.set(false);
		final List<K> items = this.items.get(), a = get();
		setArray(items, a);
		return getThis();
	}

	/**
	 * Selects the specified data set into the query cache.
	 *
	 * @param c a {@link List}, {@link Collection} or any other {@link Iterable}
	 *          source of items to replace the existing cache with
	 * @return {@code this} for the purpose of chaining
	 */
	public T select(final Iterable<K> c) {
		validate();
		final List<K> items = this.items.get(), a = new ArrayList<>();
		for (final K k : c) {
			a.add(k);
		}
		setArray(items, a);
		return getThis();
	}

	/**
	 * Selects the items which satisfy the condition of the specified
	 * {@link Filter} into the query cache.
	 *
	 * @param f the condition
	 * @return {@code this} for the purpose of chaining
	 */
	public T select(final Filter<? super K> f) {
		validate();
		final List<K> items = this.items.get(), a = new ArrayList<>(items.size());
		for (final K k : items) {
			if (f.accept(k)) {
				a.add(k);
			}
		}
		setArray(items, a);
		return getThis();
	}

	/**
	 * Sorts the items in the query cache by the specified {@link Comparator}.
	 *
	 * @param c the comparator
	 * @return {@code this} for the purpose of chaining
	 */
	public T sort(final Comparator<? super K> c) {
		validate();
		final List<K> items = this.items.get(), a = new ArrayList<>(items);
		Collections.sort(a, c);
		setArray(items, a);
		return getThis();
	}

	/**
	 * Sorts the items in the query cache by a random rearrangement.
	 *
	 * @return {@code this} for the purpose of chaining
	 */
	public T shuffle() {
		validate();
		final List<K> items = this.items.get(), a = new ArrayList<>(items);
		Collections.shuffle(a);
		setArray(items, a);
		return getThis();
	}

	/**
	 * Reverses the order of items in the query cache.
	 *
	 * @return {@code this} for the purpose of chaining
	 */
	public T reverse() {
		validate();
		final List<K> items = this.items.get(), a = new ArrayList<>(items);
		Collections.reverse(a);
		setArray(items, a);
		return getThis();
	}

	private void setArray(final List<K> a, final List<K> c) {
		if (set != null) {
			try {
				set.invoke(a, c.toArray());
				return;
			} catch (final IllegalAccessException | InvocationTargetException ignored) {
			}
		}

		a.clear();
		a.addAll(c);
	}

	/**
	 * Limits the query cache to the specified number of items.
	 *
	 * @param count the maximum number of items to retain
	 * @return {@code this} for the purpose of chaining
	 */
	public T limit(final int count) {
		return limit(0, count);
	}

	/**
	 * Limits the query cache to the items within the specified bounds.
	 *
	 * @param offset the starting index
	 * @param count  the maximum number of items to retain
	 * @return {@code this} for the purpose of chaining
	 */
	public T limit(final int offset, final int count) {
		validate();
		final List<K> items = this.items.get(), a = new ArrayList<>(count);
		final int c = Math.min(offset + count, items.size());
		for (int i = offset; i < c; i++) {
			a.add(items.get(i));
		}
		setArray(items, a);
		return getThis();
	}

	/**
	 * Limits the query cache to the first item (if any).
	 *
	 * @return {@code this} for the purpose of chaining
	 */
	public T first() {
		return limit(1);
	}

	/**
	 * Adds every item in the query cache to the specified {@link Collection}.
	 *
	 * @param c the {@link Collection} to add to
	 * @return {@code this} for the purpose of chaining
	 */
	public T addTo(final Collection<? super K> c) {
		validate();
		c.addAll(items.get());
		return getThis();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<K> iterator() {
		validate();
		return items.get().iterator();
	}

	/**
	 * Enumerates through each item in the query cache.
	 *
	 * @param c the handler for each iteration
	 * @return {@code this} for the purpose of chaining
	 */
	public T each(final ChainingIterator<K> c) {
		return each(c, false);
	}

	/**
	 * Enumerates through each item in the query cache with an option for non-blocking execution.
	 *
	 * @param c     the handler for each iteration
	 * @param async {@code true} to iterate asynchronously, otherwise {@code false} for blocking execution
	 * @return {@code this} for the purpose of chaining
	 */
	public T each(final ChainingIterator<K> c, final boolean async) {
		validate();

		final Runnable r = new Runnable() {
			@Override
			public void run() {
				int i = 0;
				for (final K k : AbstractQuery.this) {
					if (!c.next(i++, k)) {
						break;
					}
				}
			}
		};

		if (async) {
			new Thread(r).start();
		} else {
			r.run();
		}

		return getThis();
	}

	/**
	 * Marks the cache as stale forcing the next query to load a fresh data set.
	 *
	 * @return {@code this} for the purpose of chaining
	 * @see {@link #select()} ()}
	 */
	public T invalidate() {
		mark.set(true);
		return getThis();
	}

	private void validate() {
		if (mark.compareAndSet(true, false)) {
			select();
		}
	}

	/**
	 * Returns {@code true} if the query cache contains no items.
	 *
	 * @return {@code true} if the query cache contains no items
	 */
	public boolean isEmpty() {
		return items.get().isEmpty();
	}

	/**
	 * Returns {@code true} if the query cache contains the specified item.
	 *
	 * @param k item whose presence in this query cache is to be tested
	 * @return {@code true} if the query cache contains the specified item
	 */
	public boolean contains(final K k) {
		return items.get().contains(k);
	}

	/**
	 * Returns the number of items in the query cache.
	 *
	 * @return the number of items in the query cache
	 */
	public int size() {
		return items.get().size();
	}
}
