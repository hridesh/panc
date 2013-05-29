#lang racket
(#%provide to-string flat-list)

(define (to-string s . los)
  (letrec 
      ([lst (cons s los)]
       [lst-of-strings (map (lambda (s) (format "~a" s)) lst)])
    (foldr string-append "" lst-of-strings)
    )
  )

(define (flat-list el1 . rest)
  (flatten (list el1 rest))
  )
