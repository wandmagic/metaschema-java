/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.function.library;

import static gov.nist.secauto.metaschema.core.metapath.TestUtils.integer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nist.secauto.metaschema.core.metapath.ExpressionTestBase;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IIntegerItem;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

class ArraySizeTest
    extends ExpressionTestBase {
  private static Stream<Arguments> provideValues() { // NOPMD - false positive
    return Stream.of(
        Arguments.of(
            integer(3),
            "array:size([\"a\", \"b\", \"c\"])"),
        Arguments.of(
            integer(2),
            "array:size([\"a\", [\"b\", \"c\"]])"),
        Arguments.of(
            integer(0),
            "array:size([ ])"),
        Arguments.of(
            integer(1),
            "array:size([[ ]])"));
  }

  @ParameterizedTest
  @MethodSource("provideValues")
  void testExpression(@NonNull IIntegerItem expected, @NonNull String metapath) {

    IItem result = IMetapathExpression.compile(metapath)
        .evaluateAs(null, IMetapathExpression.ResultType.ITEM, newDynamicContext());
    assertEquals(expected, result);
  }
}
