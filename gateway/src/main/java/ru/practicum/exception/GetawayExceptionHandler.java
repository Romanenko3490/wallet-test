package ru.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.LowBalanceException;
import ru.practicum.NotFoundException;
import ru.practicum.model.Violation;

@RestControllerAdvice
@Slf4j
public class GetawayExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Violation handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String field = ex.getBindingResult().getFieldError().getField();
        String message = ex.getBindingResult().getFieldError().getDefaultMessage();
        Object rejectedValue = ex.getBindingResult().getFieldError().getRejectedValue();

        String errorMessage = String.format("Field: %s. Error %s. Value",
                field, message, rejectedValue == null ? "null" : rejectedValue.toString());
        log.error("Validation error {}", errorMessage);
        return new Violation(
                "Incorrect made request",
                errorMessage
        );
    }


    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Violation handleConstraintViolationException(ConstraintViolationException ex) {
        var violation = ex.getConstraintViolations().iterator().next();

        String field = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        Object rejectedValue = violation.getInvalidValue();

        String errorMessage = String.format("Field: %s. Error %s. Value",
                field, message, rejectedValue == null ? "null" : rejectedValue.toString());

        return new Violation(
                "Incorrect made request",
                errorMessage
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Violation handleNotFoundException(NotFoundException ex) {
        log.error("Not Found Exception: ", ex);
        return new Violation(
                "Not Found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(LowBalanceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Violation handleLowBalanceException(LowBalanceException ex) {
        log.error("Low Balance Exception: ", ex);
        return new Violation(
                "Low Balance Exception",
                ex.getMessage()
        );
    }

}
