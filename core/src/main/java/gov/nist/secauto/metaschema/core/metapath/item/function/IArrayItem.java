/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.item.function;

import gov.nist.secauto.metaschema.core.metapath.function.IFunction;
import gov.nist.secauto.metaschema.core.metapath.impl.AbstractArrayItem;
import gov.nist.secauto.metaschema.core.metapath.impl.ArrayItemN;
import gov.nist.secauto.metaschema.core.metapath.item.ICollectionValue;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.IItemVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IAnyAtomicItem;
import gov.nist.secauto.metaschema.core.metapath.type.IItemType;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A representation of a Metapath array item type.
 * <p>
 * Instances of this interface are required to enforce non-mutability for array
 * contents.
 *
 * @param <ITEM>
 *          the Metapath item type of array members
 */
@SuppressWarnings({ "PMD.ShortMethodName", "PMD.ExcessivePublicCount" })
public interface IArrayItem<ITEM extends ICollectionValue> extends IFunction, List<ITEM> {
  /**
   * Get the type information for this item.
   *
   * @return the type information
   */
  @NonNull
  static IItemType type() {
    return IItemType.array();
  }

  @Override
  default IItemType getType() {
    return type();
  }

  /**
   * Get an empty, immutable array item.
   *
   * @param <T>
   *          the item Java type
   * @return an immutable map item
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> empty() {
    return AbstractArrayItem.empty();
  }

  @Override
  List<ITEM> getValue();

  @Override
  default boolean hasValue() {
    return true;
  }

  @Override
  default ISequence<?> contentsAsSequence() {
    return ISequence.of(ObjectUtils.notNull(stream()
        .flatMap(ICollectionValue::normalizeAsItems)));
  }

  @Override
  default Stream<IAnyAtomicItem> atomize() {
    return ObjectUtils.notNull(stream().flatMap(ICollectionValue::atomize));
  }

  /**
   * Determine if this sequence is empty.
   *
   * @return {@code true} if the sequence contains no items, or {@code false}
   *         otherwise
   */
  @Override
  default boolean isEmpty() {
    return getValue().isEmpty();
  }

  /**
   * Get the count of items in this sequence.
   *
   * @return the count of items
   */
  @Override
  default int size() {
    return getValue().size();

  }

  @Override
  default boolean contains(Object obj) {
    return getValue().contains(obj);
  }

  @Override
  default Object[] toArray() {
    return getValue().toArray();
  }

  @Override
  default <T> T[] toArray(T[] array) {
    return getValue().toArray(array);
  }

  @Override
  default boolean containsAll(Collection<?> collection) {
    return getValue().containsAll(collection);
  }

  @Override
  default ITEM get(int index) {
    return getValue().get(index);
  }

  @Override
  default int indexOf(Object obj) {
    return getValue().indexOf(obj);
  }

  @Override
  default int lastIndexOf(Object obj) {
    return getValue().lastIndexOf(obj);
  }

  @Override
  default ListIterator<ITEM> listIterator() {
    return getValue().listIterator();
  }

  @Override
  default ListIterator<ITEM> listIterator(int index) {
    return getValue().listIterator(index);
  }

  @Override
  default List<ITEM> subList(int fromIndex, int toIndex) {
    return getValue().subList(fromIndex, toIndex);
  }

  /**
   * A {@link Collector} implementation to generates a sequence from a stream of
   * Metapath items.
   *
   * @param <T>
   *          the Java type of the items
   * @return a collector that will generate a sequence
   */
  @NonNull
  static <T extends ICollectionValue> Collector<T, ?, IArrayItem<T>> toArrayItem() {
    return new Collector<T, List<T>, IArrayItem<T>>() {

      @Override
      public Supplier<List<T>> supplier() {
        return ArrayList::new;
      }

      @Override
      public BiConsumer<List<T>, T> accumulator() {
        return List::add;
      }

      @Override
      public BinaryOperator<List<T>> combiner() {
        return (list1, list2) -> {
          list1.addAll(list2);
          return list1;
        };
      }

      @Override
      public Function<List<T>, IArrayItem<T>> finisher() {
        return list -> ofCollection(ObjectUtils.notNull(list));
      }

      @Override
      public Set<Characteristics> characteristics() {
        return Collections.emptySet();
      }
    };
  }

  @Override
  default ISequence<? extends IArrayItem<ITEM>> toSequence() {
    return ISequence.of(this);
  }

  @SuppressWarnings("null")
  @Override
  default Stream<? extends IItem> flatten() {
    return stream()
        .flatMap(ICollectionValue::flatten);
  }

