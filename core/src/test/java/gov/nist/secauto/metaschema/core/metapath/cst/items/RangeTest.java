/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst.items;

import static gov.nist.secauto.metaschema.core.metapath.TestUtils.integer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nist.secauto.metaschema.core.metapath.ExpressionTestBase;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

class RangeTest
    extends ExpressionTestBase {
  private static Stream<Arguments> provideValues() { // NOPMD - false positive
    return Stream.of(
        Arguments.of(
            ISequence.of(integer(10), integer(1), integer(2), integer(3), integer(4)),
            "(10, 1 to 4)"),
        Arguments.of(
            ISequence.of(integer(10)),
            "(10 to 10)"),
        Arguments.of(
            ISequence.of(integer(2), integer(3), integer(4), integer(5)),
            "2 to 5"),
        Arguments.of(
            ISequence.empty(),
            "() to 2"),
        Arguments.of(
            ISequence.empty(),
            "2 to ()"),
        Arguments.of(
            ISequence.empty(),
            "5 to 2"));
  }

  @ParameterizedTest
  @MethodSource("provideValues")
  void testRange(@NonNull ISequence<?> expected, @NonNull String metapath) {
    assertEquals(
        expected,
        IMetapathExpression.compile(metapath).evaluate(null, newDynamicContext()));
  }
}
