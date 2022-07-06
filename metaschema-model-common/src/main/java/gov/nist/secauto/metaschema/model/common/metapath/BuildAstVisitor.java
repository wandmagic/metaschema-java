/*
 * Portions of this software was developed by employees of the National Institute
 * of Standards and Technology (NIST), an agency of the Federal Government and is
 * being made available as a public service. Pursuant to title 17 United States
 * Code Section 105, works of NIST employees are not subject to copyright
 * protection in the United States. This software may be subject to foreign
 * copyright. Permission in the United States and in foreign countries, to the
 * extent that NIST may hold copyright, to use, copy, modify, create derivative
 * works, and distribute this software and its documentation without fee is hereby
 * granted on a non-exclusive basis, provided that this notice and disclaimer
 * of warranty appears in all copies.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER
 * EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
 * THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND FREEDOM FROM
 * INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL CONFORM TO THE
 * SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE ERROR FREE.  IN NO EVENT
 * SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT,
 * INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM,
 * OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY,
 * CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR
 * PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT
 * OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */

package gov.nist.secauto.metaschema.model.common.metapath;

import gov.nist.secauto.metaschema.model.common.metapath.IComparison.Operator;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Lexer;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.AbbrevforwardstepContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.AbbrevreversestepContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.AdditiveexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.AndexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ArgumentlistContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ArrowexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.AxisstepContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ComparisonexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ContextitemexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.EqnameContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ExprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ForwardstepContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.FunctioncallContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.GeneralcompContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.IntersectexceptexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.LiteralContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.MultiplicativeexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.NumericliteralContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.OrexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.PathexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.PostfixexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.PredicateContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.RelativepathexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ReversestepContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.StringconcatexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.UnaryexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.UnionexprContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.ValuecompContext;
import gov.nist.secauto.metaschema.model.common.metapath.antlr.metapath10Parser.WildcardContext;
import gov.nist.secauto.metaschema.model.common.util.CollectionUtil;
import gov.nist.secauto.metaschema.model.common.util.ObjectUtils;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BuildAstVisitor
    extends AbstractAstVisitor<IExpression> {

  @SuppressWarnings("null")
  @Override
  @NotNull
  public IExpression visit(ParseTree tree) {
    return super.visit(tree);
  }

  // TODO: verify javadocs are accurate for the following n-ary functions.

  /**
   * Parse the provided context as a simple n-ary phrase, which will be one of the following.
   * <ol>
   * <li><code>expr</code> for which the expr will be returned</li>
   * <li><code>left (operator right)*</code> for which a collection of the left and right members will
   * be returned based on what is provided by the supplier.
   * </ol>
   * 
   * @param <CONTEXT>
   *          the context type to parse
   * @param <NODE>
   *          the type of expression
   * @param context
   *          the context instance
   * @param supplier
   *          a supplier that will instantiate an expression based on the provided collection
   * @return the left expression or the supplied expression for a collection
   */
  @NotNull
  protected <CONTEXT extends ParserRuleContext, NODE extends IExpression> IExpression
      handleNAiryCollection(@NotNull CONTEXT context,
          @NotNull Function<@NotNull List<NODE>, IExpression> supplier) {
    return handleNAiryCollection(context, 2, (ctx, idx) -> {
      // skip operator, since we know what it is
      ParseTree tree = ctx.getChild(idx + 1);
      @SuppressWarnings({ "unchecked", "null" })
      @NotNull
      NODE node = (NODE) tree.accept(this);
      return node;
    }, supplier);
  }

  /**
   * Parse the provided context as a simple n-ary phrase, which will be one of the following.
   * <ol>
   * <li><code>expr</code> for which the expr will be returned</li>
   * <li><code>left (operator right)*</code> for which a collection of the left and right members will
   * be returned based on what is provided by the supplier.
   * </ol>
   * 
   * @param <CONTEXT>
   *          the context type to parse
   * @param <EXPRESSION>
   *          the child expression type
   * @param context
   *          the context instance
   * @param step
   *          the amount to advance the loop over the context children
   * @param parser
   *          a binary function used to parse the context children
   * @param supplier
   *          a supplier that will instantiate an expression based on the provided collection
   * @return the left expression or the supplied expression for a collection
   */
  @NotNull
  protected <CONTEXT extends ParserRuleContext, EXPRESSION extends IExpression> IExpression handleNAiryCollection(
      @NotNull CONTEXT context, int step,
      @NotNull BiFunction<@NotNull CONTEXT, @NotNull Integer, @NotNull EXPRESSION> parser,
      @NotNull Function<@NotNull List<EXPRESSION>, IExpression> supplier) {
    int numChildren = context.getChildCount();

    if (numChildren == 0) {
      throw new IllegalStateException("there should always be a child expression");
    }

    ParseTree leftTree = context.getChild(0);
    @SuppressWarnings({ "unchecked", "null" })
    @NotNull
    EXPRESSION leftResult = (EXPRESSION) leftTree.accept(this);

    IExpression retval;
    if (numChildren == 1) {
      retval = leftResult;
    } else {
      List<EXPRESSION> children = new ArrayList<>(numChildren - 1 / step);
      children.add(leftResult);
      for (int i = 1; i < numChildren; i = i + step) {
        @SuppressWarnings("null")
        EXPRESSION result = parser.apply(context, i);
        children.add(result);
      }
      IExpression result = ObjectUtils.notNull(supplier.apply(children));
      retval = result;
    }
    return retval;
  }

  /**
   * Parse the provided context as a simple n-ary phrase, which will be one of the following.
   * <ol>
   * <li><code>expr</code> for which the expr will be returned</li>
   * <li><code>left (operator right)*</code> for which a collection of the left and right members will
   * be returned based on what is provided by the supplier.
   * </ol>
   * 
   * @param <CONTEXT>
   *          the context type to parse
   * @param context
   *          the context instance
   * @param step
   *          the amount to advance the loop over the context children
   * @param parser
   *          a trinary function used to parse the context children and supply a result
   * @return the left expression or the supplied expression
   */
  protected <CONTEXT extends ParserRuleContext> IExpression handleGroupedNAiry(@NotNull CONTEXT context, int step,
      @NotNull ITriFunction<@NotNull CONTEXT, @NotNull Integer, @NotNull IExpression, @NotNull IExpression> parser) {
    int numChildren = context.getChildCount();

    IExpression retval;
    if (numChildren == 0) {
      retval = null;
    } else {
      ParseTree leftTree = context.getChild(0);
      IExpression result = ObjectUtils.notNull(leftTree.accept(this));

      for (int i = 1; i < numChildren; i = i + step) {
        result = parser.apply(context, i, result);
      }
      retval = result;
    }
    return retval;
  }

  @Override
  protected IExpression handleExpr(ExprContext ctx) {
    return handleNAiryCollection(ctx, children -> new Metapath(children));
  }

  @Override
  protected IExpression handleOrexpr(OrexprContext ctx) {
    return handleNAiryCollection(ctx, children -> new Or(children));
  }

  @Override
  protected IExpression handleAndexpr(AndexprContext ctx) {
    return handleNAiryCollection(ctx, children -> new And(children));
  }

  @Override
  protected IExpression handleComparisonexpr(ComparisonexprContext ctx) {
    int numChildren = ctx.getChildCount();
    if (numChildren == 1) {
      return super.visitComparisonexpr(ctx);
    } else if (numChildren != 3) {
      throw new UnsupportedOperationException();
    }

    IExpression left = visit(ctx.getChild(0));
    IExpression right = visit(ctx.getChild(2));

    // the operator
    ParseTree operatorTree = ctx.getChild(1);
    Object payload = operatorTree.getPayload();
    Operator operator;

    IComparison retval;
    if (payload instanceof GeneralcompContext) {
      GeneralcompContext compContext = (GeneralcompContext) payload;
      int type = ((TerminalNode) compContext.getChild(0)).getSymbol().getType();
      switch (type) {
      case metapath10Lexer.EQ:
        operator = Operator.EQ;
        break;
      case metapath10Lexer.NE:
        operator = Operator.NE;
        break;
      case metapath10Lexer.LT:
        operator = Operator.LT;
        break;
      case metapath10Lexer.LE:
        operator = Operator.LE;
        break;
      case metapath10Lexer.GT:
        operator = Operator.GT;
        break;
      case metapath10Lexer.GE:
        operator = Operator.GE;
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) compContext.getChild(0)).getSymbol().getText());
      }
      retval = new GeneralComparison(left, operator, right);
    } else if (payload instanceof ValuecompContext) {
      ValuecompContext compContext = (ValuecompContext) payload;
      int type = ((TerminalNode) compContext.getChild(0)).getSymbol().getType();
      switch (type) {
      case metapath10Lexer.KW_EQ:
        operator = Operator.EQ;
        break;
      case metapath10Lexer.KW_NE:
        operator = Operator.NE;
        break;
      case metapath10Lexer.KW_LT:
        operator = Operator.LT;
        break;
      case metapath10Lexer.KW_LE:
        operator = Operator.LE;
        break;
      case metapath10Lexer.KW_GT:
        operator = Operator.GT;
        break;
      case metapath10Lexer.KW_GE:
        operator = Operator.GE;
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) compContext.getChild(0)).getSymbol().getText());
      }
      retval = new ValueComparison(left, operator, right);
    } else {
      throw new UnsupportedOperationException();
    }
    return retval;
  }

  @Override
  protected IExpression handleStringconcatexpr(StringconcatexprContext ctx) {
    return handleNAiryCollection(ctx, children -> new StringConcat(children));
  }

  @Override
  protected IExpression handleAdditiveexpr(AdditiveexprContext context) {
    return handleGroupedNAiry(context, 2, (ctx, idx, left) -> {
      ParseTree operatorTree = ctx.getChild(idx);
      ParseTree rightTree = ctx.getChild(idx + 1);
      IExpression right = rightTree.accept(this);

      int type = ((TerminalNode) operatorTree).getSymbol().getType();

      IExpression retval;
      switch (type) {
      case metapath10Lexer.PLUS:
        retval = new Addition(left, right);
        break;
      case metapath10Lexer.MINUS:
        retval = new Subtraction(left, right);
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) operatorTree).getSymbol().getText());
      }
      return retval;
    });
  }

  @Override
  protected IExpression handleMultiplicativeexpr(MultiplicativeexprContext context) {
    return handleGroupedNAiry(context, 2, (ctx, idx, left) -> {
      ParseTree operatorTree = ctx.getChild(idx);
      ParseTree rightTree = ctx.getChild(idx + 1);
      IExpression right = rightTree.accept(this);

      int type = ((TerminalNode) operatorTree).getSymbol().getType();
      IExpression retval;
      switch (type) {
      case metapath10Lexer.STAR:
        retval = new Multiplication(left, right);
        break;
      case metapath10Lexer.KW_DIV:
        retval = new Division(left, right);
        break;
      case metapath10Lexer.KW_IDIV:
        retval = new IntegerDivision(left, right);
        break;
      case metapath10Lexer.KW_MOD:
        retval = new Modulo(left, right);
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) operatorTree).getSymbol().getText());
      }
      return retval;
    });
  }

  @Override
  protected IExpression handleUnionexpr(UnionexprContext ctx) {
    return handleNAiryCollection(ctx, children -> new Union(children));
  }

  @Override
  protected IExpression handleIntersectexceptexpr(IntersectexceptexprContext context) {
    return handleGroupedNAiry(context, 2, (ctx, idx, left) -> {
      ParseTree operatorTree = ctx.getChild(idx);
      ParseTree rightTree = ctx.getChild(idx + 1);
      IExpression right = rightTree.accept(this);

      int type = ((TerminalNode) operatorTree).getSymbol().getType();

      IExpression retval;
      switch (type) {
      case metapath10Lexer.KW_INTERSECT:
        retval = new Intersect(left, right);
        break;
      case metapath10Lexer.KW_EXCEPT:
        retval = new Except(left, right);
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) operatorTree).getSymbol().getText());
      }
      return retval;
    });
  }

  @Override
  protected IExpression handleArrowexpr(ArrowexprContext context) {
    return handleGroupedNAiry(context, 2, (ctx, idx, left) -> {
      // the next child is "=>"
      assert "=>".equals(ctx.getChild(idx).getText());

      @SuppressWarnings("null")
      FunctioncallContext fcCtx = ctx.getChild(FunctioncallContext.class, idx + 1);
      String name = fcCtx.eqname().getText();

      Stream<@NotNull IExpression> args = parseArgumentList(fcCtx.argumentlist());
      args = Stream.concat(Stream.of(left), args);

      return new FunctionCall(name, args.collect(Collectors.toUnmodifiableList()));
    });
  }

  @Override
  protected IExpression handleUnaryexpr(UnaryexprContext ctx) {
    int numChildren = ctx.getChildCount();
    int negateCount = 0;

    int idx = 0;
    for (; idx < numChildren - 1; idx++) {
      ParseTree tree = ctx.getChild(idx);
      int type = ((TerminalNode) tree).getSymbol().getType();
      switch (type) {
      case metapath10Lexer.PLUS:
        break;
      case metapath10Lexer.MINUS:
        negateCount++;
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) tree).getSymbol().getText());
      }
    }

    ParseTree expr = ctx.getChild(0);
    IExpression retval = expr.accept(this);
    if (negateCount % 2 == 1) {
      retval = new Negate(retval);
    }
    return retval;
  }

  @Override
  protected IExpression handlePathexpr(PathexprContext ctx) {
    int numChildren = ctx.getChildCount();

    IExpression retval;
    ParseTree tree = ctx.getChild(0);
    if (tree instanceof TerminalNode) {
      int type = ((TerminalNode) tree).getSymbol().getType();
      switch (type) {
      case metapath10Lexer.SLASH:
        // a slash expression with optional path
        if (numChildren == 2) {
          // the optional path
          ParseTree pathTree = ctx.getChild(1);
          IExpression relativeExpr = pathTree.accept(this);
          retval = new RootSlashPath(relativeExpr);
        } else {
          retval = new RootSlashOnlyPath();
        }
        break;
      case metapath10Lexer.SS:
        // a double slash expression with path
        ParseTree pathTree = ctx.getChild(1);
        IExpression node = pathTree.accept(this);
        retval = new RootDoubleSlashPath(node);
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) tree).getSymbol().getText());
      }
    } else {
      // a relative expression or something else
      retval = tree.accept(this);
    }
    return retval;
  }

  @Override
  protected IExpression handleRelativepathexpr(RelativepathexprContext context) {
    return handleGroupedNAiry(context, 2, (ctx, idx, left) -> {
      ParseTree operatorTree = ctx.getChild(idx);
      ParseTree rightTree = ctx.getChild(idx + 1);
      IExpression rightResult = rightTree.accept(this);

      int type = ((TerminalNode) operatorTree).getSymbol().getType();

      IExpression retval;
      switch (type) {
      case metapath10Lexer.SLASH:
        retval = new RelativeSlashPath(left, rightResult);
        break;
      case metapath10Lexer.SS:
        retval = new RelativeDoubleSlashPath(left, rightResult);
        break;
      default:
        throw new UnsupportedOperationException(((TerminalNode) operatorTree).getSymbol().getText());
      }
      return retval;
    });
  }

  @SuppressWarnings("null")
  @NotNull
  protected IExpression parsePredicate(@NotNull PredicateContext context) {
    // the expression is always the second child
    ParseTree tree = context.getChild(1);
    return tree.accept(this);
  }

  @NotNull
  protected List<@NotNull IExpression> parsePredicates(@NotNull ParseTree context, int staringChild) {
    int numChildren = context.getChildCount();
    int numPredicates = numChildren - staringChild;

    List<@NotNull IExpression> predicates;
    if (numPredicates == 0) {
      // no predicates
      predicates = CollectionUtil.emptyList();
    } else if (numPredicates == 1) {
      // single predicate
      PredicateContext predicate = ObjectUtils.notNull((PredicateContext) context.getChild(staringChild));
      predicates = CollectionUtil.singletonList(parsePredicate(predicate));
    } else {
      // multiple predicates
      predicates = new ArrayList<>(numPredicates);
      for (int i = staringChild; i < numChildren; i++) {
        PredicateContext predicate = ObjectUtils.notNull((PredicateContext) context.getChild(i));
        predicates.add(parsePredicate(predicate));
      }
    }
    return predicates;
  }

  @Override
  protected IExpression handlePostfixexpr(PostfixexprContext ctx) {
    int numChildren = ctx.getChildCount();
    ParseTree primaryTree = ctx.getChild(0);
    IExpression retval = ObjectUtils.notNull(primaryTree.accept(this));

    List<@NotNull IExpression> predicates = numChildren > 1 ? parsePredicates(ctx, 1) : CollectionUtil.emptyList();

    if (!predicates.isEmpty()) {
      retval = new Predicate(retval, predicates);
    }
    return retval;

  }

  @Override
  protected IExpression handleAxisstep(AxisstepContext ctx) {
    IExpression step = ctx.getChild(0).accept(this);

    ParseTree predicateTree = ctx.getChild(1);
    List<@NotNull IExpression> predicates = parsePredicates(predicateTree, 0);

    return predicates.isEmpty() ? step : new Predicate(step, predicates);
  }

  @Override
  protected IExpression handleForwardstep(ForwardstepContext ctx) {
    assert ctx.getChildCount() == 2;

    Token token = (Token) ctx.forwardaxis().getChild(0).getPayload();

    Step.Axis axis;
    switch (token.getType()) {
    case metapath10Lexer.KW_SELF:
      axis = Step.Axis.SELF;
      break;
    case metapath10Lexer.KW_CHILD:
      axis = Step.Axis.CHILDREN;
      break;
    case metapath10Lexer.KW_DESCENDANT:
      axis = Step.Axis.DESCENDANT;
      break;
    case metapath10Lexer.KW_DESCENDANT_OR_SELF:
      axis = Step.Axis.DESCENDANT_OR_SELF;
      break;
    default:
      throw new UnsupportedOperationException(token.getText());
    }
    return new Step(axis, ctx.nametest().accept(this));
  }

  @Override
  protected IExpression handleAbbrevforwardstep(AbbrevforwardstepContext ctx) {
    int numChildren = ctx.getChildCount();
    IExpression retval;
    if (numChildren == 1) {
      ParseTree tree = ctx.getChild(0);
      retval = new ModelInstance(tree.accept(this));
    } else {
      // this is an AT test
      ParseTree tree = ctx.getChild(1);
      retval = new Flag(tree.accept(this));

    }
    return retval;
  }

  @Override
  protected IExpression handleReversestep(ReversestepContext ctx) {
    assert ctx.getChildCount() == 2;

    Token token = (Token) ctx.reverseaxis().getChild(0).getPayload();

    Step.Axis axis;
    switch (token.getType()) {
    case metapath10Lexer.KW_PARENT:
      axis = Step.Axis.PARENT;
      break;
    case metapath10Lexer.KW_ANCESTOR:
      axis = Step.Axis.ANCESTOR;
      break;
    case metapath10Lexer.KW_ANCESTOR_OR_SELF:
      axis = Step.Axis.ANCESTOR_OR_SELF;
      break;
    default:
      throw new UnsupportedOperationException(token.getText());
    }
    return new Step(axis, ctx.nametest().accept(this));
  }

  @Override
  protected IExpression handleAbbrevreversestep(AbbrevreversestepContext ctx) {
    return ParentItem.instance();
  }

  @Override
  protected IExpression handleStringLiteral(LiteralContext ctx) {
    ParseTree tree = ctx.getChild(0);
    return new StringLiteral(tree.getText());
  }

  @Override
  protected IExpression handleNumericLiteral(NumericliteralContext ctx) {
    ParseTree tree = ctx.getChild(0);
    Token token = (Token) tree.getPayload();
    IExpression retval;
    switch (token.getType()) {
    case metapath10Lexer.IntegerLiteral:
      retval = new IntegerLiteral(new BigInteger(token.getText()));
      break;
    case metapath10Lexer.DecimalLiteral:
    case metapath10Lexer.DoubleLiteral:
      retval = new DecimalLiteral(new BigDecimal(token.getText()));
      break;
    default:
      throw new UnsupportedOperationException(token.getText());
    }
    return retval;
  }

  @Override
  protected IExpression handleContextitemexpr(ContextitemexprContext ctx) {
    return ContextItem.instance();
  }

  protected Stream<@NotNull IExpression>
      parseArgumentList(ArgumentlistContext context) {
    int numChildren = context.getChildCount();

    Stream<@NotNull IExpression> retval;
    if (numChildren == 2) {
      // just the OP CP tokens, which is an empty list
      retval = Stream.empty();
    } else {
      retval = context.argument().stream()
          .map(argument -> {
            return argument.exprsingle().accept(this);
          });
    }
    return retval;
  }

  @Override
  protected IExpression handleFunctioncall(FunctioncallContext ctx) {
    EqnameContext nameCtx = ctx.eqname();
    String name = nameCtx.getText();
    return new FunctionCall(name, parseArgumentList(ctx.argumentlist()).collect(Collectors.toUnmodifiableList()));
  }

  @Override
  protected IExpression handleEqname(EqnameContext ctx) {
    ParseTree tree = ctx.getChild(0);
    String name = ((TerminalNode) tree).getText();
    return new Name(name);
  }

  @Override
  protected IExpression handleWildcard(WildcardContext ctx) {
    return new Wildcard();
  }

  @FunctionalInterface
  interface ITriFunction<T, U, V, R> {

    R apply(T argT, U argU, V argV);

    default <W> ITriFunction<T, U, V, W> andThen(Function<? super R, ? extends W> after) {
      Objects.requireNonNull(after);
      return (T t, U u, V v) -> after.apply(apply(t, u, v));
    }
  }
}