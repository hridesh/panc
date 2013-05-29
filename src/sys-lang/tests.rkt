#lang racket
(#%provide (all-defined))
(#%require rackunit)

(#%require "interpreter.rkt")
(#%require "environment.rkt")



;this function will run all the tests.
(define basic-test-suite
  (test-suite
   "afasf"
   
   (test-case
    ""
    (define input "system{
       int fortyTwo = 42
       int fortyone = 42
      }")
    (define expected "system{\nint fortyTwo = 42; \nint fortyone = 42; \n}")
    (check-equal? expected (run input))
    )
   
   (test-case
    ""
    (define input "system{
       capsule sequential CapsuleType name
       capsule CapsuleType name2
       
      }")
    (define expected "system{\nsequential CapsuleType name; \n CapsuleType name2; \n}")
    (check-equal? expected (run input))
    )
   
   (test-case
    "and"
    (define input (list "system{int x = && true false}"
                        "system{int x = && false true}"
                        "system{int x = && true true}"
                        "system{int x = && false false}"))
    (define expected (list "system{\nint x = false; \n}"
                           "system{\nint x = false; \n}"
                           "system{\nint x = true; \n}"
                           "system{\nint x = false; \n}"
                           ))
    (for-each (lambda (ex act) (check-equal? ex (run act))) expected input)
    )
   
   (test-case
    "or"
    (define input (list "system{int x = || true false}"
                        "system{int x = || false true}"
                        "system{int x = || true true}"
                        "system{int x = || false false}"))
    (define expected (list "system{\nint x = true; \n}"
                           "system{\nint x = true; \n}"
                           "system{\nint x = true; \n}"
                           "system{\nint x = false; \n}"
                           ))
    
    (for-each (lambda (ex act) (check-equal? ex (run act)) act) expected input)
    )
   
   (test-case
    "or"
    (define input (list "system{int x = < 1 2}"
                        "system{int x = <= 2 2}"
                        "system{int x = <= 1 2}"
                        "system{int x = <= 3 2}"
                        
                        "system{int x = > 2 1}"
                        "system{int x = >= 2 2}"
                        "system{int x = >= 2 1}"
                        "system{int x = >= 1 2}"
                        
                        "system{int x = == 2 2}"
                        "system{int x = == 2 1}"
                        
                        "system{int x = != 2 1}"
                        "system{int x = != 1 1}"
                        ))
    (define expected (list "system{\nint x = true; \n}"
                           "system{\nint x = true; \n}"
                           "system{\nint x = true; \n}"
                           "system{\nint x = false; \n}"
                           
                           "system{\nint x = true; \n}"
                           "system{\nint x = true; \n}"
                           "system{\nint x = true; \n}"
                           "system{\nint x = false; \n}"
                           
                           "system{\nint x = true; \n}"
                           "system{\nint x = false; \n}"
                           
                           "system{\nint x = true; \n}"
                           "system{\nint x = false; \n}"
                           ))
    
    (for-each (lambda (ex act) (check-equal? ex (run act)) act) expected input)
    )
   
   (test-case
    "simple wiring"
    (define input "system{
         capsule A aName
         capsule B bName
         wire aName(bName)
     }")
    
    (define expected "system{\n A aName; \n B bName; \naName(bName);\n}")
    (check-equal? expected (run input))
    )
   
   (test-case
    "create capsule array"
    (define input
      "system{
         capsule-array A aName[]
         int i = 42
         set aName = create A[42]
       }")
    
    (define expected "system{\nint i = 42; \nA aName[42];\n}")
    (check-equal? expected (run input))
    )
   
   (test-case
    "for loop"
    (define input
       "system{
          capsule-array A aName[]
          capsule B b
          set aName = create A[10]
          for(int i = 0; < i 10 ; set i = + i 1){
             wire [i]aName(b)
          }
       }")
    
    (define expected "system{\n B b; \nA aName[10];\naName[0](b);\naName[1](b);\naName[2](b);\naName[3](b);\naName[4](b);\naName[5](b);\naName[6](b);\naName[7](b);\naName[8](b);\naName[9](b);\n;\n}")
    (check-equal? expected (run input))
    )
   
   
   
   )
  )

;===============================================================================
;============================test infrastructure================================
;===============================================================================
(require rackunit/text-ui)

(define (test suite)
  (run-tests suite 'verbose)  
  )

(define-syntax 342-check-exn
  (syntax-rules ()
    [ (342-check-exn expression exn-msg)
      (check-equal? 
       (with-handlers ([string? (lambda (err-msg) err-msg)]) 
         expression)
       exn-msg)
      ]
    )
  )

;;=============
(define temp 
  "system{
  capsule-array A aName[]
  capsule B b
  set aName = create A[10]
  for(int i = 0; < i 10 ; set i = + i 1){
     wire [i]aName(b)
  }

}"
  )

(define temp2 "system{
  int i = 42
  capsule-array A aName[]
  set aName = create A[42]
  set i = 23
  wire[i]aName(42)
}")
(test basic-test-suite)