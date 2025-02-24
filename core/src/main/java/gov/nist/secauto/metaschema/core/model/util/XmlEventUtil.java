/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.model.util;

import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import org.codehaus.stax2.XMLEventReader2;
import org.codehaus.stax2.XMLStreamReader2;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Provides utility functions to support reading and writing XML stream events,
 * and for producing error and warning messages.
 */
public final class XmlEventUtil { // NOPMD this is a set of utility methods
  private static final Pattern WHITESPACE_ONLY = Pattern.compile("^\\s+$");

  private static final Map<Integer, String> EVENT_NAME_MAP = new HashMap<>(); // NOPMD - this value is immutable

  static {
    EVENT_NAME_MAP.put(XMLStreamConstants.START_ELEMENT, "START_ELEMENT");
    EVENT_NAME_MAP.put(XMLStreamConstants.END_ELEMENT, "END_ELEMENT");
    EVENT_NAME_MAP.put(XMLStreamConstants.PROCESSING_INSTRUCTION, "PROCESSING_INSTRUCTION");
    EVENT_NAME_MAP.put(XMLStreamConstants.CHARACTERS, "CHARACTERS");
    EVENT_NAME_MAP.put(XMLStreamConstants.COMMENT, "COMMENT");
    EVENT_NAME_MAP.put(XMLStreamConstants.SPACE, "SPACE");
    EVENT_NAME_MAP.put(XMLStreamConstants.START_DOCUMENT, "START_DOCUMENT");
    EVENT_NAME_MAP.put(XMLStreamConstants.END_DOCUMENT, "END_DOCUMENT");
    EVENT_NAME_MAP.put(XMLStreamConstants.ENTITY_REFERENCE, "ENTITY_REFERENCE");
    EVENT_NAME_MAP.put(XMLStreamConstants.ATTRIBUTE, "ATTRIBUTE");
    EVENT_NAME_MAP.put(XMLStreamConstants.DTD, "DTD");
    EVENT_NAME_MAP.put(XMLStreamConstants.CDATA, "CDATA");
    EVENT_NAME_MAP.put(XMLStreamConstants.NAMESPACE, "XML_NAMESPACE");
    EVENT_NAME_MAP.put(XMLStreamConstants.NOTATION_DECLARATION, "NOTATION_DECLARATION");
    EVENT_NAME_MAP.put(XMLStreamConstants.ENTITY_DECLARATION, "ENTITY_DECLARATION");
  }

  private XmlEventUtil() {
    // disable construction
  }

  @SuppressWarnings("null")
  @NonNull
  private static Object escape(@NonNull String data) {
    return data.chars()
        .mapToObj(c -> (char) c)
        .map(XmlEventUtil::escape).collect(Collectors.joining());
  }

  @SuppressWarnings("null")
  @NonNull
  private static String escape(char ch) {
    String retval;
    switch (ch) {
    case '\n':
      retval = "\\n";
      break;
    case '\r':
      retval = "\\r";
      break;
    default:
      retval = String.valueOf(ch);
      break;
    }
    return retval;
  }

  /**
   * Generate a message suitable for logging that describes the provided
   * {@link XMLEvent}.
   *
   * @param xmlEvent
   *          the XML event to generate the message for
   * @param resource
   *          the resource being parsed
   * @return the message
   */
  @NonNull
  public static CharSequence toString(
      @Nullable XMLEvent xmlEvent,
      @NonNull URI resource) {
    CharSequence retval;
    if (xmlEvent == null) {
      retval = "EOF";
    } else {
      @SuppressWarnings("null")
      @NonNull
      StringBuilder builder = new StringBuilder()
          .append(toEventName(xmlEvent));
      QName name = toQName(xmlEvent);
      if (name != null) {
        builder.append(": ").append(name.toString());
      }
      if (xmlEvent.isCharacters()) {
        String text = xmlEvent.asCharacters().getData();
        if (text != null) {
          builder.append(" '").append(escape(text)).append('\'');
        }
      }
      Location location = toLocation(xmlEvent);
      if (location != null) {
        builder.append(" at ").append(toString(location, resource));
      }
      retval = builder;
    }
    return retval;
  }

