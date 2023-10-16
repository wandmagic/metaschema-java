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

package gov.nist.secauto.metaschema.core.metapath.item;

import gov.nist.secauto.metaschema.core.metapath.TypeMetapathException;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public final class ItemUtils {

  private ItemUtils() {
    // disable construction
  }

  /**
   * Checks that the item is a node item.
   *
   * @param item
   *          the item to check
   * @return the item cast to a {@link INodeItem}
   * @throws TypeMetapathException
   *           if the item is {@code null} or not an {@link INodeItem}
   */
  @NonNull
  public static INodeItem checkItemIsNodeItemForStep(@Nullable IItem item) {
    if (item instanceof INodeItem) {
      return (INodeItem) item;
    }
    if (item == null) {
      throw new TypeMetapathException(TypeMetapathException.NOT_A_NODE_ITEM_FOR_STEP,
          "Item is null.");
    }
    throw new TypeMetapathException(TypeMetapathException.NOT_A_NODE_ITEM_FOR_STEP,
        String.format(
            "The item of type '%s' is not a INodeItem.",
            item.getClass().getName()));
  }

  /**
   * Check that the item is the type specified by {@code clazz}.
   *
   * @param <TYPE>
   *          the Java type the item is required to match
   * @param item
   *          the item to check
   * @param clazz
   *          the Java class to check the item against
   * @return the item cast to the required class value
   * @throws TypeMetapathException
   *           if the item is {@code null} or does not match the type specified by
   *           {@code clazz}
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static <TYPE> TYPE checkItemType(@NonNull IItem item, @NonNull Class<TYPE> clazz) {
    if (clazz.isInstance(item)) {
      return (TYPE) item;
    }
    throw new TypeMetapathException(TypeMetapathException.INVALID_TYPE_ERROR,
        String.format(
            "The item of type '%s' is not the required type '%s'.",
            item.getClass().getName(),
            clazz.getName()));
  }
}
