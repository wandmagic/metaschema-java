/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.model.constraint;

import gov.nist.secauto.metaschema.core.model.ISource;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A container of rules constraining the effective model of a Metaschema field
 * or flag data instance.
 */
public class ValueConstraintSet implements IValueConstrained {
  @NonNull
  private final ISource source;
  @SuppressWarnings("PMD.UseConcurrentHashMap") // need ordering
  @NonNull
  private final Map<IEnhancedQName, ILet> lets = new LinkedHashMap<>();
  @NonNull
  private final List<IConstraint> constraints = new LinkedList<>();
  @NonNull
  private final List<IAllowedValuesConstraint> allowedValuesConstraints = new LinkedList<>();
  @NonNull
  private final List<IMatchesConstraint> matchesConstraints = new LinkedList<>();
  @NonNull
  private final List<IIndexHasKeyConstraint> indexHasKeyConstraints = new LinkedList<>();
  @NonNull
  private final List<IExpectConstraint> expectConstraints = new LinkedList<>();
  @NonNull
  private final ReadWriteLock instanceLock = new ReentrantReadWriteLock();

  /**
   * Construct a new constraint set.
   *
   * @param source
   *          information about the resource the constraints were loaded from
   */
  public ValueConstraintSet(@NonNull ISource source) {
    this.source = source;
  }

  @Override
  public ISource getSource() {
    return source;
  }

  /**
   * Get the read-write lock used to manage access to the underlying constraint
   * collections.
   * <p>
   * This can be used by extending classes to control read/write access to
   * constraint data, supporting multi-threaded access.
   *
   * @return the lock
   */
  @NonNull
  protected ReadWriteLock getLock() {
    return instanceLock;
  }

  /**
   * Get the underlying constraint collection.
   * <p>
   * Access to the returned collection is not thread safe. Callers should use
   * {@link #getLock()} to get the read/write lock for managing access.
   *
   * @return the constraint collection
   */
  @NonNull
  protected List<IConstraint> getConstraintsInternal() {
    return constraints;
  }

  @Override
  public Map<IEnhancedQName, ILet> getLetExpressions() {
    return lets;
  }

  @Override
  public ILet addLetExpression(ILet let) {
    return lets.put(let.getName(), let);
  }

  @Override
  public List<IConstraint> getConstraints() {
    Lock readLock = instanceLock.readLock();
    readLock.lock();
    try {
      return CollectionUtil.unmodifiableList(constraints);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public List<IAllowedValuesConstraint> getAllowedValuesConstraints() {
    Lock readLock = instanceLock.readLock();
    readLock.lock();
    try {
      return CollectionUtil.unmodifiableList(allowedValuesConstraints);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public List<IMatchesConstraint> getMatchesConstraints() {
    Lock readLock = instanceLock.readLock();
    readLock.lock();
    try {
      return CollectionUtil.unmodifiableList(matchesConstraints);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public List<IIndexHasKeyConstraint> getIndexHasKeyConstraints() {
    Lock readLock = instanceLock.readLock();
    readLock.lock();
    try {
      return CollectionUtil.unmodifiableList(indexHasKeyConstraints);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public List<IExpectConstraint> getExpectConstraints() {
    Lock readLock = instanceLock.readLock();
    readLock.lock();
    try {
      return CollectionUtil.unmodifiableList(expectConstraints);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public final void addConstraint(@NonNull IAllowedValuesConstraint constraint) {
    Lock writeLock = instanceLock.writeLock();
    writeLock.lock();
    try {
      constraints.add(constraint);
      allowedValuesConstraints.add(constraint);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public final void addConstraint(@NonNull IMatchesConstraint constraint) {
    Lock writeLock = instanceLock.writeLock();
    writeLock.lock();
    try {
      constraints.add(constraint);
      matchesConstraints.add(constraint);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public final void addConstraint(@NonNull IIndexHasKeyConstraint constraint) {
    Lock writeLock = instanceLock.writeLock();
    writeLock.lock();
    try {
      constraints.add(constraint);
      indexHasKeyConstraints.add(constraint);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public final void addConstraint(@NonNull IExpectConstraint constraint) {
    Lock writeLock = instanceLock.writeLock();
    writeLock.lock();
    try {
      constraints.add(constraint);
      expectConstraints.add(constraint);
    } finally {
      writeLock.unlock();
    }
  }
}