  /**
   * Generates a message for the provided {@link Location}.
   *
   * @param location
   *          the location to generate the message for
   * @param resource
   *          the resource being parsed
   * @return the message
   */
  @SuppressWarnings("null")
  @NonNull
  public static CharSequence toString(@Nullable Location location, @NonNull URI resource) {
    return location == null ? "unknown"
        : new StringBuilder()
            .append(location.getLineNumber())
            .append(':')
            .append(location.getColumnNumber());
  }

  /**
   * Generates a string containing the current event and location of the stream
   * reader.
   *
   * @param reader
   *          the XML event stream reader
   * @param resource
   *          the resource being parsed
   * @return the generated string
   */
  @NonNull
  public static CharSequence toString(
      @NonNull XMLStreamReader2 reader,
      @NonNull URI resource) {
    int type = reader.getEventType();

    @SuppressWarnings("null")
    @NonNull
    StringBuilder builder = new StringBuilder().append(toEventName(type));
    QName name = reader.getName();
    if (name != null) {
      builder.append(": ").append(name.toString());
    }
    if (XMLStreamConstants.CHARACTERS == type) {
      String text = reader.getText();
      if (text != null) {
        builder.append(" '").append(escape(text)).append('\'');
      }
    }
    Location location = reader.getLocation();
    if (location != null) {
      builder.append(" at ").append(toString(location, resource));
    }
    return builder;
  }

  /**
   * Retrieve the resource location of {@code event}.
   *
   * @param event
   *          the XML event to identify the location for
   * @return the location or {@code null} if the location is unknown
   */
  @Nullable
  public static Location toLocation(@NonNull XMLEvent event) {
    Location retval = null;
    if (event.isStartElement()) {
      StartElement start = event.asStartElement();
      retval = start.getLocation();
    } else if (event.isEndElement()) {
      EndElement end = event.asEndElement();
      retval = end.getLocation();
    } else if (event.isCharacters()) {
      Characters characters = event.asCharacters();
      retval = characters.getLocation();
    }
    return retval;
  }

  /**
   * Retrieve the name of the node associated with {@code event}.
   *
   * @param event
   *          the XML event to get the {@link QName} for
   * @return the name of the node or {@code null} if the event is not a start or
   *         end element
   */
  @Nullable
  public static QName toQName(@NonNull XMLEvent event) {
    QName retval = null;
    if (event.isStartElement()) {
      StartElement start = event.asStartElement();
      retval = start.getName();
    } else if (event.isEndElement()) {
      EndElement end = event.asEndElement();
      retval = end.getName();
    }
    return retval;
  }

  /**
   * Get the event name of the {@code event}.
   *
   * @param event
   *          the XML event to get the event name for
   * @return the event name
   */
  @NonNull
  public static String toEventName(@NonNull XMLEvent event) {
    return toEventName(event.getEventType());
  }

  /**
   * Get the event name of the {@code eventType}, which is one of the types
   * defined by {@link XMLStreamConstants}.
   *
   * @param eventType
   *          the event constant to get the event name for as defined by
   *          {@link XMLStreamConstants}
   * @return the event name
   */
  @NonNull
  public static String toEventName(int eventType) {
    String retval = EVENT_NAME_MAP.get(eventType);
    if (retval == null) {
      retval = "unknown event '" + Integer.toString(eventType) + "'";
    }
    return retval;
  }

  /**
   * Advance through XMLEvents until the event type identified by
   * {@code eventType} is reached or the end of stream is found.
   *
   * @param reader
   *          the XML event reader to advance
   * @param eventType
   *          the event type to stop on as defined by {@link XMLStreamConstants}
   * @return the next event of the specified type or {@code null} if the end of
   *         stream is reached
   * @throws XMLStreamException
   *           if an error occurred while advancing the stream
   */
  @Nullable
  public static XMLEvent advanceTo(@NonNull XMLEventReader2 reader, int eventType)
      throws XMLStreamException { // NO_UCD (unused code)
    XMLEvent xmlEvent;
    do {
      xmlEvent = reader.nextEvent();
      // if (LOGGER.isWarnEnabled()) {
      // LOGGER.warn("skipping over: {}", XmlEventUtil.toString(xmlEvent));
      // }
      if (xmlEvent.isStartElement()) {
        advanceTo(reader, XMLStreamConstants.END_ELEMENT);
        // skip this end element
        xmlEvent = reader.nextEvent();
        // if (LOGGER.isDebugEnabled()) {
        // LOGGER.debug("skipping over: {}", XmlEventUtil.toString(xmlEvent));
        // }
      }
    } while (reader.hasNext() && (xmlEvent = reader.peek()).getEventType() != eventType);
    return xmlEvent;
  }

