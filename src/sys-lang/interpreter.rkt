#lang racket
(#%provide run)
(#%require (lib "eopl.ss" "eopl"))

(#%require "environment.rkt")
(#%require "lang.rkt")
(#%require "lib.rkt")

;===============================================================================
;================================ Value-of =====================================
;===============================================================================
;value-of takes as a parameter an AST resulted from a call to the
;create-ast function.
(define (run program-string . arguments)
  (if (string? program-string)
      (begin 
        (initialize-store!)
        (value-of (create-ast program-string) (empty-env)))
      (raise (string-append "expected a program as string, got: " (~a program-string)))
      )  
  )

(define (value-of ast env)
  (cond 
    [(program? ast) (value-of-program ast env)]
    [(expr? ast) (value-of-expr ast env)]
    [(var-expr? ast) (value-of-var ast env)]
    [else (raise (~a "Unimplemented ast node: " ~a ast))]
    )
  )
;================================= program =====================================
(define (value-of-program prog env)
  (cases program prog
    (system (exp) (to-string "system" (value-of exp env)))
    (else (raise-unknown-ast-node-exn exp))
    )
  )

;=================================== expr =======================================
(define (value-of-expr ex env)
  (or (expr? ex) (raise (string-append "value-of-expr error: expected an expression, got " (to-string ex))))
  (cases expr ex
    
    ;(expr (number) num-expr)
    (num-expr (n) (num-val n))
    
    ;(expr ("#f") false-expr)
    (false-expr () (bool-val #f))
    
    ;(expr ("#t") true-expr)
    (true-expr () (bool-val #t))
    
    ;(expr (string) string-expr)
    (string-expr (str) (string-val str))
    ;(expr ("{" (arbno var-expr) (arbno expr) "}") block-expr)
    (block-expr
     (list-of-var-decl list-of-expr)
     (letrec ([var-decl-result (foldl (lambda
                                          (e acc) 
                                        (letrec ([computed-tuple (value-of e (first acc))]
                                                 [new-env (first computed-tuple)]
                                                 [new-string (second computed-tuple)]
                                                 [prev-los (second acc)])
                                          (list  new-env (if (equal? new-string "") prev-los (cons new-string prev-los)))
                                          )
                                        )
                                      
                                      (list env '())
                                      list-of-var-decl
                                      )]
              [new-env (first var-decl-result)]
              [var-decl-string (foldl (lambda (x y) (to-string x "; \n" y)) "" (second var-decl-result))])
       
       (to-string
        "{\n"
        var-decl-string
        (foldl (lambda (ex resulting-program) 
                 (let ([val (value-of ex new-env)])
                   (to-string resulting-program (if (string? val) (to-string val ";\n") ""))
                   ))
               ""
               list-of-expr
               ) 
        "}")
       )
     )
    
    ;    (expr("&&" expr expr) and-expr)
    (and-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->bool (value-of e1 env))]
              [val-e2 (val->bool (value-of e2 env))])
       (bool-val (and val-e1 val-e2))
       
       )
     )
    
    ;    (expr("||" expr expr) or-expr)
    (or-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->bool (value-of e1 env))]
              [val-e2 (val->bool (value-of e2 env))])
       (bool-val (or val-e1 val-e2))
       
       )
     )
    
    ;    (expr("!" expr) not-expr)
    (not-expr 
     (e) 
     (letrec ([val-e (val->bool (value-of e env))])
       (bool-val (not val-e))
       )
     )
    
    ;    (expr(">" expr expr) greater-expr)
    (greater-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (bool-val (> val-e1 val-e2))
       
       )
     )
    
    ;    (expr("<" expr expr) less-expr)
    (less-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (bool-val (< val-e1 val-e2))
       
       )
     )
    
    ;    (expr(">=" expr expr) greater-or-equal-expr)
    (greater-or-equal-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (bool-val (>= val-e1 val-e2))
       
       )
     )
    
    ;    (expr("<=" expr expr) less-or-equal-expr)
    (less-or-equal-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (bool-val (<= val-e1 val-e2))
       
       )
     )
    
    ;    (expr("==" expr expr) equal-expr)
    (equal-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (bool-val (eq? val-e1 val-e2))
       
       )
     )
    
    ;    (expr("!=" expr expr) not-equal-expr)
    (not-equal-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (bool-val (not (eq? val-e1 val-e2)))
       
       )
     )
    
    ;(expr("+" expr expr) add-expr)
    (add-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (num-val (+ val-e1 val-e2))
       )
     )
    
    ;(expr("-" expr expr) minus-expr)
    (minus-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (num-val (- val-e1 val-e2))
       )
     )
    ;(expr("*" expr expr) mult-expr)
    (mult-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (num-val (* val-e1 val-e2))
       )
     )
    ;(expr("/" expr expr) div-expr)
    (div-expr 
     (e1 e2) 
     (letrec ([val-e1 (val->num (value-of e1 env))]
              [val-e2 (val->num (value-of e2 env))])
       (num-val (/ val-e1 val-e2))
       )
     )
    
    ;    (expr (identifier) iden-expr)
    (iden-expr
     (iden)
     (letrec ([val (deref (apply-env env iden))])
       (if (equal? val CAPSULE)
           iden
           val
           )
       )
     )
    
    ;    (expr ("set" identifier "=" expr) set-expr)
    (set-expr
     (iden expr)
     (letrec ([val (value-of expr env)]
              [ref (apply-env env iden)]
              )
       (if (val-capsule-array-type? val)
           (to-string (val->capsule-array-type val) " " iden "["(val->capsule-array-size val) "]")
           (begin (setref! ref val) "")
           )
       )
     )
    
    ;    (expr  ("create" identifier "[" expr "]") new-capsule-expr)
    (new-capsule-expr
     (iden size-expr)
     (capsule-array-val (val->num (value-of size-expr env)) iden)
     )
    
    ;    (expr  ("new" type-expr "[" expr "]") new-type-expr)
    
    ;    (expr ("["expr"]"identifier) array-deref)
    ;    
    ;    (expr ("wire" (arbno "[" expr"]") identifier "(" (arbno expr)")") wire-expr)
    (wire-expr
     (optional-index iden exprs)
     (apply-env env iden)
     (letrec ([index-str (if (null? optional-index) "" (to-string "[" (val->num (value-of (car optional-index) env))"]"))]
              [wiring-str (to-string "(" (foldl (lambda (e acc) (to-string acc (if (= 1 (length exprs)) "" ",") (value-of e env))) "" exprs) ")")])
       (to-string iden index-str wiring-str)
       )
     )
        
    
    ;    (expr ("for" "("var-expr ";" expr ";" expr")" expr) for-expr)
    (for-expr
     (var-ex cond-ex control-ex body-ex)
     (letrec ([var-expr-tuple (value-of var-ex env)]
              [new-env (car var-expr-tuple)]
              [var-decl-string (cadr var-expr-tuple)])
       
       (to-string (unroll-for-loop cond-ex control-ex body-ex new-env))
     )
     )
    
    
    (else (raise-unknown-ast-node-exn ex))
    )
  
  )

