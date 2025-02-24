/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.impl;

import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A Metapath sequence supporting an unbounded number of items backed initially
 * by a stream.
 *
 * @param <ITEM>
 *          the Java type of the items
 */
public class StreamSequence<ITEM extends IItem>
    extends AbstractSequence<ITEM> {

  private Stream<ITEM> stream;
  private List<ITEM> list;
  @NonNull
  private final Lock instanceLock = new ReentrantLock();

  /**
   * Construct a new sequence using the provided item stream.
   *
   * @param stream
   *          the items to add to the sequence
   */
  public StreamSequence(@NonNull Stream<ITEM> stream) {
    Objects.requireNonNull(stream, "stream");
    this.stream = stream;
  }

  @SuppressWarnings("PMD.NullAssignment")
  @Override
  protected List<ITEM> asList() {
    instanceLock.lock();
    try {
      if (list == null) {
        if (stream == null) {
          throw new IllegalStateException(
              "Unable to collect items into a list because the stream was already consumed.");
        }
        list = stream.collect(Collectors.toUnmodifiableList());
        stream = null;
      }
    } finally {
      instanceLock.unlock();
    }
    assert list != null;
    return list;
  }

  @Override
  public ISequence<ITEM> reusable() {
    // force the stream to be backed by a list
    asList();
    return this;
  }

  @Override
  public Stream<ITEM> stream() {
    @NonNull
    Stream<ITEM> retval;
    // Ensure thread safety and prevent multiple consumptions of the stream
    instanceLock.lock();
    try {
      if (list == null) {
        if (stream == null) {
          throw new IllegalStateException("The stream is already consumed.");
        }
        assert stream != null;
        retval = stream;
        stream = null; // NOPMD - readability
      } else {
        retval = ObjectUtils.notNull(list.stream());
      }
    } finally {
      instanceLock.unlock();
    }
    return retval;
  }
}