  /**
   * Skip over the next element in the event stream.
   *
   * @param reader
   *          the XML event stream reader
   * @return the next XML event
   * @throws XMLStreamException
   *           if an error occurred while reading the event stream
   */
  @SuppressWarnings("PMD.OnlyOneReturn")
  public static XMLEvent skipElement(@NonNull XMLEventReader2 reader) throws XMLStreamException {
    XMLEvent xmlEvent = reader.peek();
    if (!xmlEvent.isStartElement()) {
      return xmlEvent;
    }
    // if (LOGGER.isInfoEnabled()) {
    // LOGGER.atInfo().log(String.format("At location %s", toString(xmlEvent)));
    // }

    int depth = 0;
    do {
      xmlEvent = reader.nextEvent();
      // if (LOGGER.isInfoEnabled()) {
      // LOGGER.atInfo().log(String.format("Skipping %s", toString(xmlEvent)));
      // }
      if (xmlEvent.isStartElement()) {
        depth++;
      } else if (xmlEvent.isEndElement()) {
        depth--;
      }
    } while (depth > 0 && reader.hasNext());
    return reader.peek();
  }

  /**
   * Skip over any processing instructions.
   *
   * @param reader
   *          the XML event reader to advance
   * @return the last processing instruction event or the reader's next event if
   *         no processing instruction was found
   * @throws XMLStreamException
   *           if an error occurred while advancing the stream
   */
  @NonNull
  public static XMLEvent skipProcessingInstructions(@NonNull XMLEventReader2 reader) throws XMLStreamException {
    XMLEvent nextEvent;
    while ((nextEvent = reader.peek()).isProcessingInstruction()) {
      nextEvent = reader.nextEvent();
    }
    return nextEvent;
  }

  /**
   * Skip over any whitespace.
   *
   * @param reader
   *          the XML event reader to advance
   * @return the last character event containing whitespace or the reader's next
   *         event if no character event was found
   * @throws XMLStreamException
   *           if an error occurred while advancing the stream
   */
  @SuppressWarnings("null")
  @NonNull
  public static XMLEvent skipWhitespace(@NonNull XMLEventReader2 reader) throws XMLStreamException {
    @NonNull
    XMLEvent nextEvent;
    while ((nextEvent = reader.peek()).isCharacters()) {
      Characters characters = nextEvent.asCharacters();
      String data = characters.getData();
      if (!WHITESPACE_ONLY.matcher(data).matches()) {
        break;
      }
      nextEvent = reader.nextEvent();
    }
    return nextEvent;
  }

  /**
   * Determine if the {@code event} is an end element whose name matches the
   * provided {@code expectedQName}.
   *
   * @param event
   *          the XML event
   * @param expectedQName
   *          the expected element name
   * @return {@code true} if the next event matches the {@code expectedQName}
   */
  public static boolean isEventEndElement(@Nullable XMLEvent event, @NonNull QName expectedQName) {
    return event != null
        && event.isEndElement()
        && expectedQName.equals(event.asEndElement().getName());
  }

  /**
   * Determine if the {@code event} is an end of document event.
   *
   * @param event
   *          the XML event
   * @return {@code true} if the next event is an end of document event
   */
  public static boolean isEventEndDocument(@Nullable XMLEvent event) {
    return event != null
        && event.isEndElement();
  }

  /**
   * Determine if the {@code event} is a start element whose name matches the
   * provided {@code expectedQName}.
   *
   * @param event
   *          the event
   * @param expectedQName
   *          the expected element name
   * @return {@code true} if the next event is a start element that matches the
   *         {@code expectedQName}
   * @throws XMLStreamException
   *           if an error occurred while looking at the next event
   */
  public static boolean isEventStartElement(
      @Nullable XMLEvent event,
      @NonNull QName expectedQName) throws XMLStreamException {
    return event != null
        && event.isStartElement()
        && expectedQName.equals(event.asStartElement().getName());
  }

