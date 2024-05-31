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

package gov.nist.secauto.metaschema.core.metapath.function.library;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.ICollectionValue;
import gov.nist.secauto.metaschema.core.metapath.ISequence;
import gov.nist.secauto.metaschema.core.metapath.MetapathConstants;
import gov.nist.secauto.metaschema.core.metapath.function.FunctionUtils;
import gov.nist.secauto.metaschema.core.metapath.function.IArgument;
import gov.nist.secauto.metaschema.core.metapath.function.IFunction;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IAnyAtomicItem;
import gov.nist.secauto.metaschema.core.metapath.item.function.IArrayItem;
import gov.nist.secauto.metaschema.core.metapath.item.function.IMapItem;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class MapFind {
  @NonNull
  public static final IFunction SIGNATURE = IFunction.builder()
      .name("find")
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS_MAP)
      .deterministic()
      .contextIndependent()
      .focusIndependent()
      .argument(IArgument.builder()
          .name("input")
          .type(IItem.class)
          .zeroOrMore()
          .build())
      .argument(IArgument.builder()
          .name("key")
          .type(IAnyAtomicItem.class)
          .one()
          .build())
      .returnType(IArrayItem.class)
      .returnOne()
      .functionHandler(MapFind::execute)
      .build();

  private MapFind() {
    // disable construction
  }

  @SuppressWarnings("unused")
  @NonNull
  private static ISequence<?> execute(@NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {
    ISequence<?> input = FunctionUtils.asType(ObjectUtils.requireNonNull(arguments.get(0)));
    IAnyAtomicItem key = FunctionUtils.asType(ObjectUtils.requireNonNull(arguments.get(1).getFirstItem(true)));

    return ISequence.of(IArrayItem.ofCollection(
        ObjectUtils.notNull(find((Collection<? extends IItem>) input, key)
            .collect(Collectors.toList()))));
  }

  /**
   * An implementation of XPath 3.1 <a href=
   * "https://www.w3.org/TR/xpath-functions-31/#func-map-find">map:find</a>.
   *
   * @param input
   *          the item sequence to search for key matches
   * @param key
   *          the key for the item to retrieve
   * @return the retrieved item
   */
  @NonNull
  public static Stream<ICollectionValue> find(
      @NonNull Collection<? extends IItem> input,
      @NonNull IAnyAtomicItem key) {
    return ObjectUtils.notNull(input.stream()
        // handle item
        .flatMap(item -> find(ObjectUtils.notNull(item), key)));
  }

  @NonNull
  public static Stream<ICollectionValue> find(
      @NonNull IItem item,
      @NonNull IAnyAtomicItem key) {
    Stream<ICollectionValue> retval;
    if (item instanceof IArrayItem) {
      IArrayItem<?> array = (IArrayItem<?>) item;
      retval = ObjectUtils.notNull(array.stream()
          // handle array values
          .flatMap(value -> find(ObjectUtils.notNull(value), key)));
    } else if (item instanceof IMapItem) {
      IMapItem<?> map = (IMapItem<?>) item;
      // handle map
      retval = find(map, key);
    } else {
      // do nothing
      retval = ObjectUtils.notNull(Stream.empty());
    }
    return retval;
  }

  @NonNull
  private static Stream<ICollectionValue> find(
      @NonNull ICollectionValue value,
      @NonNull IAnyAtomicItem key) {
    Stream<ICollectionValue> retval;
    if (value instanceof ISequence) {
      ISequence<?> sequence = (ISequence<?>) value;
      // handle sequence items
      retval = find((Collection<? extends IItem>) sequence, key);
    } else {
      // handle item
      retval = find((IItem) value, key);
    }
    return retval;
  }

  @NonNull
  public static Stream<ICollectionValue> find(
      @NonNull IMapItem<?> map,
      @NonNull IAnyAtomicItem key) {
    return ObjectUtils.notNull(Stream.concat(
        // add matching value, if it exists
        Stream.ofNullable(MapGet.get(map, key)),
        map.values().stream()
            // handle map values
            .flatMap(value -> find(ObjectUtils.notNull(value), key))));
  }
}
