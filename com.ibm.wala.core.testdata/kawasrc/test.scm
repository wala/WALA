
(define (doit op args)
  (apply op args))

(define plusish
  (lambda args
    (if (= (length args) 1)
	(car args)
	(+ (car args) (plusish (cdr args))))))

(write (doit plusish (list 3 4 5)))
