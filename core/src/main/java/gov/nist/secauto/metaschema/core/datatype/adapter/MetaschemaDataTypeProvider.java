/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.datatype.adapter;

import gov.nist.secauto.metaschema.core.datatype.AbstractDataTypeProvider;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Provides runtime discovery of built-in implementations of the core Metaschema
 * data types.
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class MetaschemaDataTypeProvider // NOPMD - Used for service initialization
    extends AbstractDataTypeProvider {
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#base64">base64</a>
   * data type instance.
   */
  @NonNull
  public static final Base64Adapter BASE64 = new Base64Adapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#boolean">boolean</a>
   * data type instance.
   */
  @NonNull
  public static final BooleanAdapter BOOLEAN = new BooleanAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#date">date</a>
   * data type instance.
   */
  @NonNull
  public static final DateAdapter DATE = new DateAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#date-with-timezone">date-with-timezone</a>
   * data type instance.
   */
  @NonNull
  public static final DateWithTZAdapter DATE_WITH_TZ = new DateWithTZAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#date-time">date-time</a>
   * data type instance.
   */
  @NonNull
  public static final DateTimeAdapter DATE_TIME = new DateTimeAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#date-time-with-timezone">date-time-with-timezone</a>
   * data type instance.
   */
  @NonNull
  public static final DateTimeWithTZAdapter DATE_TIME_WITH_TZ = new DateTimeWithTZAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#ip-v4-address">ip-v4-address</a>
   * data type instance.
   */
  @NonNull
  public static final IPv4AddressAdapter IP_V4_ADDRESS = new IPv4AddressAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#ip-v6-address">ip-v6-address</a>
   * data type instance.
   */
  @NonNull
  public static final IPv6AddressAdapter IP_V6_ADDRESS = new IPv6AddressAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#uri">uri</a> data
   * type instance.
   */
  @NonNull
  public static final UriAdapter URI = new UriAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#uri-reference">uri-reference</a>
   * data type instance.
   */
  @NonNull
  public static final UriReferenceAdapter URI_REFERENCE = new UriReferenceAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#uuid">uuid</a>
   * data type instance.
   */
  @NonNull
  public static final UuidAdapter UUID = new UuidAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#day-time-duration">day-time-duration</a>
   * data type instance.
   */
  @NonNull
  public static final DayTimeAdapter DAY_TIME_DURATION = new DayTimeAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#year-month-duration">year-month-duration</a>
   * data type instance.
   */
  @NonNull
  public static final YearMonthAdapter YEAR_MONTH_DURATION = new YearMonthAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#decimal">decimal</a>
   * data type instance.
   */
  @NonNull
  public static final DecimalAdapter DECIMAL = new DecimalAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#integer">integer</a>
   * data type instance.
   */
  @NonNull
  public static final IntegerAdapter INTEGER = new IntegerAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#non-negative-integer">non-negative-integer</a>
   * data type instance.
   */
  @NonNull
  public static final NonNegativeIntegerAdapter NON_NEGATIVE_INTEGER = new NonNegativeIntegerAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#positive-integer">positive-integer</a>
   * data type instance.
   */
  @NonNull
  public static final PositiveIntegerAdapter POSITIVE_INTEGER = new PositiveIntegerAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#email-address">email-address</a>
   * data type instance.
   */
  @NonNull
  public static final EmailAddressAdapter EMAIL_ADDRESS = new EmailAddressAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#hostname">hostname</a>
   * data type instance.
   */
  @NonNull
  public static final HostnameAdapter HOSTNAME = new HostnameAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#ncname">ncname</a>
   * data type instance.
   */
  @Deprecated(since = "0.7.0")
  @NonNull
  public static final NcNameAdapter NCNAME = new NcNameAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#string">string</a>
   * data type instance.
   */
  @NonNull
  public static final StringAdapter STRING = new StringAdapter();
  /**
   * The Metaschema <a href=
   * "https://pages.nist.gov/metaschema/specification/datatypes/#token">token</a>
   * data type instance.
   */
  @NonNull
  public static final TokenAdapter TOKEN = new TokenAdapter();
  /**
   * The default Metaschema data type instance to use when no data type is defined
   * on a field or flag.
   */
  @NonNull
  public static final StringAdapter DEFAULT_DATA_TYPE = STRING;

  /**
   * Initialize the built-in data types.
   */
  public MetaschemaDataTypeProvider() {
    // The data type "string" must be first since this is the default data type for
    // the {@link String}
    // Java type. This ensures that when a data type is resolved that this data type
    // is matched first
    // before other String-based data types.
    registerDatatype(STRING);

    registerDatatype(BASE64);
    registerDatatype(BOOLEAN);
    registerDatatype(DATE);
    registerDatatype(DATE_WITH_TZ);
    registerDatatype(DATE_TIME);
    registerDatatype(DATE_TIME_WITH_TZ);
    registerDatatype(DAY_TIME_DURATION);
    registerDatatype(DECIMAL);
    registerDatatype(EMAIL_ADDRESS);
    registerDatatype(HOSTNAME);
    registerDatatype(INTEGER);
    registerDatatype(IP_V4_ADDRESS);
    registerDatatype(IP_V6_ADDRESS);

    registerDatatype(NON_NEGATIVE_INTEGER);
    registerDatatype(POSITIVE_INTEGER);
    registerDatatype(TOKEN);
    registerDatatype(URI);
    registerDatatype(URI_REFERENCE);
    registerDatatype(UUID);
    registerDatatype(YEAR_MONTH_DURATION);
  }
}