  /**
   * Consume the next event from {@code reader} and assert that this event is of
   * the type identified by {@code presumedEventType}.
   *
   * @param reader
   *          the XML event reader
   * @param resource
   *          the resource being parsed
   * @param presumedEventType
   *          the expected event type as defined by {@link XMLStreamConstants}
   * @return the next event
   * @throws XMLStreamException
   *           if an error occurred while looking at the next event
   */
  public static XMLEvent consumeAndAssert(
      @NonNull XMLEventReader2 reader,
      @NonNull URI resource,
      int presumedEventType)
      throws XMLStreamException {
    return consumeAndAssert(reader, resource, presumedEventType, null);
  }

  /**
   * Consume the next event from {@code reader} and assert that this event is of
   * the type identified by {@code presumedEventType} and has the name identified
   * by {@code presumedName}.
   *
   * @param reader
   *          the XML event reader
   * @param resource
   *          the resource being parsed
   * @param presumedEventType
   *          the expected event type as defined by {@link XMLStreamConstants}
   * @param presumedName
   *          the expected name of the node associated with the event
   * @return the next event
   * @throws XMLStreamException
   *           if an error occurred while looking at the next event
   */
  public static XMLEvent consumeAndAssert(
      @NonNull XMLEventReader2 reader,
      @NonNull URI resource,
      int presumedEventType,
      @Nullable QName presumedName)
      throws XMLStreamException {
    XMLEvent retval = reader.nextEvent();

    int eventType = retval.getEventType();
    QName name = toQName(retval);
    assert eventType == presumedEventType
        && (presumedName == null
            || presumedName.equals(name)) : generateExpectedMessage(
                retval,
                resource,
                presumedEventType,
                presumedName);
    return retval;
  }

  /**
   * Ensure that the next event is an XML start element that matches the presumed
   * name.
   *
   * @param reader
   *          the XML event reader
   * @param resource
   *          the resource being parsed
   * @param presumedName
   *          the qualified name of the expected next event
   * @return the XML start element event
   * @throws IOException
   *           if an error occurred while parsing the resource
   * @throws XMLStreamException
   *           if an error occurred while parsing the XML event stream
   */
  @NonNull
  public static StartElement requireStartElement(
      @NonNull XMLEventReader2 reader,
      @NonNull URI resource,
      @NonNull QName presumedName) throws IOException, XMLStreamException {
    XMLEvent retval = reader.nextEvent();
    if (!retval.isStartElement()
        || !presumedName.equals(retval.asStartElement().getName())) {
      throw new IOException(generateExpectedMessage(
          retval,
          resource,
          XMLStreamConstants.START_ELEMENT,
          presumedName).toString());
    }
    return ObjectUtils.notNull(retval.asStartElement());
  }

  /**
   * Ensure that the next event is an XML start element that matches the presumed
   * name.
   *
   * @param reader
   *          the XML event reader
   * @param resource
   *          the resource being parsed
   * @param presumedName
   *          the qualified name of the expected next event
   * @return the XML start element event
   * @throws IOException
   *           if an error occurred while parsing the resource
   * @throws XMLStreamException
   *           if an error occurred while parsing the XML event stream
   */
  @NonNull
  public static EndElement requireEndElement(
      @NonNull XMLEventReader2 reader,
      @NonNull URI resource,
      @NonNull QName presumedName) throws IOException, XMLStreamException {
    XMLEvent retval = reader.nextEvent();
    if (!retval.isEndElement()
        || !presumedName.equals(retval.asEndElement().getName())) {
      throw new IOException(generateExpectedMessage(
          retval,
          resource,
          XMLStreamConstants.END_ELEMENT,
          presumedName).toString());
    }
    return ObjectUtils.notNull(retval.asEndElement());
  }

