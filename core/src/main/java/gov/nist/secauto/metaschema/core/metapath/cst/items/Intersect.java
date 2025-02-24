/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst.items;

import gov.nist.secauto.metaschema.core.metapath.IExpression;
import gov.nist.secauto.metaschema.core.metapath.cst.IExpressionVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The CST node for a Metapath
 * <a href="https://www.w3.org/TR/xpath-31/#combining_seq">intersect
 * expression</a>.
 */
public class Intersect
    extends AbstractFilterExpression {

  /**
   * Construct a new Metapath intersect expression CST node.
   *
   * @param text
   *          the parsed text of the expression
   * @param left
   *          an expression indicating the items to filter
   * @param right
   *          an expression indicating the items to keep
   */
  public Intersect(
      @NonNull String text,
      @NonNull IExpression left,
      @NonNull IExpression right) {
    super(text, left, right);
  }

  @Override
  protected ISequence<?> applyFilterTo(@NonNull List<? extends IItem> source, @NonNull List<? extends IItem> items) {
    return ISequence.of(ObjectUtils.notNull(source.stream()
        .distinct()
        .filter(items::contains)));
  }

  @Override
  public <RESULT, CONTEXT> RESULT accept(@NonNull IExpressionVisitor<RESULT, CONTEXT> visitor, CONTEXT context) {
    return visitor.visitIntersect(this, context);
  }
}
