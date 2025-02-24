/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.databind.model.metaschema.binding;

import gov.nist.secauto.metaschema.core.datatype.adapter.NonNegativeIntegerAdapter;
import gov.nist.secauto.metaschema.core.datatype.adapter.PositiveIntegerAdapter;
import gov.nist.secauto.metaschema.core.datatype.adapter.StringAdapter;
import gov.nist.secauto.metaschema.core.datatype.adapter.TokenAdapter;
import gov.nist.secauto.metaschema.core.datatype.adapter.UriAdapter;
import gov.nist.secauto.metaschema.core.datatype.adapter.UriReferenceAdapter;
import gov.nist.secauto.metaschema.core.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.core.datatype.markup.MarkupLineAdapter;
import gov.nist.secauto.metaschema.core.model.IBoundObject;
import gov.nist.secauto.metaschema.core.model.IMetaschemaData;
import gov.nist.secauto.metaschema.core.model.JsonGroupAsBehavior;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraint;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.model.annotations.AllowedValue;
import gov.nist.secauto.metaschema.databind.model.annotations.AllowedValues;
import gov.nist.secauto.metaschema.databind.model.annotations.BoundAssembly;
import gov.nist.secauto.metaschema.databind.model.annotations.BoundChoiceGroup;
import gov.nist.secauto.metaschema.databind.model.annotations.BoundField;
import gov.nist.secauto.metaschema.databind.model.annotations.BoundFieldValue;
import gov.nist.secauto.metaschema.databind.model.annotations.BoundFlag;
import gov.nist.secauto.metaschema.databind.model.annotations.BoundGroupedAssembly;
import gov.nist.secauto.metaschema.databind.model.annotations.Expect;
import gov.nist.secauto.metaschema.databind.model.annotations.GroupAs;
import gov.nist.secauto.metaschema.databind.model.annotations.Index;
import gov.nist.secauto.metaschema.databind.model.annotations.IsUnique;
import gov.nist.secauto.metaschema.databind.model.annotations.KeyField;
import gov.nist.secauto.metaschema.databind.model.annotations.Let;
import gov.nist.secauto.metaschema.databind.model.annotations.MetaschemaAssembly;
import gov.nist.secauto.metaschema.databind.model.annotations.MetaschemaField;
import gov.nist.secauto.metaschema.databind.model.annotations.ValueConstraints;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigInteger;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * A declaration of the Metaschema module.
 */
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",
    "PMD.DataClass",
    "PMD.ExcessivePublicCount",
    "PMD.FieldNamingConventions",
    "PMD.TooManyFields"
})
@MetaschemaAssembly(
    formalName = "Metaschema Module",
    description = "A declaration of the Metaschema module.",
    name = "METASCHEMA",
    moduleClass = MetaschemaModelModule.class,
    rootName = "METASCHEMA",
    valueConstraints = @ValueConstraints(lets = {
        @Let(name = "all-imports",
            target = "recurse-depth('for $import in ./import return doc(resolve-uri($import/@href))/METASCHEMA')"),
        @Let(name = "deprecated-type-map",
            target = "map { 'base64Binary':'base64','dateTime':'date-time','dateTime-with-timezone':'date-time-with-timezone','email':'email-address','nonNegativeInteger':'non-negative-integer','positiveInteger':'positive-integer' }") },
        expect = {
            @Expect(id = "module-top-level-version-required",
                formalName = "Require Schema Version for Top-Level Modules",
                description = "A top-level module, a module that is not marked as @abstract='yes', must have a schema version specified.",
                level = IConstraint.Level.WARNING, target = ".[not(@abstract) or @abstract='no']",
                test = "schema-version",
                message = "Unless marked as @abstract='yes', a Metaschema module (or an imported module) should have a schema version."),
            @Expect(id = "module-top-level-root-required",
                formalName = "Require Root Assembly for Top-Level Modules",
                description = "A top-level module, a module that is not marked as @abstract='yes', must have at least one assembly with a root-name.",
                level = IConstraint.Level.WARNING, target = ".[not(@abstract) or @abstract='no']",
                test = "exists($all-imports/define-assembly/root-name)",
                message = "Unless marked as @abstract='yes', a Metaschema module (or an imported module) should have at least one assembly with a root-name."),
            @Expect(id = "module-import-href-available", formalName = "Import is Resolvable",
                description = "Ensure each import has a resolvable @href.", level = IConstraint.Level.ERROR,
                target = "import", test = "doc-available(resolve-uri(@href))",
                message = "Unable to access a Metaschema module at '{{ resolve-uri(@href) }}'."),
            @Expect(id = "module-import-href-is-module", formalName = "Import is a Metaschema module",
                description = "Ensure each import is a Metaschema module.", level = IConstraint.Level.ERROR,
                target = "import", test = "doc(resolve-uri(@href))/METASCHEMA ! exists(.)",
                message = "Unable the resource at '{{ resolve-uri(@href) }}' is not a Metaschema module."),
            @Expect(id = "metaschema-deprecated-types", formalName = "Avoid Deprecated Data Type Use",
                description = "Ensure that the data type specified is not one of the legacy Metaschema data types which have been deprecated (i.e. base64Binary, dateTime, dateTime-with-timezone, email, nonNegativeInteger, positiveInteger).",
                level = IConstraint.Level.WARNING,
                target = ".//matches/@datatype|.//(define-field|define-flag)/@as-type",
                test = "not(.=('base64Binary','dateTime','dateTime-with-timezone','email','nonNegativeInteger','positiveInteger'))",
                message = "Use of the type '{ . }' is deprecated. Use '{ $deprecated-type-map(.)}' instead.") }),
    modelConstraints = @gov.nist.secauto.metaschema.databind.model.annotations.AssemblyConstraints(
        index = @Index(id = "module-short-name-unique", formalName = "Unique Module Short Names",
            description = "Ensures that the current and all imported modules have a unique short name.",
            level = IConstraint.Level.ERROR, target = "(.|$all-imports)", name = "metaschema-metadata-short-name-index",
            keyFields = @KeyField(target = "@short-name")),
        unique = { @IsUnique(id = "module-namespace-unique-entry", formalName = "Require Unique Namespace Entries",
            description = "Ensures that all declared namespace entries are unique.", level = IConstraint.Level.ERROR,
            target = "namespace-binding", keyFields = { @KeyField(target = "@prefix"), @KeyField(target = "@uri") }),
            @IsUnique(id = "module-namespace-unique-prefix", formalName = "Require Unique Namespace Entry Prefixes",
                description = "Ensures that all declared namespace entries have a unique prefix.",
                level = IConstraint.Level.ERROR, target = "namespace-binding",
                keyFields = @KeyField(target = "@prefix")) }))
