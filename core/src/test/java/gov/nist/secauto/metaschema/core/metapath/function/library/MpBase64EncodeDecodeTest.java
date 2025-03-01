/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.function.library;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nist.secauto.metaschema.core.metapath.ExpressionTestBase;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IBase64BinaryItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IStringItem;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;

class MpBase64EncodeDecodeTest
    extends ExpressionTestBase {

  private static Stream<Arguments> provideValuesForEncodeDecode() {
    return Stream.of(
        Arguments.of("The quick brown fox jumps over the lazy dog",
            "VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wcyBvdmVyIHRoZSBsYXp5IGRvZw=="),
        Arguments.of("Mr. Watson, come here, I need you", "TXIuIFdhdHNvbiwgY29tZSBoZXJlLCBJIG5lZWQgeW91"),
        Arguments.of(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam eu luctus lorem. Aliquam malesuada lorem"
                + " nisi, ut tincidunt neque feugiat vitae. Fusce pretium nunc ac sapien feugiat accumsan. Proin eget"
                + " ligula non turpis laoreet fermentum. Aliquam mi justo, gravida id vulputate id, venenatis eu felis."
                + " Vestibulum commodo, magna quis sollicitudin consectetur, eros erat elementum libero, nec euismod"
                + " elit arcu non diam. Sed iaculis dui lacus, vitae placerat velit iaculis quis. Sed in ligula in eros"
                + " luctus porttitor. Nullam in laoreet leo. Cras sed nisl eget turpis sollicitudin molestie et eget"
                + " tellus. Vivamus aliquam odio et dui mattis, in rhoncus mauris hendrerit. Nam viverra mattis risus"
                + " non tristique.",
            "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4gRXRpYW0gZXUgbHVjdHVzIGxvcmVtLi"
                + "BBbGlxdWFtIG1hbGVzdWFkYSBsb3JlbSBuaXNpLCB1dCB0aW5jaWR1bnQgbmVxdWUgZmV1Z2lhdCB2aXRhZS4gRnVzY2UgcHJldG"
                + "l1bSBudW5jIGFjIHNhcGllbiBmZXVnaWF0IGFjY3Vtc2FuLiBQcm9pbiBlZ2V0IGxpZ3VsYSBub24gdHVycGlzIGxhb3JlZXQgZm"
                + "VybWVudHVtLiBBbGlxdWFtIG1pIGp1c3RvLCBncmF2aWRhIGlkIHZ1bHB1dGF0ZSBpZCwgdmVuZW5hdGlzIGV1IGZlbGlzLiBWZX"
                + "N0aWJ1bHVtIGNvbW1vZG8sIG1hZ25hIHF1aXMgc29sbGljaXR1ZGluIGNvbnNlY3RldHVyLCBlcm9zIGVyYXQgZWxlbWVudHVtIG"
                + "xpYmVybywgbmVjIGV1aXNtb2QgZWxpdCBhcmN1IG5vbiBkaWFtLiBTZWQgaWFjdWxpcyBkdWkgbGFjdXMsIHZpdGFlIHBsYWNlcm"
                + "F0IHZlbGl0IGlhY3VsaXMgcXVpcy4gU2VkIGluIGxpZ3VsYSBpbiBlcm9zIGx1Y3R1cyBwb3J0dGl0b3IuIE51bGxhbSBpbiBsYW"
                + "9yZWV0IGxlby4gQ3JhcyBzZWQgbmlzbCBlZ2V0IHR1cnBpcyBzb2xsaWNpdHVkaW4gbW9sZXN0aWUgZXQgZWdldCB0ZWxsdXMuIF"
                + "ZpdmFtdXMgYWxpcXVhbSBvZGlvIGV0IGR1aSBtYXR0aXMsIGluIHJob25jdXMgbWF1cmlzIGhlbmRyZXJpdC4gTmFtIHZpdmVycm"
                + "EgbWF0dGlzIHJpc3VzIG5vbiB0cmlzdGlxdWUu"));
  }

  @ParameterizedTest
  @MethodSource("provideValuesForEncodeDecode")
  void testEncodeDecodeEncode(@NonNull String expectedDecodedString, @NonNull String expectedEncodedString) {
    assertAll(
        // test encode
        () -> assertEquals(
            expectedEncodedString,
            ObjectUtils.requireNonNull(
                ObjectUtils.requireNonNull((IBase64BinaryItem) IMetapathExpression
                    .compile(ObjectUtils.notNull(String.format("fn:base64-encode-text('%s')", expectedDecodedString)))
                    .evaluateAs(null, IMetapathExpression.ResultType.ITEM, newDynamicContext())))
                .asString(),
            "Expected same encoded text"),
        // test decode
        () -> assertEquals(
            expectedDecodedString,
            ObjectUtils.requireNonNull(
                (IStringItem) IMetapathExpression
                    .compile(ObjectUtils.notNull(
                        String.format("fn:base64-decode-text(meta:base64('%s'))", expectedEncodedString)))
                    .evaluateAs(null, IMetapathExpression.ResultType.ITEM, newDynamicContext()))
                .asString(),
            "Expected same decoded text"));
  }
}