  /**
   * Get a new, immutable array item that contains the items in the provided list.
   *
   * @param <T>
   *          the item Java type
   * @param items
   *          the list whose items are to be added to the new array
   * @return an array item containing the specified entries
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> ofCollection( // NOPMD - intentional
      @NonNull List<T> items) {
    return items.isEmpty() ? empty() : new ArrayItemN<>(items);
  }

  /**
   * Returns an unmodifiable array item containing zero elements.
   *
   * @param <T>
   *          the item type
   * @return an empty {@code IArrayItem}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of() {
    return AbstractArrayItem.empty();
  }

  /**
   * Returns an unmodifiable array item containing one item.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the single item
   * @return an {@code IArrayItem} containing the specified item
   * @throws NullPointerException
   *           if the item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1) {
    return new ArrayItemN<>(e1);
  }

  /**
   * Returns an unmodifiable array item containing two items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2) {
    return new ArrayItemN<>(e1, e2);
  }

  /**
   * Returns an unmodifiable array item containing three elements.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3) {
    return new ArrayItemN<>(e1, e2, e3);
  }

  /**
   * Returns an unmodifiable array item containing four items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @param e4
   *          the fourth item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3, @NonNull T e4) {
    return new ArrayItemN<>(e1, e2, e3, e4);
  }

  /**
   * Returns an unmodifiable array item containing five items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @param e4
   *          the fourth item
   * @param e5
   *          the fifth item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3, @NonNull T e4,
      @NonNull T e5) {
    return new ArrayItemN<>(e1, e2, e3, e4, e5);
  }

  /**
   * Returns an unmodifiable array item containing six items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @param e4
   *          the fourth item
   * @param e5
   *          the fifth item
   * @param e6
   *          the sixth item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3, @NonNull T e4,
      @NonNull T e5, @NonNull T e6) {
    return new ArrayItemN<>(e1, e2, e3, e4, e5, e6);
  }

  /**
   * Returns an unmodifiable array item containing seven items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @param e4
   *          the fourth item
   * @param e5
   *          the fifth item
   * @param e6
   *          the sixth item
   * @param e7
   *          the seventh item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3, @NonNull T e4,
      @NonNull T e5, @NonNull T e6, @NonNull T e7) {
    return new ArrayItemN<>(e1, e2, e3, e4, e5, e6, e7);
  }

  /**
   * Returns an unmodifiable array item containing eight items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @param e4
   *          the fourth item
   * @param e5
   *          the fifth item
   * @param e6
   *          the sixth item
   * @param e7
   *          the seventh item
   * @param e8
   *          the eighth item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3, @NonNull T e4,
      @NonNull T e5, @NonNull T e6, @NonNull T e7, @NonNull T e8) {
    return new ArrayItemN<>(e1, e2, e3, e4, e5, e6, e7, e8);
  }

  /**
   * Returns an unmodifiable array item containing nine items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @param e4
   *          the fourth item
   * @param e5
   *          the fifth item
   * @param e6
   *          the sixth item
   * @param e7
   *          the seventh item
   * @param e8
   *          the eighth item
   * @param e9
   *          the ninth item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3, @NonNull T e4,
      @NonNull T e5, @NonNull T e6, @NonNull T e7, @NonNull T e8, @NonNull T e9) {
    return new ArrayItemN<>(e1, e2, e3, e4, e5, e6, e7, e8, e9);
  }

  /**
   * Returns an unmodifiable array item containing ten items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param e1
   *          the first item
   * @param e2
   *          the second item
   * @param e3
   *          the third item
   * @param e4
   *          the fourth item
   * @param e5
   *          the fifth item
   * @param e6
   *          the sixth item
   * @param e7
   *          the seventh item
   * @param e8
   *          the eighth item
   * @param e9
   *          the ninth item
   * @param e10
   *          the tenth item
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null}
   */
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T e1, @NonNull T e2, @NonNull T e3, @NonNull T e4,
      @NonNull T e5, @NonNull T e6, @NonNull T e7, @NonNull T e8, @NonNull T e9, @NonNull T e10) {
    return new ArrayItemN<>(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
  }

  /**
   * Returns an unmodifiable array item containing an arbitrary number of items.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param items
   *          the items to be contained in the list
   * @return an {@code IArrayItem} containing the specified items
   * @throws NullPointerException
   *           if an item is {@code null} or if the array is {@code null}
   */
  @SafeVarargs
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> of(@NonNull T... items) {
    return items.length == 0 ? empty() : new ArrayItemN<>(items);
  }

  /**
   * Returns an unmodifiable array item containing the items of the given
   * Collection, in its iteration order. The given Collection must not be null,
   * and it must not contain any null items. If the given Collection is
   * subsequently modified, the returned array item will not reflect such
   * modifications.
   *
   * @param <T>
   *          the {@code IArrayItem}'s item type
   * @param collection
   *          a {@code Collection} from which items are drawn, must be non-null
   * @return an {@code IArrayItem} containing the items of the given
   *         {@code Collection}
   * @throws NullPointerException
   *           if collection is null, or if it contains any nulls
   */
  @SuppressWarnings("unchecked")
  @NonNull
  static <T extends ICollectionValue> IArrayItem<T> copyOf(@NonNull Collection<? extends T> collection) {
    return collection instanceof IArrayItem
        ? (IArrayItem<T>) collection
        : collection.isEmpty()
            ? empty()
            : new ArrayItemN<>(new ArrayList<>(collection));
  }

  @Override
  default void accept(IItemVisitor visitor) {
    visitor.visit(this);
  }
}
