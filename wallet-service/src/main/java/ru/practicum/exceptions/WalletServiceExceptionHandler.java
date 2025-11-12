package ru.practicum.exceptions;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.LowBalanceException;
import ru.practicum.NotFoundException;

@RestControllerAdvice
@Slf4j
public class WalletServiceExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(NotFoundException e) {
        log.error("Not Found Exception: ", e);
        return e.getMessage();
    }

    @ExceptionHandler(LowBalanceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String handleLowBalanceException(LowBalanceException e) {
        log.error("Low Balance Exception: ", e);
        return e.getMessage();
    }
}
