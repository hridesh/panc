#lang racket
(#%require (lib "eopl.ss" "eopl"))
(#%require "lang.rkt")
(#%require "lib.rkt")

(#%provide (all-defined))

;===============================================================================
;============================= Expressed Values ================================
;===============================================================================
(define (invalid-args-exception fun-name expected-val actual-val)
  (raise (to-string fun-name ", expected: " expected-val ", got: " actual-val) ))

(define-datatype exp-val exp-val?
  (bool-val (v boolean?))
  (num-val (v number?))
  (string-val (v string?))
  
  (array-val (v reference?) (length number?))
  (capsule-array-val  (size number?) (capsule-type symbol?))
  )

;================================ expressed-val ===================================
(define (val->bool exp-v)
  (or (exp-val? exp-v) (type-mismatch-exn "expressed-value" exp-v))
  (cases exp-val exp-v
    (bool-val (v) v)
    (else (raise (type-mismatch-exn "bool" exp-v)))
    )
  )

(define (val->num exp-v)
  (or (exp-val? exp-v) (type-mismatch-exn "expressed-value" exp-v))
  (cases exp-val exp-v
    (num-val (v) v)
    (else (raise (type-mismatch-exn "num" exp-v)))
    )
  )

(define (val->string exp-v)
  (or (exp-val? exp-v) (type-mismatch-exn "expressed-value" exp-v))
  (cases exp-val exp-v
    (num-val (v) v)
    (else (raise (type-mismatch-exn "string" exp-v)))
    )
  )


(define (val-capsule-array-type? exp-v)
  (and (exp-val? exp-v)
       (cases exp-val exp-v
         (capsule-array-val (v t) #t)
         (else #f)
         )
       )
  )

(define (val->capsule-array-type exp-v)
  (or (exp-val? exp-v) (type-mismatch-exn "expressed-value" exp-v))
  (cases exp-val exp-v
    (capsule-array-val (v t) t)
    (else (raise (type-mismatch-exn "num" exp-v)))
    )
  )

(define (val->capsule-array-size exp-v)
  (or (exp-val? exp-v) (type-mismatch-exn "expressed-value" exp-v))
  (cases exp-val exp-v
    (capsule-array-val (v t) v)
    (else (raise (type-mismatch-exn "num" exp-v)))
    )
  )

(define (type-mismatch-exn fun-name expected-val actual-val)
  (raise (to-string fun-name ", expected: " expected-val ", got: " actual-val) ))
;===============================================================================
;=============================== Environment ===================================
;===============================================================================

(define-datatype environment environment?
  (empty-env)
  (extend-env
   (bvar symbol?)
   (bval reference?)
   (saved-env environment?)))

(define (apply-env env search-sym)
  (cases environment env
    (empty-env ()
               (no-binding-exception search-sym))
    
    (extend-env (var val saved-env)
                (if (eqv? search-sym var)
                    val
                    (apply-env saved-env search-sym))
                )
    
    )
  )

(define (sym-final-exception sym)
  (raise (to-string "variable '" sym " is final and cannot be overridden."))
  )

(define (no-binding-exception sym)
  (raise (to-string "No binding for '" sym))
  )

;===============================================================================
;================================ the store ====================================
;===============================================================================

(define CAPSULE 'capsule)
(define NULL -1)
;;; world's dumbest model of the store:  the store is a list and a
;;; reference is number which denotes a position in the list.

;; the-store: a Scheme variable containing the current state of the
;; store.  Initially set to a dummy value.
(define the-store 'uninitialized)

;; empty-store : () -> Store
;; Page: 111
(define empty-store
  (lambda () '()))

;; initialize-store! : () -> Store
;; usage: (initialize-store!) sets the-store to the empty-store
;; Page 111
(define initialize-store!
  (lambda ()
    (set! the-store (empty-store))))

;; reference? : SchemeVal -> Bool
;; Page: 111
(define (reference? v)
  (integer? v))

;; newref : ExpVal -> Ref -- malloc in C/C++
;; Page: 111 
(define (newref val)
  (let ((next-ref (length the-store)))
    (set! the-store
          (append the-store (list val)))
    next-ref))

;; deref : Ref -> ExpVal -- value->string at certain reference
;; Page 111
(define (deref ref)
  (or (list? the-store) (raise "unitialized store"))
  (if (>= ref (length the-store))
      (report-invalid-reference ref)
      (list-ref the-store ref)
      )
  )

;; setref! : Ref * ExpVal -> Unspecified -- backend of assignment
;; Page: 112
(define (setref! ref val)
  (or (list? the-store) (raise "unitialized store"))
  (if (>= ref (length the-store))
      (report-invalid-reference ref)
      (set! the-store
            ;we map the old store to a new one where only the element on
            ;position 'ref' is changed. The exact same thing we did for the
            ;change-at-index problem in homework 2.
            (map (lambda (store-entry index)
                   (if (= index ref)
                       val
                       store-entry
                       )
                   )
                 the-store
                 (range (length the-store))))
      )
  )

(define (report-invalid-reference ref)
  (raise (to-string "illegal reference: " ref "; in store: " the-store)))