/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.model.constraint.impl;

import gov.nist.secauto.metaschema.core.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.core.datatype.markup.MarkupMultiline;
import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.node.IDefinitionNodeItem;
import gov.nist.secauto.metaschema.core.model.IAttributable;
import gov.nist.secauto.metaschema.core.model.ISource;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraint;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The base class for all constraint implementations.
 */
public abstract class AbstractConstraint implements IConstraint { // NOPMD - intentional data class
  @Nullable
  private final String id;
  @Nullable
  private final String formalName;
  @Nullable
  private final MarkupLine description;
  @NonNull
  private final ISource source;
  @NonNull
  private final Level level;
  @Nullable
  private final MarkupMultiline remarks;
  @NonNull
  private final Map<IAttributable.Key, Set<String>> properties;
  @NonNull
  private final IMetapathExpression target;

  /**
   * Construct a new Metaschema constraint.
   *
   * @param id
   *          the optional identifier for the constraint
   * @param formalName
   *          the constraint's formal name or {@code null} if not provided
   * @param description
   *          the constraint's semantic description or {@code null} if not
   *          provided
   * @param source
   *          information about the constraint source
   * @param level
   *          the significance of a violation of this constraint
   * @param target
   *          the Metapath expression identifying the nodes the constraint targets
   * @param properties
   *          a collection of associated properties
   * @param remarks
   *          optional remarks describing the intent of the constraint
   */
  protected AbstractConstraint(
      @Nullable String id,
      @Nullable String formalName,
      @Nullable MarkupLine description,
      @NonNull ISource source,
      @NonNull Level level,
      @NonNull IMetapathExpression target,
      @NonNull Map<IAttributable.Key, Set<String>> properties,
      @Nullable MarkupMultiline remarks) {
    Objects.requireNonNull(target);
    this.id = id;
    this.formalName = formalName;
    this.description = description;
    this.source = source;
    this.level = ObjectUtils.requireNonNull(level, "level");
    this.properties = properties;
    this.remarks = remarks;
    this.target = target;
  }

  @Override
  public final String getId() {
    return id;
  }

  @Override
  public MarkupLine getDescription() {
    return description;
  }

  @Override
  public final String getFormalName() {
    return formalName;
  }

  @Override
  public ISource getSource() {
    return source;
  }

  @Override
  @NonNull
  public Level getLevel() {
    return level;
  }

  @Override
  public final IMetapathExpression getTarget() {
    return target;
  }

  @Override
  public Map<IAttributable.Key, Set<String>> getProperties() {
    return CollectionUtil.unmodifiableMap(properties);
  }

  @Override
  public MarkupMultiline getRemarks() {
    return remarks;
  }

  @Override
  @NonNull
  public ISequence<? extends IDefinitionNodeItem<?, ?>> matchTargets(
      @NonNull IDefinitionNodeItem<?, ?> item,
      @NonNull DynamicContext dynamicContext) {
    return item.hasValue() ? getTarget().evaluate(item, dynamicContext) : ISequence.empty();
  }
}
