/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst.items;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.IExpression;
import gov.nist.secauto.metaschema.core.metapath.cst.AbstractBinaryExpression;
import gov.nist.secauto.metaschema.core.metapath.cst.IExpressionVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IAnyAtomicItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IIntegerItem;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The CST node for a Metapath
 * <a href="https://www.w3.org/TR/xpath-31/#doc-xpath31-RangeExpr">range
 * expression</a>.
 */
public class Range
    extends AbstractBinaryExpression<IExpression, IExpression> {

  /**
   * Construct a new range expression.
   *
   * @param text
   *          the parsed text of the expression
   * @param start
   *          the expressions representing the start of the range
   * @param end
   *          the expressions representing the end of the range
   *
   */
  public Range(
      @NonNull String text,
      @NonNull IExpression start,
      @NonNull IExpression end) {
    super(text, start, end);
  }

  @Override
  public Class<IIntegerItem> getBaseResultType() {
    return IIntegerItem.class;
  }

  @Override
  protected ISequence<?> evaluate(DynamicContext dynamicContext, ISequence<?> focus) {
    IAnyAtomicItem leftItem = ISequence.of(getLeft().accept(dynamicContext, focus).atomize()).getFirstItem(true);
    IAnyAtomicItem rightItem = ISequence.of(getRight().accept(dynamicContext, focus).atomize()).getFirstItem(true);

    IIntegerItem left = leftItem == null ? null : IIntegerItem.cast(leftItem);
    IIntegerItem right = rightItem == null ? null : IIntegerItem.cast(rightItem);

    ISequence<IIntegerItem> retval;
    if (left == null || right == null || left.compareTo(right) > 0) {
      retval = ISequence.empty();
    } else {

      BigInteger min = left.asInteger();
      BigInteger max = right.asInteger();

      List<IIntegerItem> range = new ArrayList<>(max.subtract(min).add(BigInteger.ONE).intValueExact());
      for (BigInteger val = left.asInteger(); val.compareTo(max) <= 0; val = val.add(BigInteger.ONE)) {
        range.add(IIntegerItem.valueOf(val));
      }

      retval = ISequence.ofCollection(range);
    }
    return retval;
  }

  @Override
  public <RESULT, CONTEXT> RESULT accept(IExpressionVisitor<RESULT, CONTEXT> visitor, CONTEXT context) {
    return visitor.visitRange(this, context);
  }
}
