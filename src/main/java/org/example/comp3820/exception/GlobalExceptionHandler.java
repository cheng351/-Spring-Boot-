package org.example.comp3820.exception;

import org.example.comp3820.ai.AiUnavailableException;
import org.example.comp3820.fhir.ClinicalDataUnavailableException;
import org.example.comp3820.fhir.PatientNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Turns failures into responses the front end can act on.
 *
 * <p>The distinctions matter to the caller: a patient id that does not exist is the caller's
 * mistake and retrying will not help, whereas an unreachable FHIR server or language model is
 * ours and retrying might. Collapsing both into one 500 leaves the UI unable to tell the user
 * anything useful.
 *
 * <p>Messages returned to the browser stay generic; the detail goes to the log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PatientNotFoundException ex) {
        log.info("Patient not found: {}", ex.getMessage());
        return body(HttpStatus.NOT_FOUND, "Patient not found",
                "We could not find that patient in the connected health record.");
    }

    @ExceptionHandler(ClinicalDataUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleFhirDown(ClinicalDataUnavailableException ex) {
        log.warn("FHIR server unavailable", ex);
        return body(HttpStatus.SERVICE_UNAVAILABLE, "Health record unavailable",
                "We are having trouble reaching the health record. Please try again shortly.");
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleAiDown(AiUnavailableException ex) {
        log.warn("Language model unavailable", ex);
        return body(HttpStatus.SERVICE_UNAVAILABLE, "Assistant unavailable",
                "The assistant is not responding right now. Your health record is unaffected.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAnythingElse(Exception ex) {
        log.error("Unhandled failure", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Service interruption",
                "Something went wrong at our end. Please try again later.");
    }

    private ResponseEntity<Map<String, String>> body(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of("error", error, "message", message));
    }
}
