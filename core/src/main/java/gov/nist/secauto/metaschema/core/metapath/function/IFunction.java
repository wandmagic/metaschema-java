/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.function;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.MetapathException;
import gov.nist.secauto.metaschema.core.metapath.StaticContext;
import gov.nist.secauto.metaschema.core.metapath.StaticMetapathException;
import gov.nist.secauto.metaschema.core.metapath.item.ICollectionValue;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IAnyAtomicItem;
import gov.nist.secauto.metaschema.core.metapath.type.IItemType;
import gov.nist.secauto.metaschema.core.metapath.type.ISequenceType;
import gov.nist.secauto.metaschema.core.metapath.type.Occurrence;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A common interface for all Metapath functions.
 */
public interface IFunction extends IItem {
  /**
   * Details specific characteristics of a function.
   */
  enum FunctionProperty {
    /**
     * Indicates that the function will produce identical results for the same
     * arguments (see XPath 3.1 <a href=
     * "https://www.w3.org/TR/xpath-functions-31/#dt-deterministic">deterministic</a>).
     * If not assigned to a function definition, a function call with the same
     * arguments is not guaranteed to produce the same results in the same order for
     * subsequent calls within the same execution context.
     */
    DETERMINISTIC,
    /**
     * Indicates that the result of the function depends on property values within
     * the static or dynamic context and the provided arguments (see XPath 3.1
     * <a href=
     * "https://www.w3.org/TR/xpath-functions-31/#dt-context-dependent">context-dependent</a>).
     * If not assigned to a function definition, a call will not be affected by the
     * property values within the static or dynamic context and will not have any
     * arguments.
     */
    CONTEXT_DEPENDENT,
    /**
     * Indicates that the result of the function depends on the current focus (see
     * XPath 3.1 <a href=
     * "https://www.w3.org/TR/xpath-functions-31/#dt-focus-independent">focus-dependent</a>).
     * If not assigned to a function definition, a call will not be affected by the
     * current focus.
     */
    FOCUS_DEPENDENT,
    /**
     * The function allows the last argument to be repeated any number of times.
     */
    UNBOUNDED_ARITY;
  }

  /**
   * Get the type information for this item.
   *
   * @return the type information
   */
  @NonNull
  static IItemType type() {
    return IItemType.function();
  }

  @Override
  default IItemType getType() {
    // TODO: implement this based on the signature
    return IItemType.function();
  }

  /**
   * Retrieve the name of the function.
   *
   * @return the function's name
   */
  @NonNull
  default String getName() {
    return ObjectUtils.notNull(getQName().getLocalName());
  }

  /**
   * Retrieve the namespace qualified name of the function.
   *
   * @return the namespace qualified name
   */
  @NonNull
  IEnhancedQName getQName();

  /**
   * Retrieve the set of assigned function properties.
   *
   * @return the set of properties or an empty set
   */
  @NonNull
  Set<FunctionProperty> getProperties();

  /**
   * Retrieve the list of function arguments.
   *
   * @return the function arguments or an empty list if there are none
   */
  @NonNull
  List<IArgument> getArguments();

  /**
   * Determine the number of arguments the function has.
   *
   * @return the number of function arguments
   */
  int arity();

  /**
   * Determines if the result of the function call will produce identical results
   * when provided the same implicit or explicit arguments.
   *
   * @return {@code true} if function is deterministic or {@code false} otherwise
   * @see FunctionProperty#DETERMINISTIC
   */
  default boolean isDeterministic() {
    return getProperties().contains(FunctionProperty.DETERMINISTIC);
  }

  /**
   * Determines if the result of the function call depends on property values
   * within the static or dynamic context and the provided arguments.
   *
   * @return {@code true} if function is context dependent or {@code false}
   *         otherwise
   * @see FunctionProperty#CONTEXT_DEPENDENT
   */
  default boolean isContextDepenent() {
    return getProperties().contains(FunctionProperty.CONTEXT_DEPENDENT);
  }

  /**
   * Determines if the result of the function call depends on the current focus.
   *
   * @return {@code true} if function is focus dependent or {@code false}
   *         otherwise
   * @see FunctionProperty#FOCUS_DEPENDENT
   */
  default boolean isFocusDependent() {
    return getProperties().contains(FunctionProperty.FOCUS_DEPENDENT);
  }

  /**
   * Determines if the final argument can be repeated.
   *
   * @return {@code true} if the final argument can be repeated or {@code false}
   *         otherwise
   * @see FunctionProperty#UNBOUNDED_ARITY
   */
  default boolean isArityUnbounded() {
    return getProperties().contains(FunctionProperty.UNBOUNDED_ARITY);
  }

  /**
   * Retrieve the function result sequence type.
   *
   * @return the function result sequence type
   */
  @NonNull
  ISequenceType getResult();

