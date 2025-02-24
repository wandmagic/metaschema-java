/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.item.atomic.impl;

import gov.nist.secauto.metaschema.core.datatype.markup.IMarkupString;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.AbstractAnyAtomicItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IMarkupItem;
import gov.nist.secauto.metaschema.core.metapath.item.function.IMapKey;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An abstract implementation of a Metapath atomic item representing a
 * markup-based data value.
 *
 * @param <TYPE>
 *          the Java type of this markup item
 */
public abstract class AbstractMarkupItem<TYPE extends IMarkupString<TYPE>>
    extends AbstractAnyAtomicItem<TYPE>
    implements IMarkupItem {

  /**
   * Construct a new item.
   *
   * @param value
   *          the item's data value
   */
  protected AbstractMarkupItem(@NonNull TYPE value) {
    super(value);
  }

  @Override
  public IMarkupString<TYPE> asMarkup() {
    return getValue();
  }

  @Override
  public int hashCode() {
    return asMarkup().hashCode();
  }

  @Override
  protected String getValueSignature() {
    return "'" + asString() + "'";
  }

  @SuppressWarnings("PMD.OnlyOneReturn")
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof IMarkupItem && compareTo((IMarkupItem) obj) == 0;
  }

  @Override
  public IMapKey asMapKey() {
    return new MapKey();
  }

  private final class MapKey implements IMapKey {
    @Override
    public IMarkupItem getKey() {
      return AbstractMarkupItem.this;
    }

    @Override
    public int hashCode() {
      return getKey().asString().hashCode();
    }

    @SuppressWarnings("PMD.OnlyOneReturn")
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof AbstractMarkupItem.MapKey)) {
        return false;
      }

      AbstractMarkupItem<?>.MapKey other = (AbstractMarkupItem<?>.MapKey) obj;
      return getKey().compareTo(other.getKey()) == 0;
    }
  }
}
