/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.datatype.adapter;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;

import gov.nist.secauto.metaschema.core.datatype.AbstractDataTypeAdapter;
import gov.nist.secauto.metaschema.core.metapath.MetapathConstants;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IIPv4AddressItem;
import gov.nist.secauto.metaschema.core.qname.EQNameFactory;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IPAddressStringParameters;
import inet.ipaddr.IncompatibleAddressException;
import inet.ipaddr.ipv4.IPv4Address;

/**
 * Support for the Metaschema <a href=
 * "https://pages.nist.gov/metaschema/specification/datatypes/#ip-v4-address">ip-v4-address</a>
 * data type.
 */
public class IPv4AddressAdapter
    extends AbstractDataTypeAdapter<IPv4Address, IIPv4AddressItem> {
  @NonNull
  private static final List<IEnhancedQName> NAMES = ObjectUtils.notNull(
      List.of(
          EQNameFactory.instance().newQName(MetapathConstants.NS_METAPATH, "ip-v4-address")));
  private static final IPAddressStringParameters IP_V_4;

  static {
    IP_V_4 = new IPAddressStringParameters.Builder()
        .allowIPv6(false)
        .allowEmpty(false)
        .allowSingleSegment(false)
        .allowWildcardedSeparator(false)
        .getIPv4AddressParametersBuilder()
        .allowBinary(false)
        .allowLeadingZeros(false)
        .allowPrefixesBeyondAddressSize(false)
        .getParentBuilder()
        .toParams();
  }

  IPv4AddressAdapter() {
    super(IPv4Address.class, IIPv4AddressItem.class, IIPv4AddressItem::cast);
  }

  @Override
  public List<IEnhancedQName> getNames() {
    return NAMES;
  }

  @Override
  public JsonFormatTypes getJsonRawType() {
    return JsonFormatTypes.STRING;
  }

  @SuppressWarnings("null")
  @Override
  public IPv4Address parse(String value) {
    try {
      return (IPv4Address) new IPAddressString(value, IP_V_4).toAddress();
    } catch (AddressStringException | IncompatibleAddressException ex) {
      throw new IllegalArgumentException(ex.getLocalizedMessage(), ex);
    }
  }

  @Override
  public IPv4Address copy(Object obj) {
    // value is immutable
    return (IPv4Address) obj;
  }

  @Override
  public IIPv4AddressItem newItem(Object value) {
    IPv4Address item = toValue(value);
    return IIPv4AddressItem.valueOf(item);
  }
}
