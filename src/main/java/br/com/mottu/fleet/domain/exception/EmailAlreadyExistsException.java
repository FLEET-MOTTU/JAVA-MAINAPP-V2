package br.com.mottu.fleet.domain.exception;

// Esta é uma exceção "unchecked", o que significa que não somos forçados
// a usar try-catch toda vez que chamamos um método que pode lançá-la.
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}