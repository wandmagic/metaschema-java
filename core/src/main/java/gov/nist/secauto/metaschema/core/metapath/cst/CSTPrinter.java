/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst;

import gov.nist.secauto.metaschema.core.metapath.IExpression;
import gov.nist.secauto.metaschema.core.metapath.cst.items.ArraySequenceConstructor;
import gov.nist.secauto.metaschema.core.metapath.cst.items.ArraySquareConstructor;
import gov.nist.secauto.metaschema.core.metapath.cst.items.DecimalLiteral;
import gov.nist.secauto.metaschema.core.metapath.cst.items.EmptySequence;
import gov.nist.secauto.metaschema.core.metapath.cst.items.Except;
import gov.nist.secauto.metaschema.core.metapath.cst.items.IntegerLiteral;
import gov.nist.secauto.metaschema.core.metapath.cst.items.Intersect;
import gov.nist.secauto.metaschema.core.metapath.cst.items.MapConstructor;
import gov.nist.secauto.metaschema.core.metapath.cst.items.PostfixLookup;
import gov.nist.secauto.metaschema.core.metapath.cst.items.Quantified;
import gov.nist.secauto.metaschema.core.metapath.cst.items.Range;
import gov.nist.secauto.metaschema.core.metapath.cst.items.SequenceExpression;
import gov.nist.secauto.metaschema.core.metapath.cst.items.SimpleMap;
import gov.nist.secauto.metaschema.core.metapath.cst.items.StringConcat;
import gov.nist.secauto.metaschema.core.metapath.cst.items.StringLiteral;
import gov.nist.secauto.metaschema.core.metapath.cst.items.UnaryLookup;
import gov.nist.secauto.metaschema.core.metapath.cst.items.Union;
import gov.nist.secauto.metaschema.core.metapath.cst.logic.And;
import gov.nist.secauto.metaschema.core.metapath.cst.logic.GeneralComparison;
import gov.nist.secauto.metaschema.core.metapath.cst.logic.If;
import gov.nist.secauto.metaschema.core.metapath.cst.logic.Or;
import gov.nist.secauto.metaschema.core.metapath.cst.logic.PredicateExpression;
import gov.nist.secauto.metaschema.core.metapath.cst.logic.ValueComparison;
import gov.nist.secauto.metaschema.core.metapath.cst.math.Addition;
import gov.nist.secauto.metaschema.core.metapath.cst.math.Division;
import gov.nist.secauto.metaschema.core.metapath.cst.math.IntegerDivision;
import gov.nist.secauto.metaschema.core.metapath.cst.math.Modulo;
import gov.nist.secauto.metaschema.core.metapath.cst.math.Multiplication;
import gov.nist.secauto.metaschema.core.metapath.cst.math.Negate;
import gov.nist.secauto.metaschema.core.metapath.cst.math.Subtraction;
import gov.nist.secauto.metaschema.core.metapath.cst.path.ContextItem;
import gov.nist.secauto.metaschema.core.metapath.cst.path.FlagStep;
import gov.nist.secauto.metaschema.core.metapath.cst.path.KindNodeTest;
import gov.nist.secauto.metaschema.core.metapath.cst.path.ModelInstanceStep;
import gov.nist.secauto.metaschema.core.metapath.cst.path.NameNodeTest;
import gov.nist.secauto.metaschema.core.metapath.cst.path.RelativeDoubleSlashPath;
import gov.nist.secauto.metaschema.core.metapath.cst.path.RelativeSlashPath;
import gov.nist.secauto.metaschema.core.metapath.cst.path.RootDoubleSlashPath;
import gov.nist.secauto.metaschema.core.metapath.cst.path.RootSlashOnlyPath;
import gov.nist.secauto.metaschema.core.metapath.cst.path.RootSlashPath;
import gov.nist.secauto.metaschema.core.metapath.cst.path.Step;
import gov.nist.secauto.metaschema.core.metapath.cst.path.WildcardNodeTest;
import gov.nist.secauto.metaschema.core.metapath.cst.type.Cast;
import gov.nist.secauto.metaschema.core.metapath.cst.type.Castable;
import gov.nist.secauto.metaschema.core.metapath.cst.type.InstanceOf;
import gov.nist.secauto.metaschema.core.metapath.cst.type.Treat;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Supports the generation of a human-readable representation of a Metapath
 * compact syntax tree (CST).
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class CSTPrinter {
  private CSTPrinter() {
    // disable construction
  }

  /**
   * Generate a string representation of the CST tree.
   *
   * @param expr
   *          an expression that is a branch in the tree to visualize.
   * @return a string representation of the CST graph
   */
  public static String toString(@NonNull IExpression expr) {
    return new CSTPrinterVisitor().visit(expr);
  }

  @SuppressWarnings("PMD.ExcessivePublicCount")
  private static final class CSTPrinterVisitor
      extends AbstractExpressionVisitor<String, State> {

    @Override
    protected String visitChildren(IExpression expr, State context) {
      context.push();
      String result = super.visitChildren(expr, context);
      context.pop();
      return result;
    }

    @Override
    protected String aggregateResult(String result, String nextResult, State context) {
      StringBuilder buffer = new StringBuilder();
      if (result != null) {
        buffer.append(result);
        // buffer.append(" ar "+System.lineSeparator());
      }

      buffer.append(context.getIndentation())
          .append(nextResult);
      return buffer.toString();
    }

    @Override
    protected String defaultResult() {
      return "";
    }

    /**
     * Append the {@code childResult} to the record produced for the current node.
     *
     * @param expr
     *          the current node
     * @param childResult
     *          the output generated for the curren't node's children
     * @param context
     *          the output context state
     * @return the string representation of the node tree for the current node and
     *         its children
     */
    @SuppressWarnings("static-method")
    protected String appendNode(
        @NonNull IExpression expr,
        @Nullable String childResult,
        @NonNull State context) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(context.getIndentation())
          .append(expr.toCSTString());
      if (childResult != null) {
        buffer.append(System.lineSeparator())
            .append(childResult);
      }
      return buffer.toString();
    }

    /**
     * Visit a node and produce a string representation of its the node tree.
     *
     * @param expression
     *          the node to build the node tree for
     * @return the string representation of the node tree for the provided
     *         expression node and its children
     */
    public String visit(@NonNull IExpression expression) {
      return visit(expression, new State());
    }

    @Override
    public String visitAddition(Addition expr, State context) {
      return appendNode(expr, super.visitAddition(expr, context), context);
    }

    @Override
    public String visitAnd(And expr, State context) {
      return appendNode(expr, super.visitAnd(expr, context), context);
    }

    @Override
    public String visitStep(Step expr, State context) {
      return appendNode(expr, super.visitStep(expr, context), context);
    }

    @Override
    public String visitValueComparison(ValueComparison expr, State context) {
      return appendNode(expr, super.visitValueComparison(expr, context), context);
    }

    @Override
    public String visitGeneralComparison(GeneralComparison expr, State context) {
      return appendNode(expr, super.visitGeneralComparison(expr, context), context);
    }

    @Override
    public String visitContextItem(ContextItem expr, State context) {
      return appendNode(expr, super.visitContextItem(expr, context), context);
    }

    @Override
    public String visitDecimalLiteral(DecimalLiteral expr, State context) {
      return appendNode(expr, super.visitDecimalLiteral(expr, context), context);
    }

    @Override
    public String visitDivision(Division expr, State context) {
      return appendNode(expr, super.visitDivision(expr, context), context);
    }

    @Override
    public String visitExcept(@NonNull Except expr, State context) {
      return appendNode(expr, super.visitExcept(expr, context), context);
    }

    @Override
    public String visitFlagStep(FlagStep expr, State context) {
      return appendNode(expr, super.visitFlagStep(expr, context), context);
    }

    @Override
    public String visitStaticFunctionCall(StaticFunctionCall expr, State context) {
      return appendNode(expr, super.visitStaticFunctionCall(expr, context), context);
    }

    @Override
    public String visitDynamicFunctionCall(DynamicFunctionCall expr, State context) {
      return appendNode(expr, super.visitDynamicFunctionCall(expr, context), context);
    }

    @Override
    public String visitAnonymousFunctionCall(AnonymousFunctionCall expr, State context) {
      return appendNode(expr, super.visitAnonymousFunctionCall(expr, context), context);
    }

    @Override
    public String visitIntegerDivision(IntegerDivision expr, State context) {
      return appendNode(expr, super.visitIntegerDivision(expr, context), context);
    }

    @Override
    public String visitIntegerLiteral(IntegerLiteral expr, State context) {
      return appendNode(expr, super.visitIntegerLiteral(expr, context), context);
    }

    @Override
    public String visitIntersect(Intersect expr, State context) {
      return appendNode(expr, super.visitIntersect(expr, context), context);
    }

    @Override
    public String visitMetapath(SequenceExpression expr, State context) {
      return appendNode(expr, super.visitMetapath(expr, context), context);
    }

    @Override
    public String visitModulo(Modulo expr, State context) {
      return appendNode(expr, super.visitModulo(expr, context), context);
    }

    @Override
    public String visitModelInstanceStep(ModelInstanceStep expr, State context) {
      return appendNode(expr, super.visitModelInstanceStep(expr, context), context);
    }

    @Override
    public String visitMultiplication(Multiplication expr, State context) {
      return appendNode(expr, super.visitMultiplication(expr, context), context);
    }

    @Override
    public String visitNameNodeTest(NameNodeTest expr, State context) {
      return appendNode(expr, super.visitNameNodeTest(expr, context), context);
    }

    @Override
    public String visitNegate(Negate expr, State context) {
      return appendNode(expr, super.visitNegate(expr, context), context);
    }

    @Override
    public String visitOr(Or expr, State context) {
      return appendNode(expr, super.visitOr(expr, context), context);
    }

    @Override
    public String visitPredicate(PredicateExpression expr, State context) {
      return appendNode(expr, super.visitPredicate(expr, context), context);
    }

    @Override
    public String visitRelativeDoubleSlashPath(RelativeDoubleSlashPath expr, State context) {
      return appendNode(expr, super.visitRelativeDoubleSlashPath(expr, context), context);
    }

    @Override
    public String visitRelativeSlashPath(RelativeSlashPath expr, State context) {
      return appendNode(expr, super.visitRelativeSlashPath(expr, context), context);
    }

    @Override
    public String visitRootDoubleSlashPath(RootDoubleSlashPath expr, State context) {
      return appendNode(expr, super.visitRootDoubleSlashPath(expr, context), context);
    }

    @Override
    public String visitRootSlashOnlyPath(RootSlashOnlyPath expr, State context) {
      return appendNode(expr, super.visitRootSlashOnlyPath(expr, context), context);
    }

    @Override
    public String visitRootSlashPath(RootSlashPath expr, State context) {
      return appendNode(expr, super.visitRootSlashPath(expr, context), context);
    }

    @Override
    public String visitStringConcat(StringConcat expr, State context) {
      return appendNode(expr, super.visitStringConcat(expr, context), context);
    }

    @Override
    public String visitStringLiteral(StringLiteral expr, State context) {
      return appendNode(expr, super.visitStringLiteral(expr, context), context);
    }

    @Override
    public String visitSubtraction(Subtraction expr, State context) {
      return appendNode(expr, super.visitSubtraction(expr, context), context);
    }

    @Override
    public String visitUnion(Union expr, State context) {
      return appendNode(expr, super.visitUnion(expr, context), context);
    }

    @Override
    public String visitWildcardNodeTest(WildcardNodeTest expr, State context) {
      return appendNode(expr, super.visitWildcardNodeTest(expr, context), context);
    }

    @Override
    public String visitLet(Let expr, State context) {
      return appendNode(expr, super.visitLet(expr, context), context);
    }

    @Override
    public String visitNamedFunctionReference(NamedFunctionReference expr, State context) {
      return appendNode(expr, super.visitNamedFunctionReference(expr, context), context);
    }

    @Override
    public String visitVariableReference(VariableReference expr, State context) {
      return appendNode(expr, super.visitVariableReference(expr, context), context);
    }

    @Override
    public String visitEmptySequence(EmptySequence<?> expr, State context) {
      return appendNode(expr, super.visitEmptySequence(expr, context), context);
    }

    @Override
    public String visitRange(Range expr, State context) {
      return appendNode(expr, super.visitRange(expr, context), context);
    }

    @Override
    public String visitIf(If expr, State context) {
      return appendNode(expr, super.visitIf(expr, context), context);
    }

    @Override
    public String visitQuantified(Quantified expr, State context) {
      return appendNode(expr, super.visitQuantified(expr, context), context);
    }

    @Override
    public String visitFor(For expr, State context) {
      return appendNode(expr, super.visitFor(expr, context), context);
    }

    @Override
    public String visitSimpleMap(SimpleMap expr, State context) {
      return appendNode(expr, super.visitSimpleMap(expr, context), context);
    }

    @Override
    public String visitArray(ArraySequenceConstructor expr, State context) {
      return appendNode(expr, super.visitArray(expr, context), context);
    }

    @Override
    public String visitArray(ArraySquareConstructor expr, State context) {
      return appendNode(expr, super.visitArray(expr, context), context);
    }

    @Override
    public String visitPostfixLookup(PostfixLookup expr, State context) {
      return appendNode(expr, super.visitPostfixLookup(expr, context), context);
    }

    @Override
    public String visitFunctionCallAccessor(FunctionCallAccessor expr, State context) {
      return appendNode(expr, super.visitFunctionCallAccessor(expr, context), context);
    }

    @Override
    public String visitUnaryLookup(UnaryLookup expr, State context) {
      return appendNode(expr, super.visitUnaryLookup(expr, context), context);
    }

    @Override
    public String visitMapConstructor(MapConstructor expr, State context) {
      return appendNode(expr, super.visitMapConstructor(expr, context), context);
    }

    @Override
    public String visitMapConstructorEntry(MapConstructor.Entry expr, State context) {
      return appendNode(expr, super.visitMapConstructorEntry(expr, context), context);
    }

    @Override
    public String visitInstanceOf(InstanceOf expr, State context) {
      return appendNode(expr, super.visitInstanceOf(expr, context), context);
    }

    @Override
    public String visitCast(Cast expr, State context) {
      return appendNode(expr, super.visitCast(expr, context), context);
    }

    @Override
    public String visitCastable(Castable expr, State context) {
      return appendNode(expr, super.visitCastable(expr, context), context);
    }

    @Override
    public String visitTreat(Treat expr, State context) {
      return appendNode(expr, super.visitTreat(expr, context), context);
    }

    @Override
    public String visitKindNodeTest(KindNodeTest expr, State context) {
      return appendNode(expr, super.visitKindNodeTest(expr, context), context);
    }

  }

  static class State {
    private int indentation; // 0;
    private int lastIndentation; // 0;
    private String indentationPadding = "";

    public String getIndentation() {
      if (indentation != lastIndentation) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < indentation; i++) {
          buffer.append("  ");
        }
        lastIndentation = indentation;
        indentationPadding = buffer.toString();
      }
      return indentationPadding;
    }

    public State push() {
      indentation++;
      return this;
    }

    public State pop() {
      indentation--;
      return this;
    }
  }
}
