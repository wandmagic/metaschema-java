// This grammar is derived from the XPath 3.1 grammar produced by Ken Domino, et al (https://github.com/antlr/grammars-v4/blob/63359bd91593ece31a384acd507ae860d6cf7ff7/xpath/xpath31/XPath31Parser.g4).

parser grammar Metapath10;

options { tokenVocab=Metapath10Lexer; superClass=Metapath10ParserBase; }

// [1]
metapath : expr EOF ;
paramlist : param ( COMMA param)* ;
param : DOLLAR eqname typedeclaration? ;
functionbody : enclosedexpr ;
// [5]
enclosedexpr : OC expr? CC ;
expr : exprsingle ( COMMA exprsingle)* ;
exprsingle : forexpr | letexpr | quantifiedexpr  | ifexpr | orexpr ;
forexpr : simpleforclause KW_RETURN exprsingle ;
simpleforclause : KW_FOR simpleforbinding ( COMMA simpleforbinding)* ;
// [10]
simpleforbinding : DOLLAR varname KW_IN exprsingle ;
letexpr :    simpleletclause KW_RETURN exprsingle ;
simpleletclause : KW_LET simpleletbinding ( COMMA simpleletbinding)* ;
simpleletbinding : DOLLAR varname CEQ exprsingle ;
quantifiedexpr :    ( KW_SOME | KW_EVERY) DOLLAR varname KW_IN exprsingle ( COMMA DOLLAR varname KW_IN exprsingle)* KW_SATISFIES exprsingle ;
// [15]
ifexpr : KW_IF OP expr CP KW_THEN exprsingle KW_ELSE exprsingle ;
orexpr : andexpr ( KW_OR andexpr )* ;
andexpr : comparisonexpr ( KW_AND comparisonexpr )* ;
// comparisonexpr : stringconcatexpr ( (valuecomp | generalcomp | nodecomp) stringconcatexpr )? ;
comparisonexpr : stringconcatexpr ( (valuecomp | generalcomp) stringconcatexpr )? ;
stringconcatexpr : rangeexpr ( PP rangeexpr )* ;
// [20]
rangeexpr : additiveexpr ( KW_TO additiveexpr )? ;
additiveexpr : multiplicativeexpr ( (PLUS | MINUS) multiplicativeexpr )* ;
multiplicativeexpr : unionexpr ( (STAR | KW_DIV | KW_IDIV | KW_MOD) unionexpr )* ;
unionexpr : intersectexceptexpr ( (KW_UNION | P) intersectexceptexpr )* ;
intersectexceptexpr : instanceofexpr ( ( KW_INTERSECT | KW_EXCEPT) instanceofexpr )* ;
// [25]
instanceofexpr : treatexpr ( KW_INSTANCE KW_OF sequencetype )? ;
treatexpr : castableexpr ( KW_TREAT KW_AS sequencetype )? ;
castableexpr : castexpr ( KW_CASTABLE KW_AS singletype )? ;
castexpr : arrowexpr ( KW_CAST KW_AS singletype )? ;
arrowexpr : unaryexpr ( EG arrowfunctionspecifier argumentlist )* ;
// [30]
unaryexpr : ( MINUS | PLUS)* valueexpr ;
valueexpr : simplemapexpr ;
generalcomp : EQ | NE | LT | LE | GT | GE ;
valuecomp : KW_EQ | KW_NE | KW_LT | KW_LE | KW_GT | KW_GE ;
// nodecomp : KW_IS | LL | GG ;
// [35]
simplemapexpr : pathexpr ( BANG pathexpr)* ;
pathexpr : SLASH relativepathexpr? | SS relativepathexpr | relativepathexpr ;
relativepathexpr : stepexpr (( SLASH | SS) stepexpr)* ;
stepexpr : postfixexpr | axisstep ;
axisstep : (reversestep | forwardstep) predicatelist ;
// [40]
forwardstep : forwardaxis nodetest | abbrevforwardstep ;
// forwardaxis : KW_CHILD COLONCOLON | KW_DESCENDANT COLONCOLON | KW_FLAG COLONCOLON | KW_SELF COLONCOLON | KW_DESCENDANT_OR_SELF COLONCOLON | KW_FOLLOWING_SIBLING COLONCOLON | KW_FOLLOWING COLONCOLON | KW_NAMESPACE COLONCOLON ;
forwardaxis : KW_CHILD COLONCOLON | KW_DESCENDANT COLONCOLON | KW_FLAG COLONCOLON | KW_SELF COLONCOLON | KW_DESCENDANT_OR_SELF COLONCOLON | KW_FOLLOWING_SIBLING COLONCOLON | KW_FOLLOWING COLONCOLON ;
abbrevforwardstep : AT? nodetest ;
reversestep : reverseaxis nodetest | abbrevreversestep ;
reverseaxis : KW_PARENT COLONCOLON | KW_ANCESTOR COLONCOLON | KW_PRECEDING_SIBLING COLONCOLON | KW_PRECEDING COLONCOLON | KW_ANCESTOR_OR_SELF COLONCOLON ;
// [45]
abbrevreversestep : DD ;
nodetest : kindtest | nametest ;
nametest : eqname | wildcard ;
wildcard : STAR | NCName CS | SC NCName | BracedURILiteral STAR ;
postfixexpr : primaryexpr (predicate | argumentlist | lookup)* ;
// [50]
argumentlist : OP (argument ( COMMA argument)*)? CP ;
predicatelist : predicate* ;
predicate : OB expr CB ;
lookup : QM keyspecifier ;
keyspecifier : NCName | IntegerLiteral | parenthesizedexpr | STAR ;
// [55]
arrowfunctionspecifier : eqname | varref | parenthesizedexpr ;
primaryexpr : literal | varref | parenthesizedexpr | contextitemexpr | functioncall | functionitemexpr | mapconstructor | arrayconstructor | unarylookup ;
literal : numericliteral | StringLiteral ;
numericliteral : IntegerLiteral | DecimalLiteral | DoubleLiteral ;
varref : DOLLAR varname ;
// [60]
varname : eqname ;
parenthesizedexpr : OP expr? CP ;
contextitemexpr : D ;
functioncall : { this.isFuncCall() }? eqname argumentlist ;
// argument : exprsingle | argumentplaceholder ;
argument : exprsingle ;
// [65]
// argumentplaceholder : QM ;
functionitemexpr : namedfunctionref | inlinefunctionexpr ;
namedfunctionref : eqname POUND IntegerLiteral /* xgc: reserved-function-names */;
inlinefunctionexpr : KW_FUNCTION OP paramlist? CP ( KW_AS sequencetype)? functionbody ;
mapconstructor : KW_MAP OC (mapconstructorentry ( COMMA mapconstructorentry)*)? CC ;
// [70]
mapconstructorentry : mapkeyexpr COLON mapvalueexpr ;
mapkeyexpr : exprsingle ;
mapvalueexpr : exprsingle ;
arrayconstructor : squarearrayconstructor | curlyarrayconstructor ;
squarearrayconstructor : OB (exprsingle ( COMMA exprsingle)*)? CB ;
// [75]
curlyarrayconstructor : KW_ARRAY enclosedexpr ;
unarylookup : QM keyspecifier ;
singletype : simpletypename QM? ;
typedeclaration : KW_AS sequencetype ;
sequencetype : KW_EMPTY_SEQUENCE OP CP | itemtype occurrenceindicator? ;
// [80]
occurrenceindicator : QM | STAR | PLUS ;
// itemtype : kindtest | KW_ITEM OP CP | functiontest | maptest | arraytest | atomicoruniontype | parenthesizeditemtype ;
itemtype : kindtest | KW_ITEM OP CP | functiontest | maptest | arraytest | atomicoruniontype | parenthesizeditemtype ;
atomicoruniontype : eqname ;
// kindtest : documenttest | fieldtest | assemblytest | flagtest | schemaelementtest | schemaattributetest | pitest | commenttest | texttest | namespacenodetest | anykindtest ;
kindtest : documenttest | fieldtest | assemblytest | flagtest | anykindtest ;
anykindtest : KW_NODE OP CP ;
// [85]
// documenttest : KW_DOCUMENT_NODE OP (fieldtest | assemblytest | schemaelementtest)? CP ;
documenttest : KW_DOCUMENT_NODE OP (fieldtest | assemblytest)? CP ;
texttest : KW_TEXT OP CP ;
// commenttest : KW_COMMENT OP CP ;
// namespacenodetest : KW_NAMESPACE_NODE OP CP ;
// pitest : KW_PROCESSING_INSTRUCTION OP (NCName | StringLiteral)? CP ;
// [90]
flagtest : KW_FLAG OP (flagnameorwildcard ( COMMA typename_)?)? CP ;
flagnameorwildcard : flagname | STAR ;
// schemaattributetest : KW_SCHEMA_ATTRIBUTE OP flagdeclaration CP ;
// flagdeclaration : flagname ;
fieldtest : KW_FIELD OP (elementnameorwildcard ( COMMA typename_ QM?)?)? CP ;
assemblytest : KW_ASSEMBLY OP (elementnameorwildcard ( COMMA typename_ QM?)?)? CP ;
// [95]
elementnameorwildcard : elementname | STAR ;
// schemaelementtest : KW_SCHEMA_ELEMENT OP elementdeclaration CP ;
elementdeclaration : elementname ;
flagname : eqname ;
elementname : eqname ;
// [100]
simpletypename : typename_ ;
typename_ : eqname ;
functiontest : anyfunctiontest | typedfunctiontest ;
anyfunctiontest : KW_FUNCTION OP STAR CP ;
typedfunctiontest : KW_FUNCTION OP (sequencetype ( COMMA sequencetype)*)? CP KW_AS sequencetype ;
// [105]
maptest : anymaptest | typedmaptest ;
anymaptest : KW_MAP OP STAR CP ;
typedmaptest : KW_MAP OP atomicoruniontype COMMA sequencetype CP ;
arraytest : anyarraytest | typedarraytest ;
anyarraytest : KW_ARRAY OP STAR CP ;
// [110]
typedarraytest : KW_ARRAY OP sequencetype CP ;
parenthesizeditemtype : OP itemtype CP ;


