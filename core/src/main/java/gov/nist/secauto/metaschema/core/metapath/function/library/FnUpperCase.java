/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.function.library;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.ISequence;
import gov.nist.secauto.metaschema.core.metapath.MetapathConstants;
import gov.nist.secauto.metaschema.core.metapath.function.FunctionUtils;
import gov.nist.secauto.metaschema.core.metapath.function.IArgument;
import gov.nist.secauto.metaschema.core.metapath.function.IFunction;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IStringItem;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.List;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Based on the XPath 3.1 <a href=
 * "https://www.w3.org/TR/xpath-functions-31/#func-upper-case">fn:upper-case</a>
 * functions.
 */
public final class FnUpperCase {
  private static final String NAME = "upper-case";

  @NonNull
  static final IFunction SIGNATURE_ONE_ARG = IFunction.builder()
      .name(NAME)
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS)
      .deterministic()
      .contextIndependent()
      .focusIndependent()
      .argument(IArgument.builder()
          .name("arg")
          .type(IStringItem.type())
          .zeroOrOne()
          .build())
      .returnType(IStringItem.type())
      .returnOne()
      .functionHandler(FnUpperCase::executeOneArg)
      .build();

  private FnUpperCase() {
    // disable construction
  }

  @SuppressWarnings("unused")
  @NonNull
  private static ISequence<IStringItem> executeOneArg(@NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments, @NonNull DynamicContext dynamicContext, IItem focus) {

    // From the XPath 3.1 specification:
    // If the value of $arg is the empty sequence, the zero-length string is
    // returned.
    return ISequence.of(arguments.get(0).isEmpty()
        ? IStringItem.valueOf("")
        : fnUpperCase(FunctionUtils.asType(ObjectUtils.notNull(arguments.get(0).getFirstItem(true)))));
  }

  /**
   * An implementation of <a href=
   * "https://www.w3.org/TR/xpath-functions-31/#func-upper-case">fn:upper-case</a>.
   *
   * @param arg
   *          the string to be converted to lower case
   * @return the resulting string in upper case
   */
  @NonNull
  public static IStringItem fnUpperCase(@NonNull IStringItem arg) {
    return IStringItem.valueOf(ObjectUtils.notNull(arg.toString().toUpperCase(Locale.ROOT)));
  }
}
