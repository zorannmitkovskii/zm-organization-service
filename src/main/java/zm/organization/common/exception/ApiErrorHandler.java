package zm.organization.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One place where domain failures become HTTP responses, so controllers stay
 * free of status codes.
 *
 * <p>Bodies are shaped for machine consumption — the callers are backend
 * services deciding what to show their own users, so {@code error} is a stable
 * code and {@code message} is for logs, not for display.
 */
@Slf4j
@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_FAILED",
                "fieldErrors", fieldErrors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "NOT_FOUND",
                "message", e.getMessage()));
    }

    @ExceptionHandler(InviteNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleInviteNotFound(InviteNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "INVITE_NOT_FOUND",
                "message", e.getMessage()));
    }

    @ExceptionHandler(InviteNotUsableException.class)
    ResponseEntity<Map<String, Object>> handleSpentInvite(InviteNotUsableException e) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "error", "INVITE_NOT_USABLE",
                "message", e.getMessage()));
    }

    @ExceptionHandler(LastOwnerException.class)
    ResponseEntity<Map<String, Object>> handleLastOwner(LastOwnerException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "LAST_OWNER",
                "message", e.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "error", "RATE_LIMITED",
                "message", e.getMessage()));
    }

    @ExceptionHandler(TaxIdAlreadyExistsException.class)
    ResponseEntity<Map<String, Object>> handleDuplicateTaxId(TaxIdAlreadyExistsException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "TAX_ID_EXISTS");
        body.put("message", e.getMessage());
        UUID existing = e.getExistingOrgId();
        if (existing != null) {
            body.put("existingOrgId", existing.toString());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
