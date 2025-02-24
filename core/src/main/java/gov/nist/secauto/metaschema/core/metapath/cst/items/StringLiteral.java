/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst.items;

import gov.nist.secauto.metaschema.core.metapath.cst.IExpressionVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IStringItem;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An implementation of the
 * <a href="https://www.w3.org/TR/xpath-31/#id-literals">String Literal
 * Expression</a> supporting the creation of a Metapath constant literal
 * {@link IStringItem}.
 */
public class StringLiteral
    extends AbstractLiteralExpression<IStringItem> {
  /**
   * Construct a new expression that always returns the same string value.
   *
   * @param text
   *          the parsed text of the expression
   * @param value
   *          the literal value
   */
  public StringLiteral(@NonNull String text, @NonNull String value) {
    super(text, IStringItem.valueOf(removeQuotes(value)));
  }

  @Override
  public Class<IStringItem> getBaseResultType() {
    return IStringItem.class;
  }

  @SuppressWarnings("null")
  @NonNull
  private static String removeQuotes(@NonNull String value) {
    return value.substring(1, value.length() - 1);
  }

  @Override
  public <RESULT, CONTEXT> RESULT accept(IExpressionVisitor<RESULT, CONTEXT> visitor, CONTEXT context) {
    return visitor.visitStringLiteral(this, context);
  }
}
