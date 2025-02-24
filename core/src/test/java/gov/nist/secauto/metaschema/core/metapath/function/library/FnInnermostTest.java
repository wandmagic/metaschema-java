/*
/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.function.library;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.ExpressionTestBase;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.MetapathException;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IStringItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem;
import gov.nist.secauto.metaschema.core.metapath.type.InvalidTypeMetapathException;
import gov.nist.secauto.metaschema.core.metapath.type.TypeMetapathException;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;
import gov.nist.secauto.metaschema.core.testing.model.mocking.MockedDocumentGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

class FnInnermostTest
    extends ExpressionTestBase {

  private static Stream<Arguments> provideValues() { // NOPMD - false positive
    return Stream.of(
        Arguments.of(
            "(/root/assembly/@assembly-flag,/root/field/@field-flag)",
            "(.,/root,/root/assembly,/root/assembly/@assembly-flag,/root/field,/root/field/@field-flag)"),
        Arguments.of(
            "(/root/assembly,/root/field)",
            "(.,/root,/root/assembly,/root/field)"),
        Arguments.of(
            "(/root/assembly/@assembly-flag,/root/field/@field-flag)",
            "(.,/root,/root/assembly,/root/assembly/@assembly-flag,/root/field,/root/field/@field-flag," +
                ".,/root,/root/assembly,/root/assembly/@assembly-flag,/root/field,/root/field/@field-flag)"));
  }

  @ParameterizedTest
  @MethodSource("provideValues")
  void test(@NonNull String expectedValueMetapath, @NonNull String actualValuesMetapath) {
    DynamicContext dynamicContext = newDynamicContext();
    INodeItem node = MockedDocumentGenerator.generateDocumentNodeItem();

    ISequence<? extends INodeItem> expected
        = IMetapathExpression.compile(expectedValueMetapath, dynamicContext.getStaticContext())
            .evaluate(node, dynamicContext);

    ISequence<? extends INodeItem> actual
        = IMetapathExpression.compile("innermost(" + actualValuesMetapath + ")", dynamicContext.getStaticContext())
            .evaluate(node, dynamicContext);

    // Test the expected values against the alternate implementation from the spec
    ISequence<? extends INodeItem> values
        = IMetapathExpression.compile(expectedValueMetapath, dynamicContext.getStaticContext())
            .evaluate(node, dynamicContext);
    ISequence<? extends INodeItem> alternate
        = IMetapathExpression.compile("$nodes except $nodes/ancestor::node()", dynamicContext.getStaticContext())
            .evaluate(null, dynamicContext.subContext().bindVariableValue(IEnhancedQName.of("nodes"), values));

    assertEquals(expected, actual);
    assertEquals(expected, alternate);
  }

  @Test
  void testNotANode() {
    DynamicContext dynamicContext = newDynamicContext();

    MetapathException ex = assertThrows(MetapathException.class, () -> {
      IMetapathExpression.compile("innermost('test')", dynamicContext.getStaticContext())
          .evaluateAs(IStringItem.valueOf("test"), IMetapathExpression.ResultType.ITEM, dynamicContext);
    });
    Throwable cause = ex.getCause() != null ? ex.getCause().getCause() : null;

    assertAll(
        () -> assertEquals(InvalidTypeMetapathException.class, cause == null
            ? null
            : cause.getClass()),
        () -> assertEquals(TypeMetapathException.INVALID_TYPE_ERROR, cause instanceof TypeMetapathException
            ? ((TypeMetapathException) cause).getCode()
            : null));
  }
}
