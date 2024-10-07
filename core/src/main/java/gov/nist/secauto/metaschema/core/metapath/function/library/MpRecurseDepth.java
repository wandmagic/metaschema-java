/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.function.library;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.ISequence;
import gov.nist.secauto.metaschema.core.metapath.MetapathConstants;
import gov.nist.secauto.metaschema.core.metapath.MetapathException;
import gov.nist.secauto.metaschema.core.metapath.MetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.StaticMetapathException;
import gov.nist.secauto.metaschema.core.metapath.function.FunctionUtils;
import gov.nist.secauto.metaschema.core.metapath.function.IArgument;
import gov.nist.secauto.metaschema.core.metapath.function.IFunction;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IStringItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.List;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Provides functions that evaluate a Metapath recursively over sequences.
 */
public final class MpRecurseDepth {
  private static final String NAME = "recurse-depth";

  @NonNull
  static final IFunction SIGNATURE_ONE_ARG = IFunction.builder()
      .name(NAME)
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS_EXTENDED)
      .deterministic()
      .contextDependent()
      .focusDependent()
      .argument(IArgument.builder()
          .name("recursePath")
          .type(IStringItem.class)
          .one()
          .build())
      .returnType(INodeItem.class)
      .returnZeroOrMore()
      .functionHandler(MpRecurseDepth::executeOneArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_TWO_ARG = IFunction.builder()
      .name(NAME)
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS_EXTENDED)
      .deterministic()
      .contextDependent()
      .focusIndependent()
      .argument(IArgument.builder()
          .name("context")
          .type(INodeItem.class)
          .zeroOrMore()
          .build())
      .argument(IArgument.builder()
          .name("recursePath")
          .type(IStringItem.class)
          .one()
          .build())
      .returnType(INodeItem.class)
      .returnZeroOrMore()
      .functionHandler(MpRecurseDepth::executeTwoArg)
      .build();

  private MpRecurseDepth() {
    // disable construction
  }

  @SuppressWarnings("unused")
  @NonNull
  private static ISequence<INodeItem> executeOneArg(
      @NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {

    ISequence<INodeItem> initalContext = ISequence.of(FunctionUtils.requireType(INodeItem.class, focus));

    ISequence<? extends IStringItem> arg = FunctionUtils.asType(ObjectUtils.requireNonNull(arguments.get(0)));
    IStringItem recursionPath = ObjectUtils.requireNonNull(arg.getFirstItem(true));

    return recurseDepth(initalContext, recursionPath, dynamicContext);
  }

  @SuppressWarnings("unused")
  @NonNull
  private static ISequence<INodeItem> executeTwoArg(
      @NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {

    ISequence<INodeItem> initalContext = FunctionUtils.asType(ObjectUtils.requireNonNull(arguments.get(0)));

    ISequence<? extends IStringItem> arg = FunctionUtils.asType(ObjectUtils.requireNonNull(arguments.get(1)));
    IStringItem recursionPath = ObjectUtils.requireNonNull(arg.getFirstItem(true));

    return recurseDepth(initalContext, recursionPath, dynamicContext);
  }

  @NonNull
  private static ISequence<INodeItem> recurseDepth(
      @NonNull ISequence<INodeItem> initialContext,
      @NonNull IStringItem recursionPath,
      @NonNull DynamicContext dynamicContext) {
    MetapathExpression recursionMetapath;
    try {
      recursionMetapath = MetapathExpression.compile(recursionPath.asString(), dynamicContext.getStaticContext());
    } catch (MetapathException ex) {
      throw new StaticMetapathException(StaticMetapathException.INVALID_PATH_GRAMMAR, ex.getMessage(), ex);
    }

    return recurseDepth(initialContext, recursionMetapath, dynamicContext);
  }

  /**
   * Evaluates the {@code recursionMetapath} starting with the the items in the
   * {@code initialContext} and also recursively using the resulting items
   * returned by evaluating this path.
   *
   * @param initialContext
   *          the sequence containing the initial node items to evaluate the
   *          Metapath against
   * @param recursionMetapath
   *          the Metapath expression to use for recursive evaluation
   * @param dynamicContext
   *          the dynamic context supporting evaluation
   * @return the Metapath node items resulting from the Metapath execution
   */
  @NonNull
  public static ISequence<INodeItem> recurseDepth(
      @NonNull ISequence<INodeItem> initialContext,
      @NonNull MetapathExpression recursionMetapath,
      @NonNull DynamicContext dynamicContext) {

    return ISequence.of(ObjectUtils.notNull(initialContext.stream()
        .flatMap(item -> {
          @NonNull ISequence<INodeItem> metapathResult = recursionMetapath.evaluate(item, dynamicContext);
          // ensure this is list backed
          metapathResult.getValue();

          ISequence<INodeItem> result = recurseDepth(metapathResult, recursionMetapath, dynamicContext);
          return ObjectUtils.notNull(Stream.concat(Stream.of(item), result.stream()));
        })));
  }
}