(define (unroll-for-loop cond-exp control-exp body-exp env)
  (letrec ([cond-val (val->bool (value-of cond-exp env))]
           [val-of-body (if cond-val
                            (string-normalize-spaces (string-replace (string-replace (value-of body-exp env) "{" "") "}" ""))
                            "")]
           [control-val (value-of control-exp env)])
    (if cond-val 
        (to-string val-of-body "\n" (unroll-for-loop cond-exp control-exp body-exp env) )
        val-of-body
        )
    )
  )









;=================================== var =======================================
(define (value-of-var v-ex env)
  
  (cases var-expr v-ex
    
    ;   (var-expr (type-expr identifier "=" expr) val)
    (val (type iden val-expr) 
         (letrec ([v (value-of-expr val-expr env)]
                  [new-env (extend-env iden (newref v) env)]
                  [str (to-string (type->string type) " " iden " = " (exp-val->java-denoted v))])
           (list new-env str)
           )
         )
    
    ;    (var-expr ( "capsule"(arbno capsule-mods) identifier identifier) capsule-def-expr)
    (capsule-def-expr 
     (mod capsule-type var-name)
     (letrec ([new-env (extend-env var-name (newref CAPSULE) env)]
              [str (to-string (capsule-mod->string mod) " " capsule-type " " var-name)])
       (list new-env str)
       )
     )
    ;    (var-expr ("array" type-expr identifier"[]") normal-array-expr)
    (normal-array-expr 
     (type name)
     (letrec ([new-env (extend-env name NULL env)]
              [str (to-string " " (type->string type) " " name "[]")])
       (list new-env str)
       )
     )
    
    ;    (var-expr ("capsule array" (arbno capsule-mods) identifier identifier"[" expr"]") capsule-array-expr)
    (capsule-array-expr 
     (mod capsule-type var-name)
     (letrec ([new-env (extend-env var-name NULL env)])
       (list new-env "")
       )
     )
    
    
    (else (raise-unknown-ast-node-exn v-ex))
    )
  
  
  
  )

(define (type->string t-expr)
  ;    (type-expr ("int") int-type)
  ;    (type-expr ("boolean") bool-type)
  ;    (type-expr ("long") long-type)
  ;    (type-expr ("float") float-type)
  
  (cases type-expr t-expr
    (int-type () "int")
    (bool-type () "boolean")
    (long-type () "long")
    (float-type () "float")
    (else "unknown")
    )
  )

(define (capsule-mod->string mod)
  (if (null? mod)
      ""
      (cases capsule-mods (first mod)
        ;    (capsule-mods ("sequential") seq-mod)
        (seq-mod () "sequential")
        ;    (capsule-mods ("thread") thread-mod)
        (thread-mod() "thread")
        ;    (capsule-mods ("task") task-mod)
        (task-mod () "task")
        
        )
      )
  )


(define (exp-val->java-denoted v)
  (cases exp-val v
    (bool-val (v) (if v "true" "false"))
    (num-val (v) (to-string v))
    (string-val (str) str)
    
    (else (raise "bla"))
    )
  )

(define (raise-unknown-ast-node-exn ast-node)
  (raise (to-string "unknown ast node: " ast-node))
  )