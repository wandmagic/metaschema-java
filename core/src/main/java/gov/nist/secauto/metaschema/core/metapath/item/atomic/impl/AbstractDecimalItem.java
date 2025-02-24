/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.item.atomic.impl;

import gov.nist.secauto.metaschema.core.metapath.item.atomic.AbstractAnyAtomicItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IDecimalItem;
import gov.nist.secauto.metaschema.core.metapath.item.function.IMapKey;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An abstract implementation of a Metapath atomic item containing a decimal
 * data value.
 *
 * @param <TYPE>
 *          the Java type of the wrapped value
 */
public abstract class AbstractDecimalItem<TYPE>
    extends AbstractAnyAtomicItem<TYPE>
    implements IDecimalItem {

  /**
   * Construct a new item with the provided {@code value}.
   *
   * @param value
   *          the value to wrap
   */
  protected AbstractDecimalItem(@NonNull TYPE value) {
    super(value);
  }

  @Override
  protected String getValueSignature() {
    return asString();
  }

  @Override
  public IMapKey asMapKey() {
    return new MapKey();
  }

  private final class MapKey
      implements IMapKey {

    @Override
    public IDecimalItem getKey() {
      return AbstractDecimalItem.this;
    }

    @Override
    public int hashCode() {
      return getKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj ||
          obj instanceof AbstractDecimalItem.MapKey
              && getKey().asDecimal().equals(((AbstractDecimalItem<?>.MapKey) obj).getKey().asDecimal());
    }
  }
}