  // /**
  // * Determines by static analysis if the function supports the expression
  // arguments provided.
  // *
  // * @param arguments
  // * the expression arguments to evaluate
  // * @return {@code true} if the arguments are supported or {@code false}
  // otherwise
  // */
  // boolean isSupported(List<IExpression<?>> arguments);

  @Override
  default boolean deepEquals(ICollectionValue other) {
    // this is the expected result
    return false;
  }

  /**
   * Execute the function with the provided {@code arguments}, using the provided
   * {@code DynamicContext} and {@code focus}.
   *
   * @param arguments
   *          the function arguments or an empty list if there are no arguments
   * @param dynamicContext
   *          the dynamic evaluation context
   * @param focus
   *          the current focus or an empty sequence if there is no focus
   * @return the function result
   * @throws MetapathException
   *           if an error occurred while executing the function
   */
  @NonNull
  ISequence<?> execute(
      @NonNull List<? extends ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      @NonNull ISequence<?> focus);

  @Override
  default IAnyAtomicItem toAtomicItem() {
    throw new InvalidTypeFunctionException(InvalidTypeFunctionException.DATA_ITEM_IS_FUNCTION, this);
  }

  /**
   * Get the signature of the function as a string.
   *
   * @return the signature
   */
  @Override
  @NonNull
  default String toSignature() {
    return ObjectUtils.notNull(String.format("%s(%s) as %s",
        getQName(),
        getArguments().isEmpty() ? ""
            : getArguments().stream().map(IArgument::toSignature).collect(Collectors.joining(","))
                + (isArityUnbounded() ? ", ..." : ""),
        getResult().toSignature()));
  }

  /**
   * Construct a new function signature builder.
   *
   * @return the new builder instance
   */
  @NonNull
  static Builder builder() {
    return builder(StaticContext.instance());
  }

  /**
   * Construct a new function signature builder.
   *
   * @param staticContext
   *          the static context used to lookup data types and function
   *          implementations
   *
   * @return the new builder instance
   */
  @NonNull
  static Builder builder(@NonNull StaticContext staticContext) {
    return new Builder(staticContext);
  }

  /**
   * Used to create a function's signature using a builder pattern.
   */
  @SuppressWarnings("PMD.LooseCoupling")
  final class Builder {
    @NonNull
    private final StaticContext staticContext;
    private String name;
    private String namespace;
    @SuppressWarnings("null")
    @NonNull
    private final EnumSet<FunctionProperty> properties = EnumSet.noneOf(FunctionProperty.class);
    @NonNull
    private final List<IArgument> arguments = new LinkedList<>();
    @NonNull
    private IItemType returnType = IItem.type();
    @NonNull
    private Occurrence returnOccurrence = Occurrence.ONE;
    private IFunctionExecutor functionHandler;

    private Builder(@NonNull StaticContext staticContext) {
      this.staticContext = staticContext;
    }

    private StaticContext getStaticContext() {
      return staticContext;
    }

    /**
     * Define the name of the function.
     *
     * @param name
     *          the function's name
     * @return this builder
     */
    @NonNull
    public Builder name(@NonNull String name) {
      Objects.requireNonNull(name, "name");
      if (name.isBlank()) {
        throw new IllegalArgumentException("the name must be non-blank");
      }
      this.name = name.trim();
      return this;
    }

    /**
     * Define the namespace of the function.
     *
     * @param name
     *          the function's namespace URI as a string
     * @return this builder
     */
    @NonNull
    public Builder namespace(@NonNull String name) {
      Objects.requireNonNull(name, "name");
      if (name.isBlank()) {
        throw new IllegalArgumentException("the name must be non-blank");
      }
      this.namespace = name.trim();
      return this;
    }

    /**
     * Mark the function as deterministic.
     *
     * @return this builder
     * @see IFunction.FunctionProperty#DETERMINISTIC
     */
    @NonNull
    public Builder deterministic() {
      properties.add(FunctionProperty.DETERMINISTIC);
      return this;
    }

    /**
     * Mark the function as non-deterministic.
     *
     * @return this builder
     * @see IFunction.FunctionProperty#DETERMINISTIC
     */
    @NonNull
    public Builder nonDeterministic() {
      properties.remove(FunctionProperty.DETERMINISTIC);
      return this;
    }

    /**
     * Mark the function as context dependent.
     *
     * @return this builder
     * @see IFunction.FunctionProperty#CONTEXT_DEPENDENT
     */
    @NonNull
    public Builder contextDependent() {
      properties.add(FunctionProperty.CONTEXT_DEPENDENT);
      return this;
    }

    /**
     * Mark the function as context independent.
     *
     * @return this builder
     * @see IFunction.FunctionProperty#CONTEXT_DEPENDENT
     */
    @NonNull
    public Builder contextIndependent() {
      properties.remove(FunctionProperty.CONTEXT_DEPENDENT);
      return this;
    }

    /**
     * Mark the function as focus dependent.
     *
     * @return this builder
     * @see IFunction.FunctionProperty#FOCUS_DEPENDENT
     */
    @NonNull
    public Builder focusDependent() {
      properties.add(FunctionProperty.FOCUS_DEPENDENT);
      return this;
    }

