/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst.items;

import gov.nist.secauto.metaschema.core.metapath.cst.IExpressionVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IDecimalItem;

import java.math.BigDecimal;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An implementation of the
 * <a href="https://www.w3.org/TR/xpath-31/#id-literals">Decimal Literal
 * Expression</a> supporting the creation of a Metapath constant literal
 * {@link IDecimalItem}.
 */
public class DecimalLiteral
    extends AbstractLiteralExpression<IDecimalItem> {

  /**
   * Construct a new expression that always returns the same decimal value.
   *
   * @param text
   *          the parsed text of the expression
   * @param value
   *          the literal value
   */
  public DecimalLiteral(@NonNull String text, @NonNull BigDecimal value) {
    super(text, IDecimalItem.valueOf(value));
  }

  @Override
  public Class<IDecimalItem> getBaseResultType() {
    return IDecimalItem.class;
  }

  @Override
  public <RESULT, CONTEXT> RESULT accept(IExpressionVisitor<RESULT, CONTEXT> visitor, CONTEXT context) {
    return visitor.visitDecimalLiteral(this, context);
  }
}
