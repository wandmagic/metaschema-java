/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IBooleanItem;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.hosuaby.inject.resources.junit.jupiter.GivenTextResource;
import io.hosuaby.inject.resources.junit.jupiter.TestWithResources;

@TestWithResources
class MetapathExpressionTest {

  @GivenTextResource(from = "/correct-examples.txt", charset = "UTF-8")
  String correctMetapathInstances;

  // @GivenTextResource(from = "/incorrect-examples.txt", charset = "UTF-8")
  // String incorrectMetapathInstances;

  @Test
  @Disabled
  void testCorrect() {
    for (String line : correctMetapathInstances.split("\\r?\\n")) {
      if (line.startsWith("# ")) {
        continue;
      }
      // System.out.println(line);
      IMetapathExpression.compile(line);
    }
  }
  //
  // @Test
  // @Disabled
  // void testIncorrect() {
  // for (String line : incorrectMetapathInstances.split("\\r?\\n")) {
  // if (line.startsWith("# ")) {
  // continue;
  // }
  // // System.out.println(line);
  // try {
  // MetapathExpression.compile(line);
  // } catch (ParseCancellationException ex) {
  // // ex.printStackTrace();
  // }
  // }
  // }

  @Test
  void testSyntaxError() {
    assertThrows(MetapathException.class, () -> {
      IMetapathExpression.compile("**");
    });
  }

  @Test
  void test() {
    IMetapathExpression path = IMetapathExpression.compile("2 eq 1 + 1");
    ISequence<?> result = path.evaluate();
    assertNotNull(result, "null result");
    assertTrue(!result.isEmpty(), "result was empty");
    assertEquals(1, result.size(), "unexpected size");
    assertEquals(true, ((IBooleanItem) result.iterator().next()).toBoolean(), "unexpected result");
  }

  @Test
  void testMalformedIf() {
    StaticMetapathException ex = assertThrows(StaticMetapathException.class, () -> {
      IMetapathExpression.compile("if 'a' = '1.1.2' then true() else false()");
    });
    assertEquals(StaticMetapathException.INVALID_PATH_GRAMMAR, ex.getCode());
  }
}
