/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.datatype.markup.flexmark.impl; // NOPMD AST processor has many members

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.CodeBlock;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ast.HtmlEntity;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.MailLink;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.ParagraphItemContainer;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.TextBase;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ext.escaped.character.EscapedCharacter;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.typographic.TypographicQuotes;
import com.vladsch.flexmark.ext.typographic.TypographicSmarts;
import com.vladsch.flexmark.parser.ListOptions;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Escaping;

import gov.nist.secauto.metaschema.core.datatype.markup.flexmark.HtmlQuoteTagExtension.DoubleQuoteNode;
import gov.nist.secauto.metaschema.core.datatype.markup.flexmark.InsertAnchorExtension.InsertAnchorNode;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.select.NodeVisitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Used to write HTML-based Markup to various types of streams.
 *
 * @param <T>
 *          the type of stream to write to
 * @param <E>
 *          the type of exception that can be thrown when a writing error occurs
 */
@SuppressFBWarnings(
    value = "THROWS_METHOD_THROWS_CLAUSE_THROWABLE",
    justification = "Class supports writers that use both Exception and RuntimeException.")
public abstract class AbstractMarkupWriter<T, E extends Throwable> // NOPMD not god class
    implements IMarkupWriter<T, E> {
  private static final Pattern ENTITY_PATTERN = Pattern.compile("^&([^;]+);$");
  private static final Map<String, String> ENTITY_MAP;

  static {
    ENTITY_MAP = new HashMap<>();
    // force writing of non-breaking spaces
    ENTITY_MAP.put("&npsb;", "&npsb;");
  }

  @NonNull
  private final String namespace;

  @NonNull
  private final T stream;

  @NonNull
  private final ListOptions options;

  /**
   * Construct a new markup writer.
   *
   * @param namespace
   *          the XHTML namespace to use for elements
   * @param options
   *          list production options
   * @param stream
   *          the stream to write to
   */
  protected AbstractMarkupWriter(@NonNull String namespace, @NonNull ListOptions options, @NonNull T stream) {
    this.namespace = namespace;
    this.options = options;
    this.stream = stream;
  }

  @NonNull
  private String getNamespace() {
    return namespace;
  }

  private ListOptions getOptions() {
    return options;
  }

  /**
   * Get the underlying stream to write to.
   *
   * @return the stream
   */
  @NonNull
  protected T getStream() {
    return stream;
  }

  @NonNull
  private QName asQName(@NonNull String localName) {
    return new QName(getNamespace(), localName);
  }

  private void visitChildren(
      @NonNull Node parentNode,
      @NonNull ChildHandler<T, E> childHandler) throws E {
    for (Node node : parentNode.getChildren()) {
      assert node != null;
      childHandler.accept(node, this);
    }
  }

  private void writePrecedingNewline(@NonNull Block node) throws E {
    Node prev = node.getPrevious();
    if (prev != null
        || !(node.getParent() instanceof com.vladsch.flexmark.util.ast.Document)) {
      writeText("\n");
    }
  }

  private void writeTrailingNewline(@NonNull Block node) throws E {
    Node next = node.getNext();
    if (next != null && !next.isOrDescendantOfType(Block.class) // handled by preceding block
        || next == null && !(node.getParent() instanceof com.vladsch.flexmark.util.ast.Document)) {
      writeText("\n");
    }
  }

  /**
   * Write an HTML element with the provided local name.
   *
   * @param localName
   *          the element name
   * @param node
   *          the markup node related to the element
   * @param attributes
   *          attributes associated with the element to also write
   * @param childHandler
   *          a handler used to process child node content or {@code null}
   * @throws E
   *           if an error occurred while writing the markup
   */
  @Override
  public void writeElement(
      @NonNull String localName,
      @NonNull Node node,
      @NonNull Map<String, String> attributes,
      @Nullable ChildHandler<T, E> childHandler) throws E {
    QName qname = asQName(localName);
    writeElement(qname, node, attributes, childHandler);
  }

  private void writeElement(
      @NonNull QName qname,
      @NonNull Node node,
      @NonNull Map<String, String> attributes,
      @Nullable ChildHandler<T, E> childHandler) throws E {
    if (node.hasChildren()) {
      writeElementStart(qname, attributes);
      if (childHandler != null) {
        visitChildren(node, childHandler);
      }
      writeElementEnd(qname);
    } else {
      writeEmptyElement(qname, attributes);
    }
  }

  @Override
  public void writeEmptyElement(
      @NonNull String localName,
      @NonNull Map<String, String> attributes) throws E {
    QName qname = asQName(localName);
    writeEmptyElement(qname, attributes);
  }

  /**
   * Write an empty element with the provided qualified name and attributes.
   *
   * @param qname
   *          the qualified name to use for the element name
   * @param attributes
   *          the attributes
   * @throws E
   *           if an error occurred while writing
   */
  protected abstract void writeEmptyElement(
      @NonNull QName qname,
      @NonNull Map<String, String> attributes) throws E;

  /**
   * Write a start element with the provided qualified name and no attributes.
   *
   * @param qname
   *          the qualified name to use for the element name
   * @throws E
   *           if an error occurred while writing
   */
  private void writeElementStart(
      @NonNull QName qname) throws E {
    writeElementStart(qname, CollectionUtil.emptyMap());
  }

  /**
   * Write a start element with the provided qualified name and attributes.
   *
   * @param qname
   *          the qualified name to use for the element name
   * @param attributes
   *          the attributes
   * @throws E
   *           if an error occurred while writing
   */
  protected abstract void writeElementStart(
      @NonNull QName qname,
      @NonNull Map<String, String> attributes) throws E;

  /**
   * Write an end element with the provided qualified name.
   *
   * @param qname
   *          the qualified name to use for the element name
   * @throws E
   *           if an error occurred while writing
   */
  protected abstract void writeElementEnd(@NonNull QName qname) throws E;

  @SuppressWarnings({
      "unchecked",
      "PMD.UnusedPrivateMethod"
  }) // while unused, keeping code for when inline HTML is supported
  private void writeHtml(Node node) throws E {
    Document doc = Jsoup.parse(node.getChars().toString());
    try {
      doc.body().traverse(new MarkupNodeVisitor());
    } catch (NodeVisitorException ex) {
      throw (E) ex.getCause(); // NOPMD exception is wrapper
    }
  }

  @Override
  public final void writeText(Text node) throws E {
    BasedSequence text = node.getChars();
    Node prev = node.getPrevious();
    if (prev instanceof HardLineBreak) {
      // strip leading after hard line break
      assert text != null;
      text = text.trimStart();
    }
    assert text != null;
    writeText(text);
  }

  @Override
  public void writeText(@NonNull TextBase node) throws E {
    StringBuilder buf = new StringBuilder(node.getChars().length());
    for (Node child : node.getChildren()) {
      CharSequence chars;
      if (child instanceof Text) {
        Text text = (Text) child;
        chars = text.getChars();
      } else if (child instanceof EscapedCharacter) {
        EscapedCharacter ec = (EscapedCharacter) child;
        chars = ec.getChars().unescape();
      } else {
        throw new UnsupportedOperationException("Node type: " + child.getNodeName());
      }
      buf.append(chars);
    }
    writeText(buf);
  }

  @Override
  public void writeHtmlEntity(@NonNull HtmlEntity node) throws E {
    String text = node.getChars().unescape();
    assert text != null;
    writeHtmlEntity(text);
  }

  @Override
  public void writeHtmlEntity(@NonNull TypographicSmarts node) throws E {
    String text = ObjectUtils.requireNonNull(node.getTypographicText());
    assert text != null;
    writeHtmlEntity(text);
  }

  private void writeHtmlEntity(String entityText) throws E {
    String replacement = ENTITY_MAP.get(entityText);
    if (replacement != null) {
      Matcher matcher = ENTITY_PATTERN.matcher(replacement);
      if (matcher.matches()) {
        writeHtmlEntityInternal(ObjectUtils.notNull(matcher.group(1)));
      } else {
        writeText(replacement);
      }
    } else {
      String value = StringEscapeUtils.unescapeHtml4(entityText);
      assert value != null;
      writeText(value);
    }
  }

  /**
   * Write an HTML entity.
   *
   * @param text
   *          the entity text
   * @throws E
   *           if an error occurred while writing
   */
  protected void writeHtmlEntityInternal(@NonNull String text) throws E {
    writeText(text);
  }

  @Override
  public void writeParagraph(
      @NonNull Paragraph node,
      @NonNull ChildHandler<T, E> childHandler) throws E {
    if (node.getParent() instanceof ParagraphItemContainer && getOptions().isInTightListItem(node)) {
      if (node.getPrevious() != null) {
        writeText("\n");
      }
      visitChildren(node, childHandler);
    } else {
      writePrecedingNewline(node);
      writeElement("p", node, childHandler);
      writeTrailingNewline(node);
    }
  }

  @Override
  public void writeLink(
      @NonNull Link node,
      @NonNull ChildHandler<T, E> childHandler) throws E {
    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe
    String href = Escaping.percentEncodeUrl(node.getUrl().unescape());
    try {
      // ensure URI is valid
      attributes.put("href", new URI(href).toASCIIString());
    } catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }

    if (!node.getTitle().isBlank()) {
      String title = ObjectUtils.notNull(node.getTitle().unescape());
      attributes.put("title", title);
    }

    // writeElement("a", node, attributes, childHandler);
    QName qname = asQName("a");
    writeElementStart(qname, attributes);
    if (node.hasChildren()) {
      visitChildren(node, childHandler);
    } else {
      writeText("");
    }
    writeElementEnd(qname);
  }

  @Override
  public void writeLink(@NonNull MailLink node) throws E {
    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe

    String href = Escaping.percentEncodeUrl(node.getText().unescape());
    try {
      // ensure URI is valid
      attributes.put("href", new URI("mailto:" + href).toASCIIString());
    } catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }

    QName qname = asQName("a");
    writeElementStart(qname, attributes);

    BasedSequence text = node.getText();
    writeText(text == null ? "\n" : ObjectUtils.notNull(text.unescape()));
    writeElementEnd(qname);
  }

  @Override
  public void writeLink(@NonNull AutoLink node) throws E {
    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe

    String href = Escaping.percentEncodeUrl(node.getUrl().unescape());
    try {
      // ensure URI is valid
      attributes.put("href", new URI(href).toASCIIString());
    } catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }

    QName qname = asQName("a");
    writeElementStart(qname, attributes);
    writeText(ObjectUtils.notNull(node.getText().unescape()));
    writeElementEnd(qname);
  }

  @Override
  public final void writeTypographicQuotes(
      TypographicQuotes node,
      ChildHandler<T, E> childHandler) throws E {
    if (node instanceof DoubleQuoteNode) {
      writeElement("q", node, childHandler);
    } else {
      String opening = node.getTypographicOpening();
      if (opening != null && !opening.isEmpty()) {
        writeHtmlEntity(opening);
      }

      visitChildren(node, childHandler);

      String closing = node.getTypographicClosing();
      if (closing != null && !closing.isEmpty()) {
        writeHtmlEntity(closing);
      }
    }
  }

  @Override
  public final void writeInlineHtml(HtmlInline node) throws E {
    // throw new UnsupportedOperationException(
    // String.format("Inline HTML is not supported. Found: %s", node.getChars()));
    writeHtml(node);
  }

  @Override
  public final void writeBlockHtml(HtmlBlock node) throws E {
    // throw new UnsupportedOperationException(
    // String.format("Inline HTML is not supported. Found: %s", node.getChars()));

    writePrecedingNewline(node);
    writeHtml(node);
    writeTrailingNewline(node);
  }

  @Override
  public final void writeTable(
      TableBlock node,
      ChildHandler<T, E> cellChildHandler) throws E {
    writePrecedingNewline(node);
    QName qname = asQName("table");
    writeElementStart(qname);

    TableHead head = (TableHead) node.getChildOfType(TableHead.class);

    QName theadQName = asQName("thead");
    if (head != null) {
      writeText("\n");
      writeElementStart(theadQName);
      for (Node childNode : head.getChildren()) {
        if (childNode instanceof TableRow) {
          writeTableRow((TableRow) childNode, cellChildHandler);
        }
      }
      writeElementEnd(theadQName);
    }

    TableBody body = (TableBody) node.getChildOfType(TableBody.class);

    if (body != null) {
      QName tbodyQName = asQName("tbody");
      writeText("\n");
      writeElementStart(tbodyQName);
      for (Node childNode : body.getChildren()) {
        if (childNode instanceof TableRow) {
          writeTableRow((TableRow) childNode, cellChildHandler);
        }
      }
      writeElementEnd(tbodyQName);
    }

    writeText("\n");
    writeElementEnd(qname);
    writeTrailingNewline(node);
  }

  private void writeTableRow(
      @NonNull TableRow node,
      @NonNull ChildHandler<T, E> cellChildHandler) throws E {
    writeText("\n");
    QName qname = asQName("tr");
    writeElementStart(qname);

    for (Node childNode : node.getChildren()) {
      if (childNode instanceof TableCell) {
        writeTableCell((TableCell) childNode, cellChildHandler);
      }
    }

    writeElementEnd(qname);
    if (node.getNext() == null) {
      writeText("\n");
    }
  }

  private void writeTableCell(
      @NonNull TableCell node,
      @NonNull ChildHandler<T, E> cellChildHandler) throws E {
    QName qname = node.isHeader() ? asQName("th") : asQName("td");

    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe
    if (node.getAlignment() != null) {
      attributes.put("align", ObjectUtils.requireNonNull(node.getAlignment().toString()));
    }

    writeElementStart(qname, attributes);
    visitChildren(node, cellChildHandler);
    writeElementEnd(qname);
  }

  @Override
  public void writeImage(
      @NonNull Image node) throws E {
    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe
    String href = ObjectUtils.requireNonNull(Escaping.percentEncodeUrl(node.getUrl().unescape()));
    try {
      // ensure URI is valid
      attributes.put("src", new URI(href).toASCIIString());
    } catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }

    attributes.put("alt", ObjectUtils.requireNonNull(node.getText().toString()));

    if (!node.getTitle().isBlank()) {
      attributes.put("title", ObjectUtils.requireNonNull(node.getTitle().toString()));
    }

    writeEmptyElement("img", attributes);
  }

  @Override
  public void writeInsertAnchor(@NonNull InsertAnchorNode node) throws E {
    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe
    attributes.put("type", ObjectUtils.requireNonNull(node.getType().toString()));
    attributes.put("id-ref", ObjectUtils.requireNonNull(node.getIdReference().toString()));

    writeElement("insert", node, attributes, null);
  }

  @Override
  public void writeHeading(
      @NonNull Heading node,
      @NonNull ChildHandler<T, E> childHandler) throws E {
    writePrecedingNewline(node);
    int level = node.getLevel();

    QName qname = asQName(ObjectUtils.notNull(String.format("h%d", level)));

    writeElementStart(qname);
    if (node.hasChildren()) {
      visitChildren(node, childHandler);
    } else {
      // ensure empty tags are created
      writeText("");
    }
    writeElementEnd(qname);
    writeTrailingNewline(node);
  }

  /**
   * Normalize whitespace according to
   * https://spec.commonmark.org/0.30/#code-spans. Based on code from Flexmark.
   *
   * @param text
   *          text to process
   * @return the normalized text
   */
  @NonNull
  protected static String collapseWhitespace(@NonNull CharSequence text) {
    StringBuilder sb = new StringBuilder(text.length());
    int length = text.length();
    boolean needsSpace = false;
    for (int i = 0; i < length; i++) {
      char ch = text.charAt(i);
      // convert line endings to spaces
      if (ch == '\n' || ch == '\r') {
        if (sb.length() > 0) {
          // ignore leading
          needsSpace = true;
        }
      } else {
        if (needsSpace) {
          sb.append(' ');
          needsSpace = false;
        }
        sb.append(ch);
      }
    }

    String result = sb.toString();
    if (result.matches("^[ ]{1,}[^ ].* $")) {
      // if there is a space at the start and end, remove them
      result = result.substring(1, result.length() - 1);
    }
    return ObjectUtils.notNull(result);
  }

  @Override
  public void writeCode(Code node, ChildHandler<T, E> childHandler) throws E {
    QName qname = asQName("code");
    writeElementStart(qname);
    visitChildren(node, (child, writer) -> {
      if (child instanceof Text || child instanceof TextBase) {
        String text = collapseWhitespace(ObjectUtils.notNull(child.getChars()));
        writeText(text);
      } else {
        childHandler.accept(child, writer);
      }
    });
    writeElementEnd(qname);
  }

  @Override
  public final void writeCodeBlock(
      IndentedCodeBlock node,
      ChildHandler<T, E> childHandler) throws E {
    writePrecedingNewline(node);
    QName preQName = asQName("pre");

    writeElementStart(preQName);

    QName codeQName = asQName("code");

    writeElementStart(codeQName);

    if (node.hasChildren()) {
      visitChildren(node, childHandler);
    } else {
      // ensure empty tags are created
      writeText("");
    }

    writeElementEnd(codeQName);

    writeElementEnd(preQName);
    writeTrailingNewline(node);
  }

  @Override
  public final void writeCodeBlock(
      FencedCodeBlock node,
      ChildHandler<T, E> childHandler) throws E {
    writePrecedingNewline(node);
    QName preQName = asQName("pre");

    writeElementStart(preQName);

    QName codeQName = asQName("code");
    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe
    if (node.getInfo().isNotNull()) {
      attributes.put("class", "language-" + node.getInfo().unescape());
    }

    writeElementStart(codeQName, attributes);

    if (node.hasChildren()) {
      visitChildren(node, childHandler);
    } else {
      // ensure empty tags are created
      writeText("");
    }

    writeElementEnd(codeQName);

    writeElementEnd(preQName);
    writeTrailingNewline(node);
  }

  @Override
  public void writeCodeBlock(CodeBlock node, ChildHandler<T, E> childHandler) throws E {
    String text;
    if (node.getParent() instanceof IndentedCodeBlock) {
      text = node.getContentChars().trimTailBlankLines().toString();
    } else {
      text = node.getContentChars().toString();
    }
    writeText(ObjectUtils.notNull(text));
  }

  @Override
  public void writeBlockQuote(BlockQuote node, ChildHandler<T, E> childHandler) throws E {
    writePrecedingNewline(node);
    QName qname = asQName("blockquote");
    writeElementStart(qname);
    // writeText("\n");

    if (node.hasChildren()) {
      visitChildren(node, childHandler);
    } else {
      // ensure empty tags are created
      writeText("\n");
    }

    // writeText("\n");
    writeElementEnd(qname);
    writeTrailingNewline(node);
  }

  @Override
  public void writeList(
      @NonNull String localName,
      @NonNull ListBlock node,
      @NonNull ChildHandler<T, E> listItemHandler) throws E {
    QName qname = asQName(localName);
    writeList(qname, node, listItemHandler);
  }

  private void writeList(
      @NonNull QName qname,
      @NonNull ListBlock node,
      @NonNull ChildHandler<T, E> listItemHandler) throws E {
    Map<String, String> attributes = new LinkedHashMap<>(); // NOPMD local use; thread-safe
    if (node instanceof OrderedList) {
      OrderedList ol = (OrderedList) node;
      int start = ol.getStartNumber();
      if (start != 1) {
        attributes.put("start", String.valueOf(start));
      }
    }

    writePrecedingNewline(node);
    writeElementStart(qname, attributes);

    visitChildren(node, (child, writer) -> {
      ListItem item = (ListItem) child;
      writeListItem(item, listItemHandler);
    });

    writeElementEnd(qname);
    writeTrailingNewline(node);
  }

  private void writeListItem(
      @NonNull ListItem node,
      @NonNull ChildHandler<T, E> listItemHandler) throws E {
    QName qname = asQName("li");
    writePrecedingNewline(node);
    writeElementStart(qname);

    if (node.hasChildren()) {
      visitChildren(node, listItemHandler);
    } else {
      // ensure empty tags are created
      writeText("");
    }
    writeElementEnd(qname);
    writeTrailingNewline(node);
  }

  @Override
  public void writeBreak(HardLineBreak node) throws E {
    writeElement("br", node, null);
    writeText("\n");
  }

  @Override
  public void writeBreak(ThematicBreak node) throws E {
    writePrecedingNewline(node);
    writeElement("hr", node, null);
    writeTrailingNewline(node);
  }

  @Override
  public void writeComment(HtmlCommentBlock node) throws E {
    writePrecedingNewline(node);

    BasedSequence text = node.getChars();
    text = text.subSequence(4, text.length() - 4);
    writeComment(ObjectUtils.notNull(text.unescape()));
    writeTrailingNewline(node);

  }

  /**
   * Write a comment.
   *
   * @param text
   *          the comment text
   * @throws E
   *           if an error occurred while writing
   */
  protected abstract void writeComment(@NonNull CharSequence text) throws E;

  private static class NodeVisitorException
      extends IllegalStateException {
    /**
     * the serial version uid.
     */
    private static final long serialVersionUID = 1L;

    public NodeVisitorException(Throwable cause) {
      super(cause);
    }
  }

  private final class MarkupNodeVisitor implements NodeVisitor {
    @Override
    public void head(org.jsoup.nodes.Node node, int depth) { // NOPMD acceptable
      if (depth > 0) {
        try {
          if (node instanceof org.jsoup.nodes.Element) {
            org.jsoup.nodes.Element element = (org.jsoup.nodes.Element) node;

            Attributes attributes = element.attributes();

            Map<String, String> attrMap;
            if (attributes.isEmpty()) {
              attrMap = CollectionUtil.emptyMap();
            } else {
              attrMap = new LinkedHashMap<>();
              for (org.jsoup.nodes.Attribute attr : attributes) {
                attrMap.put(attr.getKey(), attr.getValue());
              }
            }

            QName qname = asQName(ObjectUtils.notNull(element.tagName()));
            if (element.childNodes().isEmpty()) {
              writeEmptyElement(qname, attrMap);
            } else {
              writeElementStart(qname, attrMap);
            }
          } else if (node instanceof org.jsoup.nodes.TextNode) {
            org.jsoup.nodes.TextNode text = (org.jsoup.nodes.TextNode) node;
            writeText(ObjectUtils.requireNonNull(text.text()));
          }
        } catch (Throwable ex) { // NOPMD need to catch Throwable
          throw new NodeVisitorException(ex);
        }
      }
    }

    @Override
    public void tail(org.jsoup.nodes.Node node, int depth) {
      if (depth > 0 && node instanceof org.jsoup.nodes.Element) {
        org.jsoup.nodes.Element element = (org.jsoup.nodes.Element) node;
        if (!element.childNodes().isEmpty()) {
          QName qname = asQName(ObjectUtils.notNull(element.tagName()));
          try {
            writeElementEnd(qname);
          } catch (Throwable ex) { // NOPMD need to catch Throwable
            throw new NodeVisitorException(ex);
          }
        }
      }
    }
  }
}
