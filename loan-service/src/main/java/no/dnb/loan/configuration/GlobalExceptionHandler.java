package no.dnb.loan.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        var status = HttpStatus.valueOf(ex.getStatusCode().value());
        var body = baseBody(status);
        body.put("message", ex.getReason());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        var status = HttpStatus.BAD_REQUEST;
        var body = baseBody(status);
        var validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        if (!validationErrors.isEmpty()) {
            body.put("message", validationErrors);
        }
        return ResponseEntity.status(status).body(body);
    }

    private static Map<String, Object> baseBody(HttpStatus status) {
        var body = new HashMap<String, Object>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        return body;
    }
}
