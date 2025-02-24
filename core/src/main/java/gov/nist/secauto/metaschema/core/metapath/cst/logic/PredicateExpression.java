/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst.logic;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.IExpression;
import gov.nist.secauto.metaschema.core.metapath.MetapathEvaluationFeature;
import gov.nist.secauto.metaschema.core.metapath.cst.AbstractExpression;
import gov.nist.secauto.metaschema.core.metapath.cst.IExpressionVisitor;
import gov.nist.secauto.metaschema.core.metapath.cst.items.IntegerLiteral;
import gov.nist.secauto.metaschema.core.metapath.function.library.FnBoolean;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An XPath 3.1
 * <a href="https://www.w3.org/TR/xpath-31/#id-filter-expression">filter
 * expression</a> or a
 * <a href="https://www.w3.org/TR/xpath-31/#id-predicate">predicate
 * expression</a> in the case where the previous expression is an
 * <a href="https://www.w3.org/TR/xpath-31/#doc-xpath31-AxisStep">axis step</a>
 * expression.
 */
public class PredicateExpression
    extends AbstractExpression {
  @NonNull
  private final IExpression base;
  @NonNull
  private final List<IExpression> predicates;

  /**
   * Construct a new predicate expression.
   *
   * @param text
   *          the parsed text of the expression
   * @param base
   *          the base to evaluate against
   * @param predicates
   *          the expression(s) to apply as a filter
   */
  public PredicateExpression(
      @NonNull String text,
      @NonNull IExpression base,
      @NonNull List<IExpression> predicates) {
    super(text);
    this.base = base;
    this.predicates = predicates;
  }

  /**
   * Get the base sub-expression.
   *
   * @return the sub-expression
   */
  @NonNull
  public IExpression getBase() {
    return base;
  }

  /**
   * Retrieve the list of predicates to filter with.
   *
   * @return the list of predicates
   */
  @NonNull
  public List<IExpression> getPredicates() {
    return predicates;
  }

  @Override
  public List<? extends IExpression> getChildren() {
    return ObjectUtils.notNull(
        Stream.concat(Stream.of(getBase()), getPredicates().stream()).collect(Collectors.toList()));
  }

  @Override
  protected ISequence<?> evaluate(@NonNull DynamicContext dynamicContext,
      @NonNull ISequence<?> focus) {

    ISequence<?> retval = getBase().accept(dynamicContext, focus);

    if (dynamicContext.getConfiguration().isFeatureEnabled(MetapathEvaluationFeature.METAPATH_EVALUATE_PREDICATES)) {
      // evaluate the predicates for this step
      AtomicInteger index = new AtomicInteger();

      Stream<? extends IItem> stream = ObjectUtils.notNull(
          retval.stream().map(item -> Map.entry(BigInteger.valueOf(index.incrementAndGet()), item)).filter(entry -> {
            @SuppressWarnings("null")
            @NonNull
            IItem item = entry.getValue();

            // return false if any predicate evaluates to false
            return !predicates.stream()
                .map(predicateExpr -> {
                  boolean bool;
                  if (predicateExpr instanceof IntegerLiteral) {
                    // reduce the result to the matching item
                    BigInteger predicateIndex = ((IntegerLiteral) predicateExpr).getValue().asInteger();

                    // get the position of the item
                    final BigInteger position = entry.getKey();

                    // it is a match if the position matches
                    bool = position.equals(predicateIndex);
                  } else {
                    ISequence<?> innerFocus = ISequence.of(item);
                    ISequence<?> predicateResult = predicateExpr.accept(dynamicContext, innerFocus);
                    bool = FnBoolean.fnBoolean(predicateResult).toBoolean();
                  }
                  return bool;
                }).anyMatch(x -> !x);
          }).map(Entry::getValue));

      retval = ISequence.of(stream);
    }
    return retval;
  }

  @Override
  public <RESULT, CONTEXT> RESULT accept(@NonNull IExpressionVisitor<RESULT, CONTEXT> visitor, CONTEXT context) {
    return visitor.visitPredicate(this, context);
  }

}