  /**
   * Ensure that the next event from {@code reader} is of the type identified by
   * {@code presumedEventType}.
   *
   * @param reader
   *          the event reader
   * @param resource
   *          the resource being parsed
   * @param presumedEventType
   *          the expected event type as defined by {@link XMLStreamConstants}
   * @return the next event
   * @throws XMLStreamException
   *           if an error occurred while looking at the next event
   * @throws AssertionError
   *           if the next event does not match the presumed event
   */
  public static XMLEvent assertNext(
      @NonNull XMLEventReader2 reader,
      @NonNull URI resource,
      int presumedEventType)
      throws XMLStreamException {
    return assertNext(reader, resource, presumedEventType, null);
  }

  /**
   * Ensure that the next event from {@code reader} is of the type identified by
   * {@code presumedEventType} and has the name identified by
   * {@code presumedName}.
   *
   * @param reader
   *          the event reader
   * @param resource
   *          the resource being parsed
   * @param presumedEventType
   *          the expected event type as defined by {@link XMLStreamConstants}
   * @param presumedName
   *          the expected name of the node associated with the event
   * @return the next event
   * @throws XMLStreamException
   *           if an error occurred while looking at the next event
   * @throws AssertionError
   *           if the next event does not match the presumed event
   */
  public static XMLEvent assertNext(
      @NonNull XMLEventReader2 reader,
      @NonNull URI resource,
      int presumedEventType,
      @Nullable QName presumedName)
      throws XMLStreamException {
    XMLEvent nextEvent = reader.peek();

    int eventType = nextEvent.getEventType();
    assert eventType == presumedEventType
        && (presumedName == null
            || presumedName.equals(toQName(nextEvent))) : generateExpectedMessage(
                nextEvent,
                resource,
                presumedEventType,
                presumedName);
    return nextEvent;
  }

  /**
   * Generate a location string for the current location in the XML event stream.
   *
   * @param event
   *          an XML event
   * @param resource
   *          the resource being parsed
   * @return the location string
   */
  public static CharSequence generateLocationMessage(
      @Nullable XMLEvent event,
      @NonNull URI resource) {
    Location location = event == null ? null : toLocation(event);
    return location == null
        ? " at '" + resource.toString() + "'"
        : generateLocationMessage(location, resource);
  }

  /**
   * Generate a location string for the current location in the XML event stream.
   *
   * @param location
   *          an XML event stream location
   * @param resource
   *          the resource being parsed
   * @return the location string
   */
  public static CharSequence generateLocationMessage(
      @NonNull Location location,
      @NonNull URI resource) {
    return new StringBuilder(12)
        .append(" in '")
        .append(resource.toString())
        .append("' at ")
        .append(XmlEventUtil.toString(location, resource));
  }

  /**
   * Generate a message intended for error reporting based on a presumed event.
   *
   * @param event
   *          the current XML event
   * @param resource
   *          the resource being parsed
   * @param presumedEventType
   *          the expected event type ({@link XMLEvent#getEventType()})
   * @param presumedName
   *          the expected event qualified name or {@code null} if there is no
   *          expectation
   * @return the message string
   */
  public static CharSequence generateExpectedMessage(
      @Nullable XMLEvent event,
      @NonNull URI resource,
      int presumedEventType,
      @Nullable QName presumedName) {
    StringBuilder builder = new StringBuilder(64);
    builder
        .append("Expected XML ")
        .append(toEventName(presumedEventType));

    if (presumedName != null) {
      builder.append(" for QName '")
          .append(presumedName.toString());
    }

    if (event == null) {
      builder.append("', instead found null event");
    } else {
      builder.append("', instead found ")
          .append(toString(event, resource));
    }
    return builder;
  }

  /**
   * Skips events specified by {@code events}.
   *
   * @param reader
   *          the event reader
   * @param events
   *          the events to skip
   * @return the next non-mataching event returned by
   *         {@link XMLEventReader2#peek()}, or {@code null} if there was no next
   *         event
   * @throws XMLStreamException
   *           if an error occurred while reading
   */
  public static XMLEvent skipEvents(XMLEventReader2 reader, int... events) throws XMLStreamException {
    Set<Integer> skipEvents = IntStream.of(events).boxed().collect(Collectors.toSet());

    XMLEvent nextEvent = null;
    while (reader.hasNext()) {
      nextEvent = reader.peek();
      if (!skipEvents.contains(nextEvent.getEventType())) {
        break;
      }
      reader.nextEvent();
    }
    return nextEvent;
  }
}
