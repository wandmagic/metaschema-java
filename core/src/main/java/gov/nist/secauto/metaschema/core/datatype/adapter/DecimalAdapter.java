/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.datatype.adapter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;

import gov.nist.secauto.metaschema.core.datatype.AbstractDataTypeAdapter;
import gov.nist.secauto.metaschema.core.metapath.MetapathConstants;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IDecimalItem;
import gov.nist.secauto.metaschema.core.qname.EQNameFactory;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Support for the Metaschema <a href=
 * "https://pages.nist.gov/metaschema/specification/datatypes/#decimal">decimal</a>
 * data type.
 */
public class DecimalAdapter
    extends AbstractDataTypeAdapter<BigDecimal, IDecimalItem> {
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  @NonNull
  private static final List<IEnhancedQName> NAMES = ObjectUtils.notNull(
      List.of(
          EQNameFactory.instance().newQName(MetapathConstants.NS_METAPATH, "decimal")));

  DecimalAdapter() {
    super(BigDecimal.class, IDecimalItem.class, IDecimalItem::cast);
  }

  @Override
  public List<IEnhancedQName> getNames() {
    return NAMES;
  }

  @Override
  public JsonFormatTypes getJsonRawType() {
    return JsonFormatTypes.NUMBER;
  }

  @Override
  public BigDecimal parse(String value) {
    return new BigDecimal(value, MATH_CONTEXT);
  }

  @Override
  public void writeJsonValue(Object value, JsonGenerator generator) throws IOException {
    try {
      generator.writeNumber((BigDecimal) value);
    } catch (ClassCastException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public BigDecimal copy(Object obj) {
    // a BigDecimal is immutable
    return (BigDecimal) obj;
  }

  @Override
  public IDecimalItem newItem(Object value) {
    BigDecimal item = toValue(value);
    return IDecimalItem.valueOf(item);
  }
}