// Error in the spec. EQName also includes acceptable keywords.
eqname : URIQualifiedName | NCName | QName
 | KW_ANCESTOR
 | KW_ANCESTOR_OR_SELF
 | KW_AND
 | KW_ARRAY
 | KW_AS
 | KW_FLAG
 | KW_CAST
 | KW_CASTABLE
 | KW_CHILD
 | KW_COMMENT 
 | KW_DESCENDANT
 | KW_DESCENDANT_OR_SELF
 | KW_DIV
 | KW_DOCUMENT_NODE
 | KW_FIELD
 | KW_ASSEMBLY
 | KW_ELSE
 | KW_EMPTY_SEQUENCE
 | KW_EQ
 | KW_EVERY
 | KW_EXCEPT
 | KW_FOLLOWING
 | KW_FOLLOWING_SIBLING
 | KW_FOR
 | KW_FUNCTION
 | KW_GE
 | KW_GT
 | KW_IDIV
 | KW_IF
 | KW_IN
 | KW_INSTANCE
 | KW_INTERSECT
 | KW_IS
 | KW_ITEM
 | KW_LE
 | KW_LET
 | KW_LT
 | KW_MAP
 | KW_MOD
 | KW_NAMESPACE
 | KW_NAMESPACE_NODE
 | KW_NE
 | KW_NODE
 | KW_OF
 | KW_OR
 | KW_PARENT
 | KW_PRECEDING
 | KW_PRECEDING_SIBLING
 | KW_PROCESSING_INSTRUCTION
 | KW_RETURN
 | KW_SATISFIES
 | KW_SCHEMA_ATTRIBUTE
 | KW_SCHEMA_ELEMENT
 | KW_SELF
 | KW_SOME
 | KW_TEXT
 | KW_THEN
 | KW_TREAT
 | KW_UNION
 ;

// Not per spec. Specified for testing.
//auxilary : (expr SEMI )+ EOF;