public class METASCHEMA implements IBoundObject {
  private final IMetaschemaData __metaschemaData;

  /**
   * Determines if the Metaschema module is abstract ("yes") or not ("no").
   */
  @BoundFlag(
      formalName = "Is Abstract?",
      description = "Determines if the Metaschema module is abstract ('yes') or not ('no').",
      name = "abstract",
      defaultValue = "no",
      typeAdapter = TokenAdapter.class,
      valueConstraints = @ValueConstraints(allowedValues = @AllowedValues(level = IConstraint.Level.ERROR,
          values = { @AllowedValue(value = "yes", description = "The module is abstract."),
              @AllowedValue(value = "no", description = "The module is not abstract.") })))
  private String _abstract;

  @BoundField(
      formalName = "Module Name",
      description = "The name of the information model represented by this Metaschema definition.",
      useName = "schema-name",
      minOccurs = 1,
      typeAdapter = MarkupLineAdapter.class)
  private MarkupLine _schemaName;

  @BoundField(
      description = "A version string used to distinguish between multiple revisions of the same Metaschema module.",
      useName = "schema-version",
      minOccurs = 1)
  private String _schemaVersion;

  @BoundField(
      formalName = "Module Short Name",
      description = "A short (code) name to be used for the Metaschema module. This name may be used as a constituent of names assigned to derived artifacts, such as schemas and conversion utilities.",
      useName = "short-name",
      minOccurs = 1,
      typeAdapter = TokenAdapter.class)
  private String _shortName;

  @BoundField(
      formalName = "Module Collection Namespace",
      description = "The namespace for the collection of Metaschema module this Metaschema module belongs to. This value is also used as the XML namespace governing the names of elements in XML documents. By using this namespace, documents and document fragments used in mixed-format environments may be distinguished from neighbor XML formats using another namespaces. This value is not reflected in Metaschema JSON.",
      useName = "namespace",
      minOccurs = 1,
      typeAdapter = UriAdapter.class)
  private URI _namespace;

  @BoundField(
      formalName = "JSON Base URI",
      description = "The JSON Base URI is the nominal base URI assigned to a JSON Schema instance expressing the model defined by this Metaschema module.",
      useName = "json-base-uri",
      minOccurs = 1,
      typeAdapter = UriAdapter.class)
  private URI _jsonBaseUri;

  @BoundField(
      formalName = "Remarks",
      description = "Any explanatory or helpful information to be provided about the remarks parent.",
      useName = "remarks")
  private Remarks _remarks;

  @BoundAssembly(
      formalName = "Module Import",
      description = "Imports a set of Metaschema modules contained in another resource. Imports support the reuse of common information structures.",
      useName = "import",
      maxOccurs = -1,
      groupAs = @GroupAs(name = "imports", inJson = JsonGroupAsBehavior.LIST))
  private List<Import> _imports;

  @BoundAssembly(
      formalName = "Metapath Namespace Declaration",
      description = "Assigns a Metapath namespace to a prefix for use in a Metapath expression in a lexical qualified name.",
      useName = "namespace-binding",
      maxOccurs = -1,
      groupAs = @GroupAs(name = "namespace-bindings"))
  private List<MetapathNamespace> _namespaceBindings;

  @BoundChoiceGroup(
      maxOccurs = -1,
      assemblies = {
          @BoundGroupedAssembly(formalName = "Global Assembly Definition",
              description = "In XML, an element with structured element content. In JSON, an object with properties. Defined globally, an assembly can be assigned to appear in the `model` of any assembly (another assembly type, or itself), by `assembly` reference.",
              useName = "define-assembly", binding = DefineAssembly.class),
          @BoundGroupedAssembly(formalName = "Global Field Definition", useName = "define-field",
              binding = DefineField.class),
          @BoundGroupedAssembly(formalName = "Global Flag Definition", useName = "define-flag",
              binding = DefineFlag.class)
      },
      groupAs = @GroupAs(name = "definitions", inJson = JsonGroupAsBehavior.LIST))
  private List<Object> _definitions;

  public METASCHEMA() {
    this(null);
  }

  public METASCHEMA(IMetaschemaData data) {
    this.__metaschemaData = data;
  }

  @Override
  public IMetaschemaData getMetaschemaData() {
    return __metaschemaData;
  }

  public String getAbstract() {
    return _abstract;
  }

  public void setAbstract(String value) {
    _abstract = value;
  }

  public MarkupLine getSchemaName() {
    return _schemaName;
  }

  public void setSchemaName(MarkupLine value) {
    _schemaName = value;
  }

  public String getSchemaVersion() {
    return _schemaVersion;
  }

  public void setSchemaVersion(String value) {
    _schemaVersion = value;
  }

  public String getShortName() {
    return _shortName;
  }

  public void setShortName(String value) {
    _shortName = value;
  }

  public URI getNamespace() {
    return _namespace;
  }

  public void setNamespace(URI value) {
    _namespace = value;
  }

  public URI getJsonBaseUri() {
    return _jsonBaseUri;
  }

  public void setJsonBaseUri(URI value) {
    _jsonBaseUri = value;
  }

  public Remarks getRemarks() {
    return _remarks;
  }

  public void setRemarks(Remarks value) {
    _remarks = value;
  }

  public List<Import> getImports() {
    return _imports;
  }

  public void setImports(List<Import> value) {
    _imports = value;
  }

  /**
   * Add a new {@link Import} item to the underlying collection.
   *
   * @param item
   *          the item to add
   * @return {@code true}
   */
  public boolean addImport(Import item) {
    Import value = ObjectUtils.requireNonNull(item, "item cannot be null");
    if (_imports == null) {
      _imports = new LinkedList<>();
    }
    return _imports.add(value);
  }

  /**
   * Remove the first matching {@link Import} item from the underlying collection.
   *
   * @param item
   *          the item to remove
   * @return {@code true} if the item was removed or {@code false} otherwise
   */
  public boolean removeImport(Import item) {
    Import value = ObjectUtils.requireNonNull(item, "item cannot be null");
    return _imports != null && _imports.remove(value);
  }

  public List<MetapathNamespace> getNamespaceBindings() {
    return _namespaceBindings;
  }

  public void setNamespaceBindings(List<MetapathNamespace> value) {
    _namespaceBindings = value;
  }

  /**
   * Add a new {@link MetapathNamespace} item to the underlying collection.
   *
   * @param item
   *          the item to add
   * @return {@code true}
   */
  public boolean addNamespaceBinding(MetapathNamespace item) {
    MetapathNamespace value = ObjectUtils.requireNonNull(item, "item cannot be null");
    if (_namespaceBindings == null) {
      _namespaceBindings = new LinkedList<>();
    }
    return _namespaceBindings.add(value);
  }

  /**
   * Remove the first matching {@link MetapathNamespace} item from the underlying
   * collection.
   *
   * @param item
   *          the item to remove
   * @return {@code true} if the item was removed or {@code false} otherwise
   */
  public boolean removeNamespaceBinding(MetapathNamespace item) {
    MetapathNamespace value = ObjectUtils.requireNonNull(item, "item cannot be null");
    return _namespaceBindings != null && _namespaceBindings.remove(value);
  }

  public List<Object> getDefinitions() {
    return _definitions;
  }

  public void setDefinitions(List<Object> value) {
    _definitions = value;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
  }

  /**
   * Imports a set of Metaschema modules contained in another resource. Imports
   * support the reuse of common information structures.
   */
  @MetaschemaAssembly(
      formalName = "Module Import",
      description = "Imports a set of Metaschema modules contained in another resource. Imports support the reuse of common information structures.",
      name = "import",
      moduleClass = MetaschemaModelModule.class)
  public static class Import implements IBoundObject {
    private final IMetaschemaData __metaschemaData;

    /**
     * "A relative or absolute URI for retrieving an out-of-line Metaschema
     * definition."
     */
    @BoundFlag(
        formalName = "Import URI Reference",
        description = "A relative or absolute URI for retrieving an out-of-line Metaschema definition.",
        name = "href",
        required = true,
        typeAdapter = UriReferenceAdapter.class)
    private URI _href;

    public Import() {
      this(null);
    }

    public Import(IMetaschemaData data) {
      this.__metaschemaData = data;
    }

    @Override
    public IMetaschemaData getMetaschemaData() {
      return __metaschemaData;
    }

    public URI getHref() {
      return _href;
    }

