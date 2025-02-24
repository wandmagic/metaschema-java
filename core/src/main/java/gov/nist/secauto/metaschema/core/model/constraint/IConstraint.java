/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.model.constraint;

import gov.nist.secauto.metaschema.core.datatype.markup.MarkupMultiline;
import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.MetapathException;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.node.IDefinitionNodeItem;
import gov.nist.secauto.metaschema.core.model.IAttributable;
import gov.nist.secauto.metaschema.core.model.IDescribable;
import gov.nist.secauto.metaschema.core.model.ISource;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Represents a rule constraining the model of a Metaschema assembly, field or
 * flag. Provides a common interface for all constraint definitions.
 */
public interface IConstraint extends IAttributable, IDescribable {
  /**
   * The type of constraint.
   */
  enum Type {
    ALLOWED_VALUES("allowed-values"),
    CARDINALITY("cardinality"),
    EXPECT("expect"),
    INDEX("index"),
    UNIQUE("unique"),
    INDEX_HAS_KEY("index-has-key"),
    MATCHES("matches");

    @NonNull
    private final String name;

    Type(@NonNull String name) {
      this.name = name;
    }

    @NonNull
    public String getName() {
      return name;
    }
  }

  /**
   * The degree to which a constraint violation is significant.
   * <p>
   * These values are ordered from least significant to most significant.
   */
  enum Level {
    /**
     * No violation.
     */
    NONE,
    /**
     * A violation of the constraint represents a point of interest.
     */
    INFORMATIONAL,
    /**
     * A violation of the constraint represents a fault in the content that may
     * warrant review by a developer when performing model or tool development.
     */
    DEBUG,
    /**
     * A violation of the constraint represents a potential issue with the content.
     */
    WARNING,
    /**
     * A violation of the constraint represents a fault in the content. This may
     * include issues around compatibility, integrity, consistency, etc.
     */
    ERROR,
    /**
     * A violation of the constraint represents a serious fault in the content that
     * will prevent typical use of the content.
     */
    CRITICAL;
  }

  /**
   * The default level to use if no level is provided.
   */
  @NonNull
  Level DEFAULT_LEVEL = Level.ERROR;

  /**
   * The compiled default target Metapath to use if no target is provided.
   */
  @NonNull
  IMetapathExpression DEFAULT_TARGET_METAPATH = IMetapathExpression.contextNode();

  /**
   * Get a string that identifies the provided constraint using the most specific
   * information available.
   *
   * @param constraint
   *          the constraint to identify
   * @return the constraint identification statement
   */
  @NonNull
  static String getConstraintIdentity(@NonNull IConstraint constraint) {
    String identity;
    if (constraint.getId() != null) {
      identity = String.format("with id '%s'", constraint.getId());
    } else if (constraint.getFormalName() != null) {
      identity = String.format("with the formal name '%s'", constraint.getFormalName());
    } else {
      identity = String.format("targeting '%s'", constraint.getTarget().getPath());
    }
    return ObjectUtils.notNull(identity);
  }

  /**
   * Get the constraint type.
   *
   * @return the constraint type
   */
  @NonNull
  Type getType();

  /**
   * Retrieve the unique identifier for the constraint.
   *
   * @return the identifier or {@code null} if no identifier is defined
   */
  @Nullable
  String getId();

  /**
   * Get information about the source of the constraint.
   *
   * @return the source information
   */
  @NonNull
  ISource getSource();

  /**
   * The significance of a violation of this constraint.
   *
   * @return the level
   */
  @NonNull
  Level getLevel();

  /**
   * Retrieve the Metapath expression to use to query the targets of the
   * constraint.
   *
   * @return a Metapath expression
   */
  @NonNull
  IMetapathExpression getTarget();

  /**
   * Based on the provided {@code contextNodeItem}, find all nodes matching the
   * target expression.
   *
   * @param item
   *          the node item to evaluate the target expression against
   * @param dynamicContext
   *          the Metapath evaluation context to use
   * @return the matching nodes as a sequence
   * @throws MetapathException
   *           if an error occurred during evaluation
   * @see #getTarget()
   */
  @NonNull
  ISequence<? extends IDefinitionNodeItem<?, ?>> matchTargets(
      @NonNull IDefinitionNodeItem<?, ?> item,
      @NonNull DynamicContext dynamicContext);

  /**
   * Retrieve the remarks associated with the constraint.
   *
   * @return the remarks or {@code null} if no remarks are defined
   */
  MarkupMultiline getRemarks();

  /**
   * Used for double dispatch supporting the visitor pattern provided by
   * implementations of {@link IConstraintVisitor}.
   *
   * @param <T>
   *          the Java type of a state object passed to the visitor
   * @param <R>
   *          the Java type of the result returned by the visitor methods
   * @param visitor
   *          the visitor implementation
   * @param state
   *          the state object passed to the visitor
   * @return the visitation result
   * @see IConstraintVisitor
   */
  <T, R> R accept(@NonNull IConstraintVisitor<T, R> visitor, T state);
}
