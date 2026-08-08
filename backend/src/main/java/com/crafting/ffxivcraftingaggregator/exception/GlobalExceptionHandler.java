package com.crafting.ffxivcraftingaggregator.exception;

import com.crafting.ffxivcraftingaggregator.domain.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Central translation of exceptions into the ErrorResponse shape.
 *
 * <p>Handler selection is by exception type specificity, not declaration order, so the grouping
 * below is for readability only.
 *
 * <p>KNOWN GAP: exceptions thrown inside JwtAuthFilter never reach this class. Filters run before
 * the dispatcher servlet, so an expired or malformed token still produces Spring Security's default
 * response body rather than an ErrorResponse - meaning clients see two different error shapes
 * depending on where the failure happened. Closing it needs a custom AuthenticationEntryPoint and
 * AccessDeniedHandler wired into SecurityConfig.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // 400 - the caller sent something wrong
    // ------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex){
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(),fe.getDefaultMessage()))
                .toList();

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Failed", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Bean validation on request PARAMETERS rather than a request body - the @Min/@Max on the
     * craft cost endpoint's quantity, for instance.
     *
     * <p>These throw ConstraintViolationException, not MethodArgumentNotValidException, so without
     * this handler ?quantity=0 fell through to the catch-all and returned 500 for what is plainly
     * a client error.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex){
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ErrorResponse.FieldError(
                        lastPathNode(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .toList();

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Failed", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Malformed or unparseable request body. Spring's own default here is a 400, but the catch-all
     * below is more specific than nothing and intercepts it - so a trailing comma in the JSON
     * came back as a server error until this was added.
     *
     * <p>The response deliberately does not echo {@code ex.getMessage()}: it carries parser
     * internals and field names back out to the caller.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex){
        log.warn("Malformed request body: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Malformed request body");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /** A path variable or query parameter that could not be converted, e.g. a non-UUID id. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        log.warn("Type mismatch on parameter '{}'", ex.getName());
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "Invalid value for parameter '%s'".formatted(ex.getName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex){
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "Missing required parameter '%s'".formatted(ex.getParameterName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * CAUTION: NumberFormatException extends IllegalArgumentException, as does the duplicate-key
     * failure from Map.of(). Both are server-side bugs but land here and get reported to the
     * caller as though they had sent bad input.
     *
     * <p>The cleaner fix is a dedicated InvalidRequestException thrown deliberately where "the
     * caller sent something bad" is actually meant, letting stray IllegalArgumentExceptions fall
     * through to the 500 handler where they belong. Until then the message is at least logged.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex){
        log.warn("IllegalArgumentException surfaced to the API boundary", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /** Unknown or mutually inconsistent world / data center names. */
    @ExceptionHandler({UnknownDataCenterException.class,
            UnknownWorldException.class,
            WorldDataCenterMismatchException.class}
    )
    public ResponseEntity<ErrorResponse> handleInvalidGameServer(Exception ex){
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ------------------------------------------------------------------
    // 401 / 403 - authentication and authorisation
    // ------------------------------------------------------------------

    /**
     * Failed sign-in.
     *
     * <p>Logged at WARN rather than ERROR because a wrong password is expected traffic, not
     * something to be woken up for. Both exceptions share one message, since distinguishing
     * "no such user" from "wrong password" confirms which usernames exist.
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentialException(Exception ex){
        log.warn("Failed authentication attempt");
        ErrorResponse error = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Thrown by method security (@PreAuthorize and friends). Without this, a permission failure
     * falls to the catch-all and is reported as a server error.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex){
        log.warn("Access denied: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ------------------------------------------------------------------
    // 404 - not found
    // ------------------------------------------------------------------

    /**
     * UnauthorizedSavedCraftAccessException maps to 404 rather than 403 on purpose: a 403 would
     * confirm the resource exists, letting someone enumerate other users' saved crafts by id.
     * Keep those exceptions' messages neutral or the status choice is undone by the body.
     */
    @ExceptionHandler({RecipeNotFoundException.class,
            UnauthorizedSavedCraftAccessException.class,
            SavedCraftNotFoundException.class,
            UserNotFoundException.class,
            ItemNotFoundException.class}
    )
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex){
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ------------------------------------------------------------------
    // 405 / 409 - method and state conflicts
    // ------------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex){
        ErrorResponse error = new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method %s is not supported for this endpoint".formatted(ex.getMethod()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(SyncAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleSyncRunning(SyncAlreadyRunningException ex){
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Unique constraint violations - a duplicate email or username on registration is the common
     * case. The message is fixed rather than echoed because the raw one names constraints,
     * tables and columns.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex){
        log.warn("Constraint violation", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), "Resource already exists");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ------------------------------------------------------------------
    // 502 / 503 - upstream and readiness
    // ------------------------------------------------------------------

    /**
     * Something went wrong at Universalis, not here. 502 says that; the previous grouping with the
     * catch-all reported an upstream outage as though this server were broken.
     */
    @ExceptionHandler({GameServerSyncException.class, UniversalisException.class})
    public ResponseEntity<ErrorResponse> handleUpstreamFailure(RuntimeException ex){
        log.error("Upstream Universalis failure", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_GATEWAY.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    /**
     * The world tables are empty, so nothing can be validated yet.
     *
     * <p>503, not 404: the caller's world name was probably fine - the server simply is not ready.
     * A 404 sends someone hunting for a typo in a value that was correct all along.
     */
    @ExceptionHandler(GameServerDataNotSyncedException.class)
    public ResponseEntity<ErrorResponse> handleNotSynced(GameServerDataNotSyncedException ex){
        log.error("World data unavailable", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // ------------------------------------------------------------------
    // 500 - anything unanticipated
    // ------------------------------------------------------------------

    /**
     * Anything reaching here is by definition a bug that was not anticipated, so the stack trace
     * is logged at ERROR. Without that line the only trace of a production failure would be
     * "Something went wrong" in the caller's response, with nothing left to debug from.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex){
        log.error("Unhandled exception", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Constraint violation paths arrive as "methodName.parameterName"; only the parameter is
     * useful to a caller, and the method name leaks internal naming.
     */
    private static String lastPathNode(String propertyPath){
        int lastDot = propertyPath.lastIndexOf('.');
        return (lastDot < 0) ? propertyPath : propertyPath.substring(lastDot + 1);
    }
}