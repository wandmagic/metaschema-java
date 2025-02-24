/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.databind.io.json;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nist.secauto.metaschema.core.model.IBoundObject;
import gov.nist.secauto.metaschema.core.model.IMetaschemaData;
import gov.nist.secauto.metaschema.core.model.util.JsonUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.io.BindingException;
import gov.nist.secauto.metaschema.databind.model.IBoundDefinitionModelAssembly;
import gov.nist.secauto.metaschema.databind.model.IBoundDefinitionModelComplex;
import gov.nist.secauto.metaschema.databind.model.IBoundDefinitionModelFieldComplex;
import gov.nist.secauto.metaschema.databind.model.IBoundFieldValue;
import gov.nist.secauto.metaschema.databind.model.IBoundInstance;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceFlag;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModel;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelAssembly;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelChoiceGroup;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelFieldComplex;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelFieldScalar;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelGroupedAssembly;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelGroupedField;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelGroupedNamed;
import gov.nist.secauto.metaschema.databind.model.IBoundProperty;
import gov.nist.secauto.metaschema.databind.model.info.AbstractModelInstanceReadHandler;
import gov.nist.secauto.metaschema.databind.model.info.IFeatureScalarItemValueHandler;
import gov.nist.secauto.metaschema.databind.model.info.IItemReadHandler;
import gov.nist.secauto.metaschema.databind.model.info.IModelInstanceCollectionInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.annotation.NotOwning;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Supports reading JSON-based Metaschema module instances.
 */
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",
    "PMD.GodClass"
})
public class MetaschemaJsonReader
    implements IJsonParsingContext, IItemReadHandler {
  private static final Logger LOGGER = LogManager.getLogger(MetaschemaJsonReader.class);

  @NonNull
  private final Deque<JsonParser> parserStack = new LinkedList<>();
  // @NonNull
  // private final InstanceReader instanceReader = new InstanceReader();
  @NonNull
  private final URI source;
  @NonNull
  private final IJsonProblemHandler problemHandler;

  /**
   * Construct a new Module-aware JSON parser using the default problem handler.
   *
   * @param parser
   *          the JSON parser to parse with
   * @param source
   *          the resource being parsed
   * @throws IOException
   *           if an error occurred while reading the JSON
   * @see DefaultJsonProblemHandler
   */
  @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Use of final fields")
  public MetaschemaJsonReader(
      @NonNull JsonParser parser,
      @NonNull URI source) throws IOException {
    this(parser, source, new DefaultJsonProblemHandler());
  }

  /**
   * Construct a new Module-aware JSON parser.
   *
   * @param parser
   *          the JSON parser to parse with
   * @param source
   *          the resource being parsed
   * @param problemHandler
   *          the problem handler implementation to use
   * @throws IOException
   *           if an error occurred while reading the JSON
   */
  @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Use of final fields")
  public MetaschemaJsonReader(
      @NonNull JsonParser parser,
      @NonNull URI source,
      @NonNull IJsonProblemHandler problemHandler) throws IOException {
    this.source = source;
    this.problemHandler = problemHandler;
    push(parser);
  }

  @SuppressWarnings("resource")
  @NotOwning
  @Override
  public JsonParser getReader() {
    return ObjectUtils.notNull(parserStack.peek());
  }

  @Override
  public URI getSource() {
    return source;
  }
  // protected void analyzeParserStack(@NonNull String action) throws IOException
  // {
  // StringBuilder builder = new StringBuilder()
  // .append("------\n");
  //
  // for (JsonParser parser : parserStack) {
  // JsonToken token = parser.getCurrentToken();
  // if (token == null) {
  // LOGGER.info(String.format("Advancing parser: %s", parser.hashCode()));
  // token = parser.nextToken();
  // }
  //
  // String name = parser.currentName();
  // builder.append(String.format("%s: %d: %s(%s)%s\n",
  // action,
  // parser.hashCode(),
  // token.name(),
  // name == null ? "" : name,
  // JsonUtil.generateLocationMessage(parser)));
  // }
  // LOGGER.info(builder.toString());
  // }

  @SuppressWarnings({ "resource", "PMD.CloseResource" })
  private void push(@NonNull JsonParser parser) throws IOException {
    assert !parser.equals(parserStack.peek());
    if (parser.getCurrentToken() == null) {
      parser.nextToken();
    }
    parserStack.push(parser);
  }

  @SuppressWarnings({ "resource", "PMD.CloseResource" })
  @NonNull
  private JsonParser pop(@NonNull JsonParser parser) {
    JsonParser old = parserStack.pop();
    assert parser.equals(old);
    return ObjectUtils.notNull(parserStack.peek());
  }

  @Override
  public IJsonProblemHandler getProblemHandler() {
    return problemHandler;
  }

  /**
   * Read a JSON object value based on the provided definition.
   *
   * @param <T>
   *          the Java type of the bound object produced by this parser
   * @param definition
   *          the Metaschema module definition that describes the node to parse
   * @return the resulting parsed bound object
   * @throws IOException
   *           if an error occurred while parsing the content
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public <T> T readObject(@NonNull IBoundDefinitionModelComplex definition) throws IOException {
    T value = (T) definition.readItem(null, this);
    if (value == null) {
      throw new IOException(String.format("Failed to read object '%s'%s.",
          definition.getDefinitionQName(),
          JsonUtil.generateLocationMessage(getReader(), getSource())));
    }
    return value;
  }

  /**
   * Read a JSON property based on the provided definition.
   *
   * @param <T>
   *          the Java type of the bound object produced by this parser
   * @param definition
   *          the Metaschema module definition that describes the node to parse
   * @param expectedFieldName
   *          the name of the JSON field to parse
   * @return the resulting parsed bound object
   * @throws IOException
   *           if an error occurred while parsing the content
   */
  @SuppressWarnings({
      "unchecked",
      "PMD.CyclomaticComplexity"
  })
  @NonNull
  public <T> T readObjectRoot(
      @NonNull IBoundDefinitionModelComplex definition,
      @NonNull String expectedFieldName) throws IOException {
    @SuppressWarnings("PMD.CloseResource")
    JsonParser parser = getReader();
    URI resource = getSource();

    boolean hasStartObject = JsonToken.START_OBJECT.equals(parser.currentToken());
    if (hasStartObject) {
      // advance past the start object
      JsonUtil.assertAndAdvance(parser, resource, JsonToken.START_OBJECT);
    }

    T retval = null;
    JsonToken token;
    while (!JsonToken.END_OBJECT.equals(token = parser.currentToken()) && token != null) {
      if (!JsonToken.FIELD_NAME.equals(token)) {
        throw new IOException(String.format("Expected FIELD_NAME token, found '%s'", token.toString()));
      }

      String propertyName = ObjectUtils.notNull(parser.currentName());
      if (expectedFieldName.equals(propertyName)) {
        // process the object value, bound to the requested class
        JsonUtil.assertAndAdvance(parser, resource, JsonToken.FIELD_NAME);

        // stop now, since we found the field
        retval = (T) definition.readItem(null, this);
        break;
      }

      if (!getProblemHandler().handleUnknownProperty(
          definition,
          null,
          propertyName,
          this)) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("Skipping unhandled JSON field '{}'{}.", propertyName, JsonUtil.toString(parser, resource));
        }
        JsonUtil.skipNextValue(parser, resource);
      }
    }

    if (hasStartObject) {
      // advance past the end object
      JsonUtil.assertAndAdvance(parser, resource, JsonToken.END_OBJECT);
    }

    if (retval == null) {
      throw new IOException(String.format("Failed to find property with name '%s'%s.",
          expectedFieldName,
          JsonUtil.generateLocationMessage(parser, resource)));
    }
    return retval;
  }

  // ================
  // Instance readers
  // ================

  @Nullable
  private Object readInstance(
      @NonNull IBoundProperty<?> instance,
      @NonNull IBoundObject parent) throws IOException {
    return instance.readItem(parent, this);
  }

  @Nullable
  private <T> Object readModelInstance(
      @NonNull IBoundInstanceModel<T> instance,
      @NonNull IBoundObject parent) throws IOException {
    IModelInstanceCollectionInfo<T> collectionInfo = instance.getCollectionInfo();
    return collectionInfo.readItems(new ModelInstanceReadHandler<>(instance, parent));
  }

  private Object readFieldValue(
      @NonNull IBoundFieldValue instance,
      @NonNull IBoundObject parent) throws IOException {
    // handle the value key name case
    return instance.readItem(parent, this);
  }

  @Nullable
  private Object readObjectProperty(
      @NonNull IBoundObject parent,
      @NonNull IBoundProperty<?> property) throws IOException {
    Object retval;
    if (property instanceof IBoundInstanceModel) {
      retval = readModelInstance((IBoundInstanceModel<?>) property, parent);
    } else if (property instanceof IBoundInstance) {
      retval = readInstance(property, parent);
    } else { // IBoundFieldValue
      retval = readFieldValue((IBoundFieldValue) property, parent);
    }
    return retval;
  }

  @Override
  public Object readItemFlag(IBoundObject parentItem, IBoundInstanceFlag instance) throws IOException {
    return readScalarItem(instance);
  }

  @Override
  public Object readItemField(IBoundObject parentItem, IBoundInstanceModelFieldScalar instance) throws IOException {
    return readScalarItem(instance);
  }

  @Override
  public IBoundObject readItemField(IBoundObject parentItem, IBoundInstanceModelFieldComplex instance)
      throws IOException {
    return readFieldObject(
        parentItem,
        instance.getDefinition(),
        instance.getJsonProperties(),
        instance.getEffectiveJsonKey(),
        getProblemHandler());
  }

  @Override
  public IBoundObject readItemField(IBoundObject parentItem, IBoundInstanceModelGroupedField instance)
      throws IOException {
    IJsonProblemHandler problemHandler = new GroupedInstanceProblemHandler(instance, getProblemHandler());
    IBoundDefinitionModelFieldComplex definition = instance.getDefinition();
    IBoundInstanceFlag jsonValueKeyFlag = definition.getJsonValueKeyFlagInstance();

    IJsonProblemHandler actualProblemHandler = jsonValueKeyFlag == null
        ? problemHandler
        : new JsomValueKeyProblemHandler(problemHandler, jsonValueKeyFlag);

    return readComplexDefinitionObject(
        parentItem,
        definition,
        instance.getEffectiveJsonKey(),
        new PropertyBodyHandler(instance.getJsonProperties()),
        actualProblemHandler);
  }

  @Override
  public IBoundObject readItemField(IBoundObject parentItem, IBoundDefinitionModelFieldComplex definition)
      throws IOException {
    return readFieldObject(
        parentItem,
        definition,
        definition.getJsonProperties(),
        null,
        getProblemHandler());
  }

  @Override
  public Object readItemFieldValue(IBoundObject parentItem, IBoundFieldValue fieldValue) throws IOException {
    // read the field value's value
    return checkMissingFieldValue(readScalarItem(fieldValue));
  }

  @Nullable
  private Object checkMissingFieldValue(Object value) {
    if (value == null && LOGGER.isWarnEnabled()) {
      LOGGER.atWarn().log("Missing property value{}",
          JsonUtil.generateLocationMessage(getReader(), getSource()));
    }
    return value;
  }

  @Override
  public IBoundObject readItemAssembly(IBoundObject parentItem, IBoundInstanceModelAssembly instance)
      throws IOException {
    IBoundInstanceFlag jsonKey = instance.getJsonKey();
    IBoundDefinitionModelComplex definition = instance.getDefinition();
    return readComplexDefinitionObject(
        parentItem,
        definition,
        jsonKey,
        new PropertyBodyHandler(instance.getJsonProperties()),
        getProblemHandler());
  }

  @Override
  public IBoundObject readItemAssembly(IBoundObject parentItem, IBoundInstanceModelGroupedAssembly instance)
      throws IOException {
    return readComplexDefinitionObject(
        parentItem,
        instance.getDefinition(),
        instance.getEffectiveJsonKey(),
        new PropertyBodyHandler(instance.getJsonProperties()),
        new GroupedInstanceProblemHandler(instance, getProblemHandler()));
  }

  @Override
  public IBoundObject readItemAssembly(IBoundObject parentItem, IBoundDefinitionModelAssembly definition)
      throws IOException {
    return readComplexDefinitionObject(
        parentItem,
        definition,
        null,
        new PropertyBodyHandler(definition.getJsonProperties()),
        getProblemHandler());
  }

  @NonNull
  private Object readScalarItem(@NonNull IFeatureScalarItemValueHandler handler)
      throws IOException {
    return handler.getJavaTypeAdapter().parse(getReader(), getSource());
  }

  @NonNull
  private IBoundObject readFieldObject(
      @Nullable IBoundObject parentItem,
      @NonNull IBoundDefinitionModelFieldComplex definition,
      @NonNull Map<String, IBoundProperty<?>> jsonProperties,
      @Nullable IBoundInstanceFlag jsonKey,
      @NonNull IJsonProblemHandler problemHandler) throws IOException {
    IBoundInstanceFlag jsonValueKey = definition.getJsonValueKeyFlagInstance();
    IJsonProblemHandler actualProblemHandler = jsonValueKey == null
        ? problemHandler
        : new JsomValueKeyProblemHandler(problemHandler, jsonValueKey);

    IBoundObject retval;
    if (jsonProperties.isEmpty() && jsonValueKey == null) {
      retval = readComplexDefinitionObject(
          parentItem,
          definition,
          jsonKey,
          (def, parent, problem) -> {
            IBoundFieldValue fieldValue = definition.getFieldValue();
            Object item = readItemFieldValue(parent, fieldValue);
            if (item != null) {
              fieldValue.setValue(parent, item);
            }
          },
          actualProblemHandler);

    } else {
      retval = readComplexDefinitionObject(
          parentItem,
          definition,
          jsonKey,
          new PropertyBodyHandler(jsonProperties),
          actualProblemHandler);
    }
    return retval;
  }

  @NonNull
  private IBoundObject readComplexDefinitionObject(
      @Nullable IBoundObject parentItem,
      @NonNull IBoundDefinitionModelComplex definition,
      @Nullable IBoundInstanceFlag jsonKey,
      @NonNull DefinitionBodyHandler<IBoundDefinitionModelComplex> bodyHandler,
      @NonNull IJsonProblemHandler problemHandler) throws IOException {
    DefinitionBodyHandler<IBoundDefinitionModelComplex> actualBodyHandler = jsonKey == null
        ? bodyHandler
        : new JsonKeyBodyHandler(jsonKey, bodyHandler);

    @SuppressWarnings("PMD.CloseResource")
    JsonLocation location = getReader().currentLocation();

    // construct the item
    IBoundObject item = definition.newInstance(
        JsonLocation.NA.equals(location)
            ? null
            : () -> new MetaschemaData(ObjectUtils.requireNonNull(location)));

    try {
      // call pre-parse initialization hook
      definition.callBeforeDeserialize(item, parentItem);

      // read the property values
      actualBodyHandler.accept(definition, item, problemHandler);

      // call post-parse initialization hook
      definition.callAfterDeserialize(item, parentItem);
    } catch (BindingException ex) {
      throw new IOException(ex);
    }

    return item;
  }

  @SuppressWarnings("resource")
  @Override
  public IBoundObject readChoiceGroupItem(IBoundObject parentItem, IBoundInstanceModelChoiceGroup instance)
      throws IOException {
    @SuppressWarnings("PMD.CloseResource")
    JsonParser parser = getReader();
    ObjectNode node = parser.readValueAsTree();

    String discriminatorProperty = instance.getJsonDiscriminatorProperty();
    JsonNode discriminatorNode = node.get(discriminatorProperty);
    if (discriminatorNode == null) {
      throw new IllegalArgumentException(String.format(
          "Unable to find discriminator property '%s' for object at '%s'.",
          discriminatorProperty,
          JsonUtil.toString(parser, getSource())));
    }
    String discriminator = ObjectUtils.requireNonNull(discriminatorNode.asText());

    IBoundInstanceModelGroupedNamed actualInstance = instance.getGroupedModelInstance(discriminator);
    assert actualInstance != null;

    IBoundObject retval;
    try (JsonParser newParser = node.traverse(parser.getCodec())) {
      assert newParser != null;
      push(newParser);

      // get initial token
      retval = actualInstance.readItem(parentItem, this);
      assert newParser.currentToken() == null;
      pop(newParser);
    }

    // advance the original parser to the next token
    parser.nextToken();

    return retval;
  }

  private final class JsonKeyBodyHandler implements DefinitionBodyHandler<IBoundDefinitionModelComplex> {
    @NonNull
    private final IBoundInstanceFlag jsonKey;
    @NonNull
    private final DefinitionBodyHandler<IBoundDefinitionModelComplex> bodyHandler;

    private JsonKeyBodyHandler(
        @NonNull IBoundInstanceFlag jsonKey,
        @NonNull DefinitionBodyHandler<IBoundDefinitionModelComplex> bodyHandler) {
      this.jsonKey = jsonKey;
      this.bodyHandler = bodyHandler;
    }

    @Override
    public void accept(
        IBoundDefinitionModelComplex definition,
        IBoundObject parent,
        IJsonProblemHandler problemHandler)
        throws IOException {
      @SuppressWarnings("PMD.CloseResource")
      JsonParser parser = getReader();
      URI resource = getSource();
      JsonUtil.assertCurrent(parser, resource, JsonToken.FIELD_NAME);

      // the field will be the JSON key
      String key = ObjectUtils.notNull(parser.currentName());
      try {
        Object value = jsonKey.getDefinition().getJavaTypeAdapter().parse(key);
        jsonKey.setValue(parent, ObjectUtils.notNull(value.toString()));
      } catch (IllegalArgumentException ex) {
        throw new IOException(
            String.format("Malformed data '%s'%s. %s",
                key,
                JsonUtil.generateLocationMessage(parser, resource),
                ex.getLocalizedMessage()),
            ex);
      }

      // skip to the next token
      parser.nextToken();
      // JsonUtil.assertCurrent(parser, JsonToken.START_OBJECT);

      // // advance past the JSON key's start object
      // JsonUtil.assertAndAdvance(parser, JsonToken.START_OBJECT);

      // read the property values
      bodyHandler.accept(definition, parent, problemHandler);

      // // advance past the JSON key's end object
      // JsonUtil.assertAndAdvance(parser, JsonToken.END_OBJECT);
    }
  }

  private final class PropertyBodyHandler implements DefinitionBodyHandler<IBoundDefinitionModelComplex> {
    @NonNull
    private final Map<String, IBoundProperty<?>> jsonProperties;

    private PropertyBodyHandler(@NonNull Map<String, IBoundProperty<?>> jsonProperties) {
      this.jsonProperties = jsonProperties;
    }

    @Override
    public void accept(
        IBoundDefinitionModelComplex definition,
        IBoundObject parent,
        IJsonProblemHandler problemHandler)
        throws IOException {
      @SuppressWarnings("PMD.CloseResource")
      JsonParser parser = getReader();
      URI resource = getSource();

      // advance past the start object
      JsonUtil.assertAndAdvance(parser, resource, JsonToken.START_OBJECT);

      // make a copy, since we use the remaining values to initialize default values
      Map<String, IBoundProperty<?>> remainingInstances = new HashMap<>(jsonProperties); // NOPMD not concurrent

      // handle each property
      while (JsonToken.FIELD_NAME.equals(parser.currentToken())) {

        // the parser's current token should be the JSON field name
        String propertyName = ObjectUtils.notNull(parser.currentName());
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("reading property {}", propertyName);
        }

        IBoundProperty<?> property = remainingInstances.get(propertyName);

        boolean handled = false;
        if (property != null) {
          // advance past the field name
          parser.nextToken();

          Object value = readObjectProperty(parent, property);
          if (value != null) {
            property.setValue(parent, value);
          }

          // mark handled
          remainingInstances.remove(propertyName);
          handled = true;
        }

        if (!handled && !problemHandler.handleUnknownProperty(
            definition,
            parent,
            propertyName,
            MetaschemaJsonReader.this)) {
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Skipping unhandled JSON field '{}' {}.", propertyName, JsonUtil.toString(parser, resource));
          }
          JsonUtil.assertAndAdvance(parser, resource, JsonToken.FIELD_NAME);
          JsonUtil.skipNextValue(parser, resource);
        }

        // the current token will be either the next instance field name or the end of
        // the parent object
        JsonUtil.assertCurrent(parser, resource, JsonToken.FIELD_NAME, JsonToken.END_OBJECT);
      }

      problemHandler.handleMissingInstances(
          definition,
          parent,
          ObjectUtils.notNull(remainingInstances.values()));

      // advance past the end object
      JsonUtil.assertAndAdvance(parser, resource, JsonToken.END_OBJECT);
    }
  }

  private static final class GroupedInstanceProblemHandler implements IJsonProblemHandler {
    @NonNull
    private final IBoundInstanceModelGroupedNamed instance;
    @NonNull
    private final IJsonProblemHandler delegate;

    private GroupedInstanceProblemHandler(
        @NonNull IBoundInstanceModelGroupedNamed instance,
        @NonNull IJsonProblemHandler delegate) {
      this.instance = instance;
      this.delegate = delegate;
    }

    @Override
    public void handleMissingInstances(
        IBoundDefinitionModelComplex parentDefinition,
        IBoundObject targetObject,
        Collection<? extends IBoundProperty<?>> unhandledInstances) throws IOException {
      delegate.handleMissingInstances(parentDefinition, targetObject, unhandledInstances);
    }

    @Override
    public boolean handleUnknownProperty(
        IBoundDefinitionModelComplex definition,
        IBoundObject parentItem,
        String fieldName,
        IJsonParsingContext parsingContext) throws IOException {
      boolean retval;
      if (instance.getParentContainer().getJsonDiscriminatorProperty().equals(fieldName)) {
        JsonUtil.skipNextValue(parsingContext.getReader(), parsingContext.getSource());
        retval = true;
      } else {
        retval = delegate.handleUnknownProperty(definition, parentItem, fieldName, parsingContext);
      }
      return retval;
    }
  }

  private final class JsomValueKeyProblemHandler implements IJsonProblemHandler {
    @NonNull
    private final IJsonProblemHandler delegate;
    @NonNull
    private final IBoundInstanceFlag jsonValueKeyFlag;
    private boolean foundJsonValueKey; // false

    private JsomValueKeyProblemHandler(
        @NonNull IJsonProblemHandler delegate,
        @NonNull IBoundInstanceFlag jsonValueKeyFlag) {
      this.delegate = delegate;
      this.jsonValueKeyFlag = jsonValueKeyFlag;
    }

    @Override
    public void handleMissingInstances(
        IBoundDefinitionModelComplex parentDefinition,
        IBoundObject targetObject,
        Collection<? extends IBoundProperty<?>> unhandledInstances) throws IOException {
      delegate.handleMissingInstances(parentDefinition, targetObject, unhandledInstances);
    }

    @Override
    public boolean handleUnknownProperty(
        IBoundDefinitionModelComplex definition,
        IBoundObject parentItem,
        String fieldName,
        IJsonParsingContext parsingContext) throws IOException {
      boolean retval;
      if (foundJsonValueKey) {
        retval = delegate.handleUnknownProperty(definition, parentItem, fieldName, parsingContext);
      } else {
        @SuppressWarnings("PMD.CloseResource")
        JsonParser parser = parsingContext.getReader();
        URI resource = parsingContext.getSource();

        // handle JSON value key
        String key = ObjectUtils.notNull(parser.currentName());
        try {
          Object keyValue = jsonValueKeyFlag.getJavaTypeAdapter().parse(key);
          jsonValueKeyFlag.setValue(ObjectUtils.notNull(parentItem), keyValue);
        } catch (IllegalArgumentException ex) {
          throw new IOException(
              String.format("Malformed data '%s'%s. %s",
                  key,
                  JsonUtil.generateLocationMessage(parser, resource),
                  ex.getLocalizedMessage()),
              ex);
        }
        // advance past the field name
        JsonUtil.assertAndAdvance(parser, resource, JsonToken.FIELD_NAME);

        IBoundFieldValue fieldValue = ((IBoundDefinitionModelFieldComplex) definition).getFieldValue();
        Object value = readItemFieldValue(ObjectUtils.notNull(parentItem), fieldValue);
        if (value != null) {
          fieldValue.setValue(ObjectUtils.notNull(parentItem), value);
        }

        retval = foundJsonValueKey = true;
      }
      return retval;
    }
  }

  private class ModelInstanceReadHandler<ITEM>
      extends AbstractModelInstanceReadHandler<ITEM> {

    protected ModelInstanceReadHandler(
        @NonNull IBoundInstanceModel<ITEM> instance,
        @NonNull IBoundObject parentItem) {
      super(instance, parentItem);
    }

    @Override
    public List<ITEM> readList() throws IOException {
      @SuppressWarnings("PMD.CloseResource")
      JsonParser parser = getReader();
      URI resource = getSource();

      List<ITEM> items = new LinkedList<>();
      switch (parser.currentToken()) {
      case START_ARRAY:
        // this is an array, we need to parse the array wrapper then each item
        JsonUtil.assertAndAdvance(parser, resource, JsonToken.START_ARRAY);

        // parse items
        while (!JsonToken.END_ARRAY.equals(parser.currentToken())) {
          items.add(readItem());
        }

        // this is the other side of the array wrapper, advance past it
        JsonUtil.assertAndAdvance(parser, resource, JsonToken.END_ARRAY);
        break;
      case VALUE_NULL:
        JsonUtil.assertAndAdvance(parser, resource, JsonToken.VALUE_NULL);
        break;
      default:
        // this is a singleton, just parse the value as a single item
        items.add(readItem());
        break;
      }
      return items;
    }

    @Override
    public Map<String, ITEM> readMap() throws IOException {
      @SuppressWarnings("PMD.CloseResource")
      JsonParser parser = getReader();
      URI resource = getSource();

      IBoundInstanceModel<?> instance = getCollectionInfo().getInstance();

      @SuppressWarnings("PMD.UseConcurrentHashMap")
      Map<String, ITEM> items = new LinkedHashMap<>();

      // A map value is always wrapped in a START_OBJECT, since fields are used for
      // the keys
      JsonUtil.assertAndAdvance(parser, resource, JsonToken.START_OBJECT);

      // process all map items
      while (!JsonToken.END_OBJECT.equals(parser.currentToken())) {

        // a map item will always start with a FIELD_NAME, since this represents the key
        JsonUtil.assertCurrent(parser, resource, JsonToken.FIELD_NAME);

        // get the object, since it must have a JSON key
        ITEM item = readItem();
        if (item == null) {
          throw new IOException(String.format("Null object encountered'%s.",
              JsonUtil.generateLocationMessage(parser, resource)));
        }

        // lookup the key
        IBoundInstanceFlag jsonKey = instance.getItemJsonKey(item);
        assert jsonKey != null;

        Object keyValue = jsonKey.getValue(item);
        if (keyValue == null) {
          throw new IOException(String.format("Null value for json-key for definition '%s'",
              jsonKey.getContainingDefinition().toCoordinates()));
        }
        String key;
        try {
          key = jsonKey.getJavaTypeAdapter().asString(keyValue);
        } catch (IllegalArgumentException ex) {
          throw new IOException(
              String.format("Malformed data '%s'%s. %s",
                  keyValue,
                  JsonUtil.generateLocationMessage(parser, resource),
                  ex.getLocalizedMessage()),
              ex);
        }
        items.put(key, item);

        // the next item will be a FIELD_NAME, or we will encounter an END_OBJECT if all
        // items have been
        // read
        JsonUtil.assertCurrent(parser, resource, JsonToken.FIELD_NAME, JsonToken.END_OBJECT);
      }

      // A map value will always end with an end object, which needs to be consumed
      JsonUtil.assertAndAdvance(parser, resource, JsonToken.END_OBJECT);

      return items;
    }

    @Override
    public ITEM readItem() throws IOException {
      IBoundInstanceModel<ITEM> instance = getCollectionInfo().getInstance();
      return instance.readItem(getParentObject(), MetaschemaJsonReader.this);
    }
  }

  @SuppressWarnings("PMD.DataClass")
  private static class MetaschemaData implements IMetaschemaData {
    private final int line;
    private final int column;
    private final long charOffset;
    private final long byteOffset;

    public MetaschemaData(@NonNull JsonLocation location) {
      this.line = location.getLineNr();
      this.column = location.getColumnNr();
      this.charOffset = location.getCharOffset();
      this.byteOffset = location.getByteOffset();
    }

    @Override
    public int getLine() {
      return line;
    }

    @Override
    public int getColumn() {
      return column;
    }

    @Override
    public long getCharOffset() {
      return charOffset;
    }

    @Override
    public long getByteOffset() {
      return byteOffset;
    }
  }

  @FunctionalInterface
  private interface DefinitionBodyHandler<DEF extends IBoundDefinitionModelComplex> {
    void accept(
        @NonNull DEF definition,
        @NonNull IBoundObject parent,
        @NonNull IJsonProblemHandler problemHandler) throws IOException;
  }
}
