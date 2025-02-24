/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.datatype.markup.flexmark; // NOPMD AST processor

import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.formatter.MarkdownWriter;
import com.vladsch.flexmark.formatter.NodeFormatter;
import com.vladsch.flexmark.formatter.NodeFormatterContext;
import com.vladsch.flexmark.formatter.NodeFormatterFactory;
import com.vladsch.flexmark.formatter.NodeFormattingHandler;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.html2md.converter.HtmlMarkdownWriter;
import com.vladsch.flexmark.html2md.converter.HtmlNodeConverterContext;
import com.vladsch.flexmark.html2md.converter.HtmlNodeRenderer;
import com.vladsch.flexmark.html2md.converter.HtmlNodeRendererFactory;
import com.vladsch.flexmark.html2md.converter.HtmlNodeRendererHandler;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitorBase;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.CharSubSequence;

import gov.nist.secauto.metaschema.core.datatype.markup.IMarkupString;

import org.jsoup.nodes.Element;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Extension that adds support for insert anchors, used in OSCAL statements, and
 * applicable more generally in other Metaschema-based models.
 */
public class InsertAnchorExtension
    implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension,
    Formatter.FormatterExtension, FlexmarkHtmlConverter.HtmlConverterExtension {
  private static final DataKey<Boolean> ENABLE_INLINE_INSERT_ANCHORS
      = new DataKey<>("ENABLE_INLINE_INSERT_ANCHORS", true);
  private static final DataKey<Boolean> ENABLE_RENDERING = new DataKey<>("ENABLE_RENDERING", true);

  /**
   * Construct a new extension instance.
   *
   * @return the instance
   */
  public static InsertAnchorExtension newInstance() {
    return new InsertAnchorExtension();
  }

  @Override
  public void parserOptions(MutableDataHolder options) {
    // do nothing
  }

  @Override
  public void rendererOptions(MutableDataHolder options) {
    // do nothing
  }

  @Override
  public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
    rendererBuilder.nodeRendererFactory(new InsertAnchorNodeRenderer.Factory());
  }

  @Override
  public void extend(Parser.Builder parserBuilder) {
    if (ENABLE_INLINE_INSERT_ANCHORS.get(parserBuilder)) {
      parserBuilder.customInlineParserExtensionFactory(new InsertAnchorInlineParser.Factory());
    }
  }

  @Override
  public void extend(Formatter.Builder builder) {
    builder.nodeFormatterFactory(new InsertAnchorFormatter.Factory());
  }

  @Override
  public void extend(FlexmarkHtmlConverter.Builder builder) {
    builder.htmlNodeRendererFactory(new InsertAnchorHtmlNodeRenderer.Factory());
  }

  private static class InsertAnchorOptions {
    public final boolean enableInlineInsertAnchors;
    public final boolean enableRendering;

    public InsertAnchorOptions(DataHolder options) {
      enableInlineInsertAnchors = ENABLE_INLINE_INSERT_ANCHORS.get(options);
      enableRendering = ENABLE_RENDERING.get(options);
    }
  }

  private static class InsertAnchorNodeRenderer implements NodeRenderer {
    private final InsertAnchorOptions options;

    public InsertAnchorNodeRenderer(DataHolder options) {
      this.options = new InsertAnchorOptions(options);
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
      return Collections.singleton(new NodeRenderingHandler<>(InsertAnchorNode.class, this::render));
    }

    @SuppressWarnings("unused")
    protected void render(InsertAnchorNode node, NodeRendererContext context, HtmlWriter html) {
      if (options.enableRendering) {
        html.attr("type", node.getType()).attr("id-ref", node.getIdReference()).withAttr().tagVoid("insert");
      }
    }

    public static class Factory implements NodeRendererFactory {

      @Override
      public NodeRenderer apply(DataHolder options) {
        return new InsertAnchorNodeRenderer(options);
      }
    }
  }

  private static class InsertAnchorInlineParser implements InlineParserExtension {
    private static final Pattern PATTERN = Pattern.compile("\\{\\{\\s*insert:\\s*([^\\s]+),\\s*([^\\s]+)\\s*\\}\\}");

    public InsertAnchorInlineParser(@SuppressWarnings("unused") LightInlineParser inlineParser) {
      // do nothing
    }

    @Override
    public void finalizeDocument(InlineParser inlineParser) {
      // do nothing
    }

    @Override
    public void finalizeBlock(InlineParser inlineParser) {
      // do nothing
    }

    @Override
    public boolean parse(LightInlineParser inlineParser) {
      if (inlineParser.peek() == '{') {
        BasedSequence input = inlineParser.getInput();
        Matcher matcher = inlineParser.matcher(PATTERN);
        if (matcher != null) {
          BasedSequence type = input.subSequence(matcher.start(1), matcher.end(1));
          BasedSequence idReference = input.subSequence(matcher.start(2), matcher.end(2));
          assert type != null;
          assert idReference != null;
          inlineParser.appendNode(new InsertAnchorNode(type, idReference));
          return true; // NOPMD - readability
        }
      }
      return false;
    }

    public static class Factory implements InlineParserExtensionFactory {
      @Override
      public Set<Class<?>> getAfterDependents() {
        return Collections.emptySet();
      }

      @Override
      public CharSequence getCharacters() {
        return "{";
      }

      @Override
      public Set<Class<?>> getBeforeDependents() {
        return Collections.emptySet();
      }

      @Override
      public InlineParserExtension apply(LightInlineParser lightInlineParser) {
        return new InsertAnchorInlineParser(lightInlineParser);
      }

      @Override
      public boolean affectsGlobalScope() {
        return false;
      }
    }
  }

  private static class InsertAnchorFormatter implements NodeFormatter {
    private final InsertAnchorOptions options;

    public InsertAnchorFormatter(DataHolder options) {
      this.options = new InsertAnchorOptions(options);
    }

    @Override
    public Set<NodeFormattingHandler<?>> getNodeFormattingHandlers() {
      return options.enableInlineInsertAnchors
          ? Collections.singleton(new NodeFormattingHandler<>(InsertAnchorNode.class, this::render))
          : Collections.emptySet();
    }

    @SuppressWarnings("unused")
    protected void render(InsertAnchorNode node, NodeFormatterContext context, MarkdownWriter markdown) {
      if (options.enableRendering) {
        markdown.append("{{ insert: ");
        markdown.append(node.getType());
        markdown.append(", ");
        markdown.append(node.getIdReference());
        markdown.append(" }}");
      }
    }

    @Override
    public Set<Class<?>> getNodeClasses() {
      return Collections.singleton(InsertAnchorNode.class);
    }

    public static class Factory implements NodeFormatterFactory {

      @Override
      public NodeFormatter create(DataHolder options) {
        return new InsertAnchorFormatter(options);
      }

    }
  }

  private static class InsertAnchorHtmlNodeRenderer implements HtmlNodeRenderer {
    private final InsertAnchorOptions options;

    public InsertAnchorHtmlNodeRenderer(DataHolder options) {
      this.options = new InsertAnchorOptions(options);
    }

    @Override
    public Set<HtmlNodeRendererHandler<?>> getHtmlNodeRendererHandlers() {
      return options.enableInlineInsertAnchors
          ? Collections.singleton(new HtmlNodeRendererHandler<>("insert", Element.class, this::processInsert))
          : Collections.emptySet();
    }

    private void processInsert( // NOPMD used as lambda
        Element node,
        @SuppressWarnings("unused") HtmlNodeConverterContext context,
        HtmlMarkdownWriter out) {

      String type = node.attr("type");
      String idRef = node.attr("id-ref");

      out.append("{{ insert: ");
      out.append(type);
      out.append(", ");
      out.append(idRef);
      out.append(" }}");
    }

    public static class Factory implements HtmlNodeRendererFactory {

      @Override
      public HtmlNodeRenderer apply(DataHolder options) {
        return new InsertAnchorHtmlNodeRenderer(options);
      }
    }
  }

  /**
   * The flexmark node for an insert anchor.
   */
  public static class InsertAnchorNode
      extends Node {

    @NonNull
    private final BasedSequence type;
    @NonNull
    private BasedSequence idReference;

    /**
     * Construct a new Metaschema insert node.
     *
     * @param type
     *          the type of insertion
     * @param idReference
     *          the identifier of the given type to use to determine what to insert
     */
    @SuppressWarnings("null")
    public InsertAnchorNode(@NonNull String type, @NonNull String idReference) {
      this(CharSubSequence.of(type), CharSubSequence.of(idReference));
    }

    /**
     * Construct a new Metaschema insert node.
     *
     * @param type
     *          the type of insertion
     * @param idReference
     *          the identifier of the given type to use to determine what to insert
     */
    protected InsertAnchorNode(@NonNull BasedSequence type, @NonNull BasedSequence idReference) {
      this.type = type;
      this.idReference = idReference;
    }

    /**
     * Get the type of insertion.
     *
     * @return the type of insertion
     */
    @NonNull
    public BasedSequence getType() {
      return type;
    }

    /**
     * Get the identifier of the given type to use to determine what to insert.
     *
     * @return the identifier
     */
    @NonNull
    public BasedSequence getIdReference() {
      return idReference;
    }

    /**
     * Set the identifier of the given type to use to determine what to insert.
     *
     * @param idReference
     *          the identifier
     */
    public void setIdReference(@NonNull BasedSequence idReference) {
      this.idReference = idReference;
    }

    @Override
    @NonNull
    public BasedSequence[] getSegments() {
      return new BasedSequence[] { getType(), getIdReference() };
    }

    @Override
    public void getAstExtra(StringBuilder out) {
      segmentSpanChars(out, getType(), "type");
      segmentSpanChars(out, getIdReference(), "id-ref");
    }
  }

  /**
   * Used to collect insert nodes.
   */
  public static class InsertVisitor
      extends NodeVisitorBase {
    @NonNull
    private final List<InsertAnchorNode> inserts = new LinkedList<>();
    @NonNull
    private final Predicate<InsertAnchorNode> filter;

    /**
     * Construct a new visitor that will use the provided filter to visit matching
     * insert nodes.
     *
     * @param filter
     *          the match criteria to use to identify matching insert nodes
     */
    public InsertVisitor(@NonNull Predicate<InsertAnchorNode> filter) {
      this.filter = filter;
    }

    /**
     * Process markup to identify insert nodes.
     *
     * @param markup
     *          the markup to process
     * @return this visitor
     */
    public InsertVisitor processNode(@NonNull IMarkupString<?> markup) {
      visit(markup.getDocument());
      return this;
    }

    @Override
    protected void visit(Node node) {
      if (node instanceof InsertAnchorNode) {
        InsertAnchorNode insert = (InsertAnchorNode) node;
        if (filter.test(insert)) {
          inserts.add((InsertAnchorNode) node);
        }
      } else {
        visitChildren(node);
      }
    }

    /**
     * Get the collected insert nodes.
     *
     * @return the nodes
     */
    @NonNull
    public List<InsertAnchorNode> getInserts() {
      return inserts;
    }
  }
}