    public void setHref(URI value) {
      _href = value;
    }

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
    }
  }

  /**
   * In XML, an element with structured element content. In JSON, an object with
   * properties. Defined globally, an assembly can be assigned to appear in the
   * <code>model</code> of any assembly (another assembly type, or itself), by
   * <code>assembly</code> reference.
   */
  @MetaschemaAssembly(
      formalName = "Global Assembly Definition",
      description = "In XML, an element with structured element content. In JSON, an object with properties. Defined globally, an assembly can be assigned to appear in the `model` of any assembly (another assembly type, or itself), by `assembly` reference.",
      name = "define-assembly",
      moduleClass = MetaschemaModelModule.class)
  public static class DefineAssembly implements IBoundObject {
    private final IMetaschemaData __metaschemaData;

    @BoundFlag(
        formalName = "Global Assembly Name",
        name = "name",
        required = true,
        typeAdapter = TokenAdapter.class)
    private String _name;

    @BoundFlag(
        formalName = "Global Assembly Binary Name",
        name = "index",
        typeAdapter = PositiveIntegerAdapter.class)
    private BigInteger _index;

    @BoundFlag(
        formalName = "Definition Scope",
        name = "scope",
        defaultValue = "global",
        typeAdapter = TokenAdapter.class,
        valueConstraints = @ValueConstraints(allowedValues = @AllowedValues(level = IConstraint.Level.ERROR, values = {
            @AllowedValue(value = "local",
                description = "This definition is only available in the context of the current Metaschema module."),
            @AllowedValue(value = "global",
                description = "This definition will be made available to any Metaschema module that includes this one either directly or indirectly through a chain of imported Metaschemas.") })))
    private String _scope;

    @BoundFlag(
        formalName = "Deprecated Version",
        name = "deprecated",
        typeAdapter = StringAdapter.class)
    private String _deprecated;

    @BoundField(
        formalName = "Formal Name",
        description = "A formal name for the data construct, to be presented in documentation.",
        useName = "formal-name")
    private String _formalName;

    @BoundField(
        formalName = "Description",
        description = "A short description of the data construct's purpose, describing the constructs semantics.",
        useName = "description",
        typeAdapter = MarkupLineAdapter.class)
    private MarkupLine _description;

    @BoundAssembly(
        formalName = "Property",
        useName = "prop",
        maxOccurs = -1,
        groupAs = @GroupAs(name = "props", inJson = JsonGroupAsBehavior.LIST))
    private List<Property> _props;

    @BoundField(
        formalName = "Use Name",
        description = "Allows the name of the definition to be overridden.",
        useName = "use-name")
    private UseName _useName;

    @BoundField(
        formalName = "Root Name",
        description = "Provides a root name, for when the definition is used as the root of a node hierarchy.",
        useName = "root-name",
        minOccurs = 1)
    private RootName _rootName;

    @BoundAssembly(
        formalName = "JSON Key",
        description = "Used in JSON (and similar formats) to identify a flag that will be used as the property name in an object hold a collection of sibling objects. Requires that siblings must never share `json-key` values.",
        useName = "json-key")
    private JsonKey _jsonKey;

    @BoundChoiceGroup(
        maxOccurs = -1,
        assemblies = {
            @BoundGroupedAssembly(formalName = "Inline Flag Definition", useName = "define-flag",
                binding = InlineDefineFlag.class),
            @BoundGroupedAssembly(formalName = "Flag Reference", useName = "flag", binding = FlagReference.class)
        },
        groupAs = @GroupAs(name = "flags", inJson = JsonGroupAsBehavior.LIST))
    private List<Object> _flags;

    @BoundAssembly(
        useName = "model")
    private AssemblyModel _model;

    @BoundAssembly(
        useName = "constraint")
    private AssemblyConstraints _constraint;

    @BoundField(
        formalName = "Remarks",
        description = "Any explanatory or helpful information to be provided about the remarks parent.",
        useName = "remarks")
    private Remarks _remarks;

    @BoundAssembly(
        formalName = "Example",
        useName = "example",
        maxOccurs = -1,
        groupAs = @GroupAs(name = "examples", inJson = JsonGroupAsBehavior.LIST))
    private List<Example> _examples;

    public DefineAssembly() {
      this(null);
    }

    public DefineAssembly(IMetaschemaData data) {
      this.__metaschemaData = data;
    }

    @Override
    public IMetaschemaData getMetaschemaData() {
      return __metaschemaData;
    }

    public String getName() {
      return _name;
    }

    public void setName(String value) {
      _name = value;
    }

    public BigInteger getIndex() {
      return _index;
    }

    public void setIndex(BigInteger value) {
      _index = value;
    }

    public String getScope() {
      return _scope;
    }

    public void setScope(String value) {
      _scope = value;
    }

    public String getDeprecated() {
      return _deprecated;
    }

    public void setDeprecated(String value) {
      _deprecated = value;
    }

    public String getFormalName() {
      return _formalName;
    }

    public void setFormalName(String value) {
      _formalName = value;
    }

    public MarkupLine getDescription() {
      return _description;
    }

    public void setDescription(MarkupLine value) {
      _description = value;
    }

    public List<Property> getProps() {
      return _props;
    }

    public void setProps(List<Property> value) {
      _props = value;
    }

    /**
     * Add a new {@link Property} item to the underlying collection.
     *
     * @param item
     *          the item to add
     * @return {@code true}
     */
    public boolean addProp(Property item) {
      Property value = ObjectUtils.requireNonNull(item, "item cannot be null");
      if (_props == null) {
        _props = new LinkedList<>();
      }
      return _props.add(value);
    }

    /**
     * Remove the first matching {@link Property} item from the underlying
     * collection.
     *
     * @param item
     *          the item to remove
     * @return {@code true} if the item was removed or {@code false} otherwise
     */
    public boolean removeProp(Property item) {
      Property value = ObjectUtils.requireNonNull(item, "item cannot be null");
      return _props != null && _props.remove(value);
    }

    public UseName getUseName() {
      return _useName;
    }

    public void setUseName(UseName value) {
      _useName = value;
    }

    public RootName getRootName() {
      return _rootName;
    }

    public void setRootName(RootName value) {
      _rootName = value;
    }

    public JsonKey getJsonKey() {
      return _jsonKey;
    }

    public void setJsonKey(JsonKey value) {
      _jsonKey = value;
    }

    public List<Object> getFlags() {
      return _flags;
    }

    public void setFlags(List<Object> value) {
      _flags = value;
    }

    public AssemblyModel getModel() {
      return _model;
    }

    public void setModel(AssemblyModel value) {
      _model = value;
    }

    public AssemblyConstraints getConstraint() {
      return _constraint;
    }

    public void setConstraint(AssemblyConstraints value) {
      _constraint = value;
    }

    public Remarks getRemarks() {
      return _remarks;
    }

    public void setRemarks(Remarks value) {
      _remarks = value;
    }

    public List<Example> getExamples() {
      return _examples;
    }

    public void setExamples(List<Example> value) {
      _examples = value;
    }

    /**
     * Add a new {@link Example} item to the underlying collection.
     *
     * @param item
     *          the item to add
     * @return {@code true}
     */
    public boolean addExample(Example item) {
      Example value = ObjectUtils.requireNonNull(item, "item cannot be null");
      if (_examples == null) {
        _examples = new LinkedList<>();
      }
      return _examples.add(value);
    }

    /**
     * Remove the first matching {@link Example} item from the underlying
     * collection.
     *
     * @param item
     *          the item to remove
     * @return {@code true} if the item was removed or {@code false} otherwise
     */
    public boolean removeExample(Example item) {
      Example value = ObjectUtils.requireNonNull(item, "item cannot be null");
      return _examples != null && _examples.remove(value);
    }

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
    }

    /**
     * Provides a root name, for when the definition is used as the root of a node
     * hierarchy.
     */
    @MetaschemaField(
        formalName = "Root Name",
        description = "Provides a root name, for when the definition is used as the root of a node hierarchy.",
        name = "root-name",
        moduleClass = MetaschemaModelModule.class)
    public static class RootName implements IBoundObject {
      private final IMetaschemaData __metaschemaData;

      /**
       * "Used for binary formats instead of the textual name."
       */
      @BoundFlag(
          formalName = "Numeric Index",
          description = "Used for binary formats instead of the textual name.",
          name = "index",
          typeAdapter = NonNegativeIntegerAdapter.class)
      private BigInteger _index;

      @BoundFieldValue(
          valueKeyName = "name",
          typeAdapter = TokenAdapter.class)
      private String _name;

      public RootName() {
        this(null);
      }

      public RootName(IMetaschemaData data) {
        this.__metaschemaData = data;
      }

      @Override
      public IMetaschemaData getMetaschemaData() {
        return __metaschemaData;
      }

      public BigInteger getIndex() {
        return _index;
      }

      public void setIndex(BigInteger value) {
        _index = value;
      }

      public String getName() {
        return _name;
      }

      public void setName(String value) {
        _name = value;
      }

      @Override
      public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
      }
    }
  }

  @MetaschemaAssembly(
      formalName = "Global Field Definition",
      name = "define-field",
      moduleClass = MetaschemaModelModule.class)
  public static class DefineField implements IBoundObject {
    private final IMetaschemaData __metaschemaData;

    @BoundFlag(
        formalName = "Global Field Name",
        name = "name",
        required = true,
        typeAdapter = TokenAdapter.class)
    private String _name;

    @BoundFlag(
        formalName = "Global Field Binary Name",
        name = "index",
        typeAdapter = PositiveIntegerAdapter.class)
    private BigInteger _index;

    @BoundFlag(
        formalName = "Definition Scope",
        name = "scope",
        defaultValue = "global",
        typeAdapter = TokenAdapter.class,
        valueConstraints = @ValueConstraints(allowedValues = @AllowedValues(level = IConstraint.Level.ERROR, values = {
            @AllowedValue(value = "local",
                description = "This definition is only available in the context of the current Metaschema module."),
            @AllowedValue(value = "global",
                description = "This definition will be made available to any Metaschema module that includes this one either directly or indirectly through a chain of imported Metaschemas.") })))
    private String _scope;

    @BoundFlag(
        formalName = "Deprecated Version",
        name = "deprecated",
        typeAdapter = StringAdapter.class)
    private String _deprecated;

    @BoundFlag(
        formalName = "Field Value Data Type",
        name = "as-type",
        defaultValue = "string",
        typeAdapter = TokenAdapter.class,
        valueConstraints = @ValueConstraints(allowedValues = @AllowedValues(level = IConstraint.Level.ERROR,
            allowOthers = true,
            values = { @AllowedValue(value = "markup-line",
                description = "The [markup-line](https://pages.nist.gov/metaschema/specification/datatypes/#markup-line) data type."),
                @AllowedValue(value = "markup-multiline",
                    description = "The [markup-multiline](https://pages.nist.gov/metaschema/specification/datatypes/#markup-multiline) data type."),
                @AllowedValue(value = "base64",
                    description = "The [base64](https://pages.nist.gov/metaschema/specification/datatypes/#base64) data type."),
                @AllowedValue(value = "boolean",
                    description = "The [boolean](https://pages.nist.gov/metaschema/specification/datatypes/#boolean) data type."),
                @AllowedValue(value = "date",
                    description = "The [date](https://pages.nist.gov/metaschema/specification/datatypes/#date) data type."),
                @AllowedValue(value = "date-time",
                    description = "The [date-time](https://pages.nist.gov/metaschema/specification/datatypes/#date-time) data type."),
                @AllowedValue(value = "date-time-with-timezone",
                    description = "The [date-time-with-timezone](https://pages.nist.gov/metaschema/specification/datatypes/#date-time-with-timezone) data type."),
                @AllowedValue(value = "date-with-timezone",
                    description = "The [date-with-timezone](https://pages.nist.gov/metaschema/specification/datatypes/#date-with-timezone) data type."),
                @AllowedValue(value = "day-time-duration",
                    description = "The [day-time-duration](https://pages.nist.gov/metaschema/specification/datatypes/#day-time-duration) data type."),
                @AllowedValue(value = "decimal",
                    description = "The [decimal](https://pages.nist.gov/metaschema/specification/datatypes/#decimal) data type."),
                @AllowedValue(value = "email-address",
                    description = "The [email-address](https://pages.nist.gov/metaschema/specification/datatypes/#email-address) data type."),
                @AllowedValue(value = "hostname",
                    description = "The [hostname](https://pages.nist.gov/metaschema/specification/datatypes/#hostname) data type."),
                @AllowedValue(value = "integer",
                    description = "The [integer](https://pages.nist.gov/metaschema/specification/datatypes/#integer) data type."),
                @AllowedValue(value = "ip-v4-address",
                    description = "The [ip-v4-address](https://pages.nist.gov/metaschema/specification/datatypes/#ip-v4-address) data type."),
                @AllowedValue(value = "ip-v6-address",
                    description = "The [ip-v6-address](https://pages.nist.gov/metaschema/specification/datatypes/#ip-v6-address) data type."),
                @AllowedValue(value = "non-negative-integer",
                    description = "The [non-negative-integer](https://pages.nist.gov/metaschema/specification/datatypes/#non-negative-integer) data type."),
                @AllowedValue(value = "positive-integer",
                    description = "The [positive-integer](https://pages.nist.gov/metaschema/specification/datatypes/#positive-integer) data type."),
                @AllowedValue(value = "string",
                    description = "The [string](https://pages.nist.gov/metaschema/specification/datatypes/#string) data type."),
                @AllowedValue(value = "token",
                    description = "The [token](https://pages.nist.gov/metaschema/specification/datatypes/#token) data type."),
                @AllowedValue(value = "uri",
                    description = "The [uri](https://pages.nist.gov/metaschema/specification/datatypes/#uri) data type."),
                @AllowedValue(value = "uri-reference",
                    description = "The [uri-reference](https://pages.nist.gov/metaschema/specification/datatypes/#uri-reference) data type."),
                @AllowedValue(value = "uuid",
                    description = "The [uuid](https://pages.nist.gov/metaschema/specification/datatypes/#uuid) data type."),
                @AllowedValue(value = "base64Binary",
                    description = "An old name which is deprecated for use in favor of the 'base64' data type."),
                @AllowedValue(value = "dateTime",
                    description = "An old name which is deprecated for use in favor of the 'date-time' data type."),
                @AllowedValue(value = "dateTime-with-timezone",
                    description = "An old name which is deprecated for use in favor of the 'date-time-with-timezone' data type."),
                @AllowedValue(value = "email",
                    description = "An old name which is deprecated for use in favor of the 'email-address' data type."),
                @AllowedValue(value = "nonNegativeInteger",
                    description = "An old name which is deprecated for use in favor of the 'non-negative-integer' data type."),
                @AllowedValue(value = "positiveInteger",
                    description = "An old name which is deprecated for use in favor of the 'positive-integer' data type.") })))
    private String _asType;

    @BoundFlag(
        formalName = "Default Field Value",
        name = "default",
        typeAdapter = StringAdapter.class)
    private String _default;

    @BoundField(
        formalName = "Formal Name",
        description = "A formal name for the data construct, to be presented in documentation.",
        useName = "formal-name")
    private String _formalName;

    @BoundField(
        formalName = "Description",
        description = "A short description of the data construct's purpose, describing the constructs semantics.",
        useName = "description",
        typeAdapter = MarkupLineAdapter.class)
    private MarkupLine _description;

    @BoundAssembly(
        formalName = "Property",
        useName = "prop",
        maxOccurs = -1,
        groupAs = @GroupAs(name = "props", inJson = JsonGroupAsBehavior.LIST))
    private List<Property> _props;

    @BoundField(
        formalName = "Use Name",
        description = "Allows the name of the definition to be overridden.",
        useName = "use-name")
    private UseName _useName;

    @BoundAssembly(
        formalName = "JSON Key",
        description = "Used in JSON (and similar formats) to identify a flag that will be used as the property name in an object hold a collection of sibling objects. Requires that siblings must never share `json-key` values.",
        useName = "json-key")
    private JsonKey _jsonKey;

    @BoundField(
        formalName = "Field Value JSON Property Name",
        useName = "json-value-key",
        typeAdapter = TokenAdapter.class)
    private String _jsonValueKey;

    @BoundAssembly(
        formalName = "Flag Used as the Field Value's JSON Property Name",
        useName = "json-value-key-flag")
    private JsonValueKeyFlag _jsonValueKeyFlag;

    @BoundChoiceGroup(
        maxOccurs = -1,
        assemblies = {
            @BoundGroupedAssembly(formalName = "Inline Flag Definition", useName = "define-flag",
                binding = InlineDefineFlag.class),
            @BoundGroupedAssembly(formalName = "Flag Reference", useName = "flag", binding = FlagReference.class)
        },
        groupAs = @GroupAs(name = "flags", inJson = JsonGroupAsBehavior.LIST))
    private List<Object> _flags;

    @BoundAssembly(
        useName = "constraint")
    private FieldConstraints _constraint;

    @BoundField(
        formalName = "Remarks",
        description = "Any explanatory or helpful information to be provided about the remarks parent.",
        useName = "remarks")
    private Remarks _remarks;

    @BoundAssembly(
        formalName = "Example",
        useName = "example",
        maxOccurs = -1,
        groupAs = @GroupAs(name = "examples", inJson = JsonGroupAsBehavior.LIST))
    private List<Example> _examples;

    public DefineField() {
      this(null);
    }

    public DefineField(IMetaschemaData data) {
      this.__metaschemaData = data;
    }

    @Override
    public IMetaschemaData getMetaschemaData() {
      return __metaschemaData;
    }

    public String getName() {
      return _name;
    }

    public void setName(String value) {
      _name = value;
    }

    public BigInteger getIndex() {
      return _index;
    }

    public void setIndex(BigInteger value) {
      _index = value;
    }

    public String getScope() {
      return _scope;
    }

    public void setScope(String value) {
      _scope = value;
    }

    public String getDeprecated() {
      return _deprecated;
    }

    public void setDeprecated(String value) {
      _deprecated = value;
    }

    public String getAsType() {
      return _asType;
    }

    public void setAsType(String value) {
      _asType = value;
    }

    public String getDefault() {
      return _default;
    }

    public void setDefault(String value) {
      _default = value;
    }

    public String getFormalName() {
      return _formalName;
    }

    public void setFormalName(String value) {
      _formalName = value;
    }

    public MarkupLine getDescription() {
      return _description;
    }

    public void setDescription(MarkupLine value) {
      _description = value;
    }

    public List<Property> getProps() {
      return _props;
    }

    public void setProps(List<Property> value) {
      _props = value;
    }

    /**
     * Add a new {@link Property} item to the underlying collection.
     *
     * @param item
     *          the item to add
     * @return {@code true}
     */
    public boolean addProp(Property item) {
      Property value = ObjectUtils.requireNonNull(item, "item cannot be null");
      if (_props == null) {
        _props = new LinkedList<>();
      }
      return _props.add(value);
    }

    /**
     * Remove the first matching {@link Property} item from the underlying
     * collection.
     *
     * @param item
     *          the item to remove
     * @return {@code true} if the item was removed or {@code false} otherwise
     */
    public boolean removeProp(Property item) {
      Property value = ObjectUtils.requireNonNull(item, "item cannot be null");
      return _props != null && _props.remove(value);
    }

    public UseName getUseName() {
      return _useName;
    }

    public void setUseName(UseName value) {
      _useName = value;
    }

    public JsonKey getJsonKey() {
      return _jsonKey;
    }

    public void setJsonKey(JsonKey value) {
      _jsonKey = value;
    }

    public String getJsonValueKey() {
      return _jsonValueKey;
    }

    public void setJsonValueKey(String value) {
      _jsonValueKey = value;
    }

    public JsonValueKeyFlag getJsonValueKeyFlag() {
      return _jsonValueKeyFlag;
    }

    public void setJsonValueKeyFlag(JsonValueKeyFlag value) {
      _jsonValueKeyFlag = value;
    }

    public List<Object> getFlags() {
      return _flags;
    }

    public void setFlags(List<Object> value) {
      _flags = value;
    }

    public FieldConstraints getConstraint() {
      return _constraint;
    }

    public void setConstraint(FieldConstraints value) {
      _constraint = value;
    }

    public Remarks getRemarks() {
      return _remarks;
    }

    public void setRemarks(Remarks value) {
      _remarks = value;
    }

    public List<Example> getExamples() {
      return _examples;
    }

    public void setExamples(List<Example> value) {
      _examples = value;
    }

    /**
     * Add a new {@link Example} item to the underlying collection.
     *
     * @param item
     *          the item to add
     * @return {@code true}
     */
    public boolean addExample(Example item) {
      Example value = ObjectUtils.requireNonNull(item, "item cannot be null");
      if (_examples == null) {
        _examples = new LinkedList<>();
      }
      return _examples.add(value);
    }

    /**
     * Remove the first matching {@link Example} item from the underlying
     * collection.
     *
     * @param item
     *          the item to remove
     * @return {@code true} if the item was removed or {@code false} otherwise
     */
    public boolean removeExample(Example item) {
      Example value = ObjectUtils.requireNonNull(item, "item cannot be null");
      return _examples != null && _examples.remove(value);
    }

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
    }
  }

  @MetaschemaAssembly(
      formalName = "Global Flag Definition",
      name = "define-flag",
      moduleClass = MetaschemaModelModule.class)
  public static class DefineFlag implements IBoundObject {
    private final IMetaschemaData __metaschemaData;

    @BoundFlag(
        formalName = "Global Flag Name",
        name = "name",
        required = true,
        typeAdapter = TokenAdapter.class)
    private String _name;

    @BoundFlag(
        formalName = "Global Flag Binary Name",
        name = "index",
        typeAdapter = PositiveIntegerAdapter.class)
    private BigInteger _index;

    @BoundFlag(
        formalName = "Definition Scope",
        name = "scope",
        defaultValue = "global",
        typeAdapter = TokenAdapter.class,
        valueConstraints = @ValueConstraints(allowedValues = @AllowedValues(level = IConstraint.Level.ERROR, values = {
            @AllowedValue(value = "local",
                description = "This definition is only available in the context of the current Metaschema module."),
            @AllowedValue(value = "global",
                description = "This definition will be made available to any Metaschema module that includes this one either directly or indirectly through a chain of imported Metaschemas.") })))
    private String _scope;

    @BoundFlag(
        formalName = "Deprecated Version",
        name = "deprecated",
        typeAdapter = StringAdapter.class)
    private String _deprecated;

    @BoundFlag(
        formalName = "Flag Value Data Type",
        name = "as-type",
        defaultValue = "string",
        typeAdapter = TokenAdapter.class,
        valueConstraints = @ValueConstraints(allowedValues = @AllowedValues(level = IConstraint.Level.ERROR,
            allowOthers = true,
            values = { @AllowedValue(value = "base64",
                description = "The [base64](https://pages.nist.gov/metaschema/specification/datatypes/#base64) data type."),
                @AllowedValue(value = "boolean",
                    description = "The [boolean](https://pages.nist.gov/metaschema/specification/datatypes/#boolean) data type."),
                @AllowedValue(value = "date",
                    description = "The [date](https://pages.nist.gov/metaschema/specification/datatypes/#date) data type."),
                @AllowedValue(value = "date-time",
                    description = "The [date-time](https://pages.nist.gov/metaschema/specification/datatypes/#date-time) data type."),
                @AllowedValue(value = "date-time-with-timezone",
                    description = "The [date-time-with-timezone](https://pages.nist.gov/metaschema/specification/datatypes/#date-time-with-timezone) data type."),
                @AllowedValue(value = "date-with-timezone",
                    description = "The [date-with-timezone](https://pages.nist.gov/metaschema/specification/datatypes/#date-with-timezone) data type."),
                @AllowedValue(value = "day-time-duration",
                    description = "The [day-time-duration](https://pages.nist.gov/metaschema/specification/datatypes/#day-time-duration) data type."),
                @AllowedValue(value = "decimal",
                    description = "The [decimal](https://pages.nist.gov/metaschema/specification/datatypes/#decimal) data type."),
                @AllowedValue(value = "email-address",
                    description = "The [email-address](https://pages.nist.gov/metaschema/specification/datatypes/#email-address) data type."),
                @AllowedValue(value = "hostname",
                    description = "The [hostname](https://pages.nist.gov/metaschema/specification/datatypes/#hostname) data type."),
                @AllowedValue(value = "integer",
                    description = "The [integer](https://pages.nist.gov/metaschema/specification/datatypes/#integer) data type."),
                @AllowedValue(value = "ip-v4-address",
                    description = "The [ip-v4-address](https://pages.nist.gov/metaschema/specification/datatypes/#ip-v4-address) data type."),
                @AllowedValue(value = "ip-v6-address",
                    description = "The [ip-v6-address](https://pages.nist.gov/metaschema/specification/datatypes/#ip-v6-address) data type."),
                @AllowedValue(value = "non-negative-integer",
                    description = "The [non-negative-integer](https://pages.nist.gov/metaschema/specification/datatypes/#non-negative-integer) data type."),
                @AllowedValue(value = "positive-integer",
                    description = "The [positive-integer](https://pages.nist.gov/metaschema/specification/datatypes/#positive-integer) data type."),
                @AllowedValue(value = "string",
                    description = "The [string](https://pages.nist.gov/metaschema/specification/datatypes/#string) data type."),
                @AllowedValue(value = "token",
                    description = "The [token](https://pages.nist.gov/metaschema/specification/datatypes/#token) data type."),
                @AllowedValue(value = "uri",
                    description = "The [uri](https://pages.nist.gov/metaschema/specification/datatypes/#uri) data type."),
                @AllowedValue(value = "uri-reference",
                    description = "The [uri-reference](https://pages.nist.gov/metaschema/specification/datatypes/#uri-reference) data type."),
                @AllowedValue(value = "uuid",
                    description = "The [uuid](https://pages.nist.gov/metaschema/specification/datatypes/#uuid) data type."),
                @AllowedValue(value = "base64Binary",
                    description = "An old name which is deprecated for use in favor of the 'base64' data type."),
                @AllowedValue(value = "dateTime",
                    description = "An old name which is deprecated for use in favor of the 'date-time' data type."),
                @AllowedValue(value = "dateTime-with-timezone",
                    description = "An old name which is deprecated for use in favor of the 'date-time-with-timezone' data type."),
                @AllowedValue(value = "email",
                    description = "An old name which is deprecated for use in favor of the 'email-address' data type."),
                @AllowedValue(value = "nonNegativeInteger",
                    description = "An old name which is deprecated for use in favor of the 'non-negative-integer' data type."),
                @AllowedValue(value = "positiveInteger",
                    description = "An old name which is deprecated for use in favor of the 'positive-integer' data type.") })))
    private String _asType;

    @BoundFlag(
        formalName = "Default Flag Value",
        name = "default",
        typeAdapter = StringAdapter.class)
    private String _default;

    @BoundField(
        formalName = "Formal Name",
        description = "A formal name for the data construct, to be presented in documentation.",
        useName = "formal-name")
    private String _formalName;

    @BoundField(
        formalName = "Description",
        description = "A short description of the data construct's purpose, describing the constructs semantics.",
        useName = "description",
        typeAdapter = MarkupLineAdapter.class)
    private MarkupLine _description;

    @BoundAssembly(
        formalName = "Property",
        useName = "prop",
        maxOccurs = -1,
        groupAs = @GroupAs(name = "props", inJson = JsonGroupAsBehavior.LIST))
    private List<Property> _props;

    @BoundField(
        formalName = "Use Name",
        description = "Allows the name of the definition to be overridden.",
        useName = "use-name")
    private UseName _useName;

    @BoundAssembly(
        useName = "constraint")
    private FlagConstraints _constraint;

    @BoundField(
        formalName = "Remarks",
        description = "Any explanatory or helpful information to be provided about the remarks parent.",
        useName = "remarks")
    private Remarks _remarks;

    @BoundAssembly(
        formalName = "Example",
        useName = "example",
        maxOccurs = -1,
        groupAs = @GroupAs(name = "examples", inJson = JsonGroupAsBehavior.LIST))
    private List<Example> _examples;

    public DefineFlag() {
      this(null);
    }

    public DefineFlag(IMetaschemaData data) {
      this.__metaschemaData = data;
    }

    @Override
    public IMetaschemaData getMetaschemaData() {
      return __metaschemaData;
    }

    public String getName() {
      return _name;
    }

    public void setName(String value) {
      _name = value;
    }

    public BigInteger getIndex() {
      return _index;
    }

    public void setIndex(BigInteger value) {
      _index = value;
    }

    public String getScope() {
      return _scope;
    }

    public void setScope(String value) {
      _scope = value;
    }

    public String getDeprecated() {
      return _deprecated;
    }

    public void setDeprecated(String value) {
      _deprecated = value;
    }

    public String getAsType() {
      return _asType;
    }

    public void setAsType(String value) {
      _asType = value;
    }

    public String getDefault() {
      return _default;
    }

    public void setDefault(String value) {
      _default = value;
    }

    public String getFormalName() {
      return _formalName;
    }

    public void setFormalName(String value) {
      _formalName = value;
    }

    public MarkupLine getDescription() {
      return _description;
    }

    public void setDescription(MarkupLine value) {
      _description = value;
    }

    public List<Property> getProps() {
      return _props;
    }

    public void setProps(List<Property> value) {
      _props = value;
    }

    /**
     * Add a new {@link Property} item to the underlying collection.
     *
     * @param item
     *          the item to add
     * @return {@code true}
     */
    public boolean addProp(Property item) {
      Property value = ObjectUtils.requireNonNull(item, "item cannot be null");
      if (_props == null) {
        _props = new LinkedList<>();
      }
      return _props.add(value);
    }

    /**
     * Remove the first matching {@link Property} item from the underlying
     * collection.
     *
     * @param item
     *          the item to remove
     * @return {@code true} if the item was removed or {@code false} otherwise
     */
    public boolean removeProp(Property item) {
      Property value = ObjectUtils.requireNonNull(item, "item cannot be null");
      return _props != null && _props.remove(value);
    }

    public UseName getUseName() {
      return _useName;
    }

    public void setUseName(UseName value) {
      _useName = value;
    }

    public FlagConstraints getConstraint() {
      return _constraint;
    }

    public void setConstraint(FlagConstraints value) {
      _constraint = value;
    }

    public Remarks getRemarks() {
      return _remarks;
    }

    public void setRemarks(Remarks value) {
      _remarks = value;
    }

    public List<Example> getExamples() {
      return _examples;
    }

    public void setExamples(List<Example> value) {
      _examples = value;
    }

    /**
     * Add a new {@link Example} item to the underlying collection.
     *
     * @param item
     *          the item to add
     * @return {@code true}
     */
    public boolean addExample(Example item) {
      Example value = ObjectUtils.requireNonNull(item, "item cannot be null");
      if (_examples == null) {
        _examples = new LinkedList<>();
      }
      return _examples.add(value);
    }

    /**
     * Remove the first matching {@link Example} item from the underlying
     * collection.
     *
     * @param item
     *          the item to remove
     * @return {@code true} if the item was removed or {@code false} otherwise
     */
    public boolean removeExample(Example item) {
      Example value = ObjectUtils.requireNonNull(item, "item cannot be null");
      return _examples != null && _examples.remove(value);
    }

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).toString();
    }
  }
}