    /**
     * Mark the function as focus independent.
     *
     * @return this builder
     * @see IFunction.FunctionProperty#FOCUS_DEPENDENT
     */
    @NonNull
    public Builder focusIndependent() {
      properties.remove(FunctionProperty.FOCUS_DEPENDENT);
      return this;
    }

    /**
     * Indicate if the last argument can be repeated.
     *
     * @param allow
     *          if {@code true} then the the last argument can be repeated an
     *          unlimited number of times, or {@code false} otherwise
     * @return this builder
     */
    @NonNull
    public Builder allowUnboundedArity(boolean allow) {
      if (allow) {
        properties.add(FunctionProperty.UNBOUNDED_ARITY);
      } else {
        properties.remove(FunctionProperty.UNBOUNDED_ARITY);
      }
      return this;
    }

    /**
     * Define the return sequence Java type of the function.
     *
     * @param name
     *          the extended qualified name of the function's return data type
     * @return this builder
     */
    @NonNull
    public Builder returnType(@NonNull String name) {
      try {
        this.returnType = getStaticContext().lookupAtomicType(name);
      } catch (StaticMetapathException ex) {
        throw new IllegalArgumentException(
            String.format("No data type with the name '%s'.", name), ex);
      }
      return this;
    }

    /**
     * Define the return sequence Java type of the function.
     *
     * @param name
     *          the qualified name of the function's return data type
     * @return this builder
     */
    @NonNull
    public Builder returnType(@NonNull IEnhancedQName name) {
      try {
        this.returnType = StaticContext.lookupAtomicType(name);
      } catch (StaticMetapathException ex) {
        throw new IllegalArgumentException(
            String.format("No data type with the name '%s'.", name), ex);
      }
      return this;
    }

    /**
     * Define the return sequence Java type of the function.
     *
     * @param type
     *          the function's return Java type
     * @return this builder
     */
    @NonNull
    public Builder returnType(@NonNull IItemType type) {
      this.returnType = type;
      return this;
    }

    /**
     * Indicate the sequence returned will contain zero or one items.
     *
     * @return this builder
     */
    @NonNull
    public Builder returnZeroOrOne() {
      return returnOccurrence(Occurrence.ZERO_OR_ONE);
    }

    /**
     * Indicate the sequence returned will contain one item.
     *
     * @return this builder
     */
    @NonNull
    public Builder returnOne() {
      return returnOccurrence(Occurrence.ONE);
    }

    /**
     * Indicate the sequence returned will contain zero or more items.
     *
     * @return this builder
     */
    @NonNull
    public Builder returnZeroOrMore() {
      return returnOccurrence(Occurrence.ZERO_OR_MORE);
    }

    /**
     * Indicate the sequence returned will contain one or more items.
     *
     * @return this builder
     */
    @NonNull
    public Builder returnOneOrMore() {
      return returnOccurrence(Occurrence.ONE_OR_MORE);
    }

    @NonNull
    private Builder returnOccurrence(@NonNull Occurrence occurrence) {
      Objects.requireNonNull(occurrence, "occurrence");
      this.returnOccurrence = occurrence;
      return this;
    }

    /**
     * Add an argument based on the provided {@code builder}.
     *
     * @param builder
     *          the argument builder
     * @return this builder
     */
    @NonNull
    public Builder argument(@NonNull IArgument.Builder builder) {
      return argument(builder.build());
    }

    /**
     * Add an argument based on the provided {@code argument} signature.
     *
     * @param argument
     *          the argument
     * @return this builder
     */
    @NonNull
    public Builder argument(@NonNull IArgument argument) {
      Objects.requireNonNull(argument, "argument");
      this.arguments.add(argument);
      return this;
    }

    /**
     * Specify the static function to call when executing the function.
     *
     * @param handler
     *          a method implementing the {@link IFunctionExecutor} functional
     *          interface
     * @return this builder
     */
    @NonNull
    public Builder functionHandler(@NonNull IFunctionExecutor handler) {
      Objects.requireNonNull(handler, "handler");
      this.functionHandler = handler;
      return this;
    }

    /**
     * Builds the function's signature.
     *
     * @return the function's signature
     */
    @NonNull
    public IFunction build() {
      if (properties.contains(FunctionProperty.UNBOUNDED_ARITY) && arguments.isEmpty()) {
        throw new IllegalStateException("to allow unbounded arity, at least one argument must be provided");
      }

      return new DefaultFunction(
          ObjectUtils.requireNonNull(name, "the name must not be null"),
          ObjectUtils.requireNonNull(namespace, "the namespace must not be null"),
          properties,
          new ArrayList<>(arguments),
          // FIXME: Should return type be ISequenceType?
          ISequenceType.of(returnType, returnOccurrence),
          ObjectUtils.requireNonNull(functionHandler, "the function handler must not be null"));
    }
  }
}
