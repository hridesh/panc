#lang racket

(#%require (lib "eopl.ss" "eopl"))
(#%provide (all-defined))

;===============================================================================
;========================= Lexical and Grammar Specs ===========================
;===============================================================================

(define the-lexical-spec
  '(
    (whitespace (whitespace) skip)
    (comment ("#" (arbno (not #\newline))) skip)
    (number (digit (arbno digit)) number)
    (number ("-" digit (arbno digit)) number)
    (string ("\""(arbno (or digit letter)) "\"") string)
    
    (identifier (letter (arbno (or letter digit "_"))) symbol)
    )
  )

(define the-grammar
  '(
    (program ("system" expr ) system)
    
    (expr (number) num-expr)
    (expr ("false") false-expr)
    (expr ("true") true-expr)
    (expr (string) string-expr)
    
    (expr ("{" (arbno var-expr) (arbno expr) "}") block-expr)
    
    
    (expr("&&" expr expr) and-expr)
    (expr("||" expr expr) or-expr)
    (expr(">" expr expr) greater-expr)
    (expr("<" expr expr) less-expr)
    (expr(">=" expr expr) greater-or-equal-expr)
    (expr("<=" expr expr) less-or-equal-expr)
    (expr("==" expr expr) equal-expr)
    (expr("!=" expr expr) not-equal-expr)
    (expr("!" expr) not-expr)
    
    (expr("+" expr expr) add-expr)
    (expr("-" expr expr) minus-expr)
    (expr("*" expr expr) mult-expr)
    (expr("/" expr expr) div-expr)
    
    (expr( "if" "(" expr ")" expr "else" expr) if-expr)
    
    (expr (identifier) iden-expr)
    (expr ("set" identifier "=" expr) set-expr)
    (expr  ("create" identifier "[" expr "]") new-capsule-expr)
    (expr  ("new" type-expr "[" expr "]") new-type-expr)
    (expr ("["expr"]"identifier) array-deref)
    
    
    
    (expr ("wire" (arbno "[" expr"]") identifier  "(" (arbno expr)")") wire-expr)
    (expr ("for" "("var-expr ";" expr ";" expr")" expr) for-expr)
    (expr (identifier) iden-expr)
    
    
    (var-expr (type-expr identifier "=" expr) val)
    ;(var-expr ("final" type-expr identifier "=" expr) final-val)
    (var-expr ( "capsule"(arbno capsule-mods) identifier identifier) capsule-def-expr)
    (var-expr ("array" type-expr identifier "[]") normal-array-expr)
    (var-expr ("capsule-array" (arbno capsule-mods) identifier identifier "[]") capsule-array-expr)
    
    
    (type-expr ("int") int-type)
    (type-expr ("boolean") bool-type)
    (type-expr ("long") long-type)
    (type-expr ("float") float-type)
    
    (capsule-mods ("sequential") seq-mod)
    (capsule-mods ("thread") thread-mod)
    (capsule-mods ("task") task-mod)
    
    )
  )

;===============================================================================
;============================= sllgen boilerplate ==============================
;===============================================================================
;this will create the AST datatype with define-datatype
;according to the lexical and grammar specifications.
(sllgen:make-define-datatypes the-lexical-spec the-grammar)

;you can use this function to display the define-datatype
;expression used to generate the AST.
(define (show-the-datatypes)
  (sllgen:list-define-datatypes the-lexical-spec the-grammar))

;create-ast is a one argument function that takes a string,
;scans & parses it and generates a resulting abstract
;syntax tree.
(define create-ast
  (sllgen:make-string-parser the-lexical-spec the-grammar))

;you can use this function to find out more about how
;the string is broken up into tokens during parsing,
;this step is automatically included in the create-ast
;function. This is a one-argument function that takes a 
;string.
(define just-scan
  (sllgen:make-string-scanner the-lexical-spec the-grammar))
