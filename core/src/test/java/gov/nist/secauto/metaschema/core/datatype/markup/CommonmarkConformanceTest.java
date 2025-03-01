/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.datatype.markup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.secauto.metaschema.core.MetaschemaConstants;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import org.codehaus.stax2.XMLStreamWriter2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

@SuppressWarnings("PMD.CouplingBetweenObjects")
class CommonmarkConformanceTest {
  private static final String SCHEMA_PATH = "src/test/resources/markup-test.xsd";
  private static final Pattern INITIAL_ELEMENT_PATTERN
      = Pattern.compile("^\\s*<([^\\s/>]+)[^>]*>.*", Pattern.DOTALL);

  private static final Pattern QUOTE_TAG_REPLACEMENT_PATTERN
      = Pattern.compile("</?q>");

  @Disabled
  @Test
  void test() {
    String vector = "<p><a href=\"foo  \r\n"
        + "bar\"></a></p><a href=\"foo  \r\n"
        + "bar\"> </a>";

    Matcher matcher = INITIAL_ELEMENT_PATTERN.matcher(vector);

    assertTrue(matcher.matches());
    assertTrue(XmlMarkupParser.XHTML_BLOCK_ELEMENTS.contains(matcher.group(1)));
  }

  private static List<Entry> generateTestVectors() throws JsonParseException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    // mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    try (Reader reader = Files.newBufferedReader(
        Paths.get("src/test/resources/commonmark-spec.json"),
        StandardCharsets.UTF_8)) {
      try (JsonParser parser = mapper.getFactory().createParser(reader)) {
        if (parser.nextToken() != JsonToken.START_ARRAY) {
          throw new IllegalStateException();
        }

        List<Entry> entries = new LinkedList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
          Entry entry = mapper
              .readerFor(Entry.class)
              .readValue(parser);
          entries.add(entry);
        }
        return entries;
      }
    }
  }

  private static Schema loadDataTypeSchema() throws SAXException, IOException {
    Path schemaPath = Paths.get(SCHEMA_PATH).toAbsolutePath();
    SchemaFactory schemafactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    return schemafactory.newSchema(schemaPath.toUri().toURL());
  }

  public boolean isMultilineMarkdown(@NonNull String markdown) {
    return markdown.trim().contains("\n");
  }

  public boolean isBlockElement(@NonNull String html) {
    Matcher matcher = INITIAL_ELEMENT_PATTERN.matcher(html);

    return matcher.matches() && XmlMarkupParser.XHTML_BLOCK_ELEMENTS.contains(matcher.group(1));
  }

  @NonNull
  public String generateXmlInstance(@NonNull String html) {

    StringBuilder builder = new StringBuilder(206);
    builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"); // NOPMD

    String topLevelElementName;
    if (isBlockElement(html)) {
      topLevelElementName = "multiline";
    } else {
      topLevelElementName = "line";
    }

    builder
        .append(
            "<!DOCTYPE " + topLevelElementName + " [\r\n" // NOPMD
                + "<!ENTITY nbsp \"&#160;\">\r\n"
                + "]>\r\n")
        .append('<')
        .append(topLevelElementName)
        .append(" xmlns=\"" + MetaschemaConstants.METASCHEMA_NAMESPACE + "\"") // NOPMD
        .append(" xmlns:zoop=\"http://csrc.nist.gov/ns/oscal/metaschema-zoop/1.0\"")
        .append('>')
        .append(html)
        .append("</")
        .append(topLevelElementName)
        .append(">\r\n");

    return ObjectUtils.notNull(builder.toString());
  }

  @NonNull
  public String generateXmlInstance(@NonNull IMarkupString<?> content) throws XMLStreamException {

    WstxOutputFactory factory = new WstxOutputFactory();
    factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    StringWriter strWriter = new StringWriter();

    XMLStreamWriter2 xmlWriter = (XMLStreamWriter2) factory.createXMLStreamWriter(strWriter);
    xmlWriter.writeStartDocument();

    String topLevelElementName;
    if (content instanceof MarkupMultiline) {
      topLevelElementName = "multiline";
    } else {
      topLevelElementName = "line";
    }

    xmlWriter.writeDTD("<!DOCTYPE " + topLevelElementName + " [\r\n" // NOPMD
        + "<!ENTITY nbsp \"&#160;\">\r\n"
        + "]>\r\n");

    xmlWriter.setPrefix("", MetaschemaConstants.METASCHEMA_NAMESPACE);
    xmlWriter.writeStartElement(MetaschemaConstants.METASCHEMA_NAMESPACE, topLevelElementName);
    xmlWriter.writeNamespace("", MetaschemaConstants.METASCHEMA_NAMESPACE);
    xmlWriter.writeNamespace("zoop", "http://csrc.nist.gov/ns/oscal/metaschema-zoop/1.0");

    content.writeXHtml(MetaschemaConstants.METASCHEMA_NAMESPACE, xmlWriter);

    xmlWriter.writeEndElement();
    xmlWriter.writeEndDocument();
    xmlWriter.flush();

    return ObjectUtils.notNull(strWriter.toString());
  }

  @Execution(ExecutionMode.SAME_THREAD)
  @DisplayName("Markup Tests")
  @TestFactory
  Stream<DynamicNode> generateConversionTests() // NOPMD
      throws IOException, SAXException {
    Schema schema = loadDataTypeSchema();

    List<Entry> entries = generateTestVectors();

    return entries.stream()
        .filter(Entry::isEnabled)
        .collect(Collectors.groupingBy(
            Entry::getSection,
            Collectors.toList()))
        .entrySet().stream()
        .map(entry -> {
          String sectionName = entry.getKey();
          List<Entry> testVectors = entry.getValue();
          return DynamicContainer.dynamicContainer(
              sectionName,
              testVectors.stream()
                  .map(testVector -> {
                    String testName = new StringBuilder()
                        .append(testVector.getSection())
                        .append(" (")
                        .append(testVector.getExampleNumber())
                        .append(')')
                        .toString();
                    return DynamicContainer.dynamicContainer(
                        testName,
                        Stream.of(
                            DynamicTest.dynamicTest(
                                testVector.getMarkdown(),
                                () -> {
                                  // do nothing
                                }),
                            DynamicContainer.dynamicContainer(
                                "Markdown To XML/HTML Conversion",
                                Stream.of(
                                    DynamicTest.dynamicTest(
                                        "Validate Markdown To XML/HTML",
                                        () -> {
                                          String markdown = testVector.getMarkdown();
                                          AbstractMarkupString<?> content
                                              = MarkupDataTypeProvider.MARKUP_MULTILINE.parse(markdown);

                                          String convertedHtmlInstance = generateXmlInstance(content);

                                          // validate the produced content
                                          StringReader reader = new StringReader(convertedHtmlInstance);
                                          StreamSource source = new StreamSource(reader);
                                          Validator validator = schema.newValidator();
                                          validator.setErrorHandler(new Handler());
                                          try {
                                            validator.validate(source);
                                          } catch (SAXParseException ex) {
                                            fail(String.format("Invalid content '%s'. %s",
                                                content,
                                                ex.getLocalizedMessage()));
                                          }
                                        }),
                                    DynamicTest.dynamicTest(
                                        "Convert Markdown to HTML/XHTML and Match with Test Vector",
                                        () -> {
                                          String markdown = testVector.getMarkdown();
                                          IMarkupString<?> content
                                              = MarkupDataTypeProvider.MARKUP_MULTILINE.parse(markdown);
                                          String convertedHtmlInstance = generateXmlInstance(content);

                                          // extract the generated HTML
                                          String topLevelElementName = "multiline";
                                          convertedHtmlInstance = convertedHtmlInstance.substring(
                                              convertedHtmlInstance.indexOf("<" + topLevelElementName),
                                              convertedHtmlInstance.indexOf("</" + topLevelElementName + ">"));
                                          Pattern pattern = Pattern.compile(
                                              String.format("^<%s[^>]+>(.+)$",
                                                  topLevelElementName),
                                              Pattern.DOTALL);
                                          Matcher matcher = pattern.matcher(convertedHtmlInstance);

                                          if (!matcher.matches()) {
                                            fail(String.format(
                                                "Unable to extract converted content using pattern '%s' from: %s",
                                                pattern.pattern(),
                                                convertedHtmlInstance));
                                          }

                                          // compare the generated XML
                                          String actualXml = matcher.group(1);

                                          // replace q tags
                                          actualXml = QUOTE_TAG_REPLACEMENT_PATTERN.matcher(actualXml)
                                              .replaceAll("\"");

                                          assertEquals(testVector.getXml(), actualXml);

                                          // compare the generated HTML
                                          String actualHtml = content.toHtml();

                                          assertEquals(testVector.getRawHtml(), actualHtml);
                                        }))),
                            DynamicTest.dynamicTest(
                                "Validate XML Test Vector",
                                () -> {
                                  // Document data = testVector.getHtmlDocument();
                                  // String html = data.body().html();
                                  String html = testVector.getHtml();
                                  String xmlTestVector = generateXmlInstance(html);

                                  // System.out.println(xmlTestVector);
                                  StringReader reader = new StringReader(xmlTestVector);

                                  StreamSource source = new StreamSource(reader);
                                  Validator validator = schema.newValidator();
                                  validator.setErrorHandler(new Handler());
                                  try {
                                    validator.validate(source);
                                  } catch (SAXParseException ex) {
                                    fail(String.format("Invalid vector '%s'. %s",
                                        html,
                                        ex.getLocalizedMessage()));
                                  }
                                })));
                  }));
        });
  }

  @JsonIgnoreProperties({ "comment", "start_line", "end_line" })
  @SuppressWarnings("PMD.DataClass")
  private static class Entry {
    @NonNull
    private final String markdown;
    @NonNull
    private final String rawHtml;
    @NonNull
    private final Document html;
    @NonNull
    private final String xml;
    private final int exampleNumber;
    @NonNull
    private final String section;
    private final boolean enabled;

    @JsonCreator
    public Entry(
        @JsonProperty("markdown") @NonNull String markdown,
        @JsonProperty("html") @NonNull String html,
        @JsonProperty("xml") @Nullable String xml,
        @JsonProperty("example") int exampleNumber,
        @JsonProperty("section") @NonNull String section,
        @JsonProperty("enabled") @Nullable Boolean enabled) {
      this.markdown = markdown;
      this.rawHtml = ObjectUtils.notNull(html.trim());
      Document document = Jsoup.parseBodyFragment(html);
      document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
      this.html = document;
      this.xml = ObjectUtils.notNull(
          (xml == null ? html : xml) // allow for XML overrides
              .replace(" />", "/>") // the XML stack produces tighter closing elements
              .replaceAll("(?<!>|\")&gt;", ">") // the XML stack doesn't need these entities
              .stripTrailing()); // this implementation removes trailing whitespace

      this.exampleNumber = exampleNumber;
      this.section = section;
      this.enabled = enabled == null || enabled;
    }

    @NonNull
    public String getMarkdown() {
      return markdown;
    }

    @NonNull
    protected Document getHtmlDocument() {
      return html;
    }

    @NonNull
    public String getRawHtml() {
      return rawHtml;
    }

    @SuppressWarnings("null")
    @NonNull
    public String getHtml() {
      return getHtmlDocument().body().html();
    }

    @NonNull
    public String getXml() {
      return xml;
    }

    public int getExampleNumber() {
      return exampleNumber;
    }

    @NonNull
    public String getSection() {
      return section;
    }

    public boolean isEnabled() {
      return enabled;
    }

  }

  static class Handler
      extends DefaultHandler {

    @Override
    public void error(SAXParseException ex) throws SAXException {
      throw ex;
    }
  }
}
