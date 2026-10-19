package com.comprasco.bakeprofit.exception.handler;

import com.comprasco.bakeprofit.exception.CategoryAlreadyExistsException;
import com.comprasco.bakeprofit.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.exception.EmailAlreadyExistsException;
import com.comprasco.bakeprofit.exception.InactiveParentException;
import com.comprasco.bakeprofit.exception.InactiveUserException;
import com.comprasco.bakeprofit.exception.InvalidCategoryHierarchyException;
import com.comprasco.bakeprofit.exception.InvalidCredentialsException;
import com.comprasco.bakeprofit.exception.ProductNotFoundException;
import com.comprasco.bakeprofit.exception.StoreAlreadyExistsException;
import com.comprasco.bakeprofit.exception.StoreNotFoundException;
import com.comprasco.bakeprofit.exception.UserNotFoundException;
import com.comprasco.bakeprofit.exception.response.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones para la API REST.
 *
 * <p>
 * Captura las excepciones lanzadas en la capa de controladores o servicios
 * y las transforma en respuestas JSON estandarizadas de tipo
 * {@link ErrorResponse}.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * Maneja errores de validacion en los atributos de los DTOs.
     *
     * @param ex      excepcion de validacion de argumentos
     * @param request peticion HTTP actual
     * @return respuesta HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String mensajes = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(mensajes)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja errores de jerarquia de categorias no valida.
     *
     * @param ex      excepcion de jerarquia invalida
     * @param request peticion HTTP actual
     * @return respuesta HTTP 400 Bad Request
     */
    @ExceptionHandler(InvalidCategoryHierarchyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCategoryHierarchy(
            InvalidCategoryHierarchyException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    /**
     * Maneja credenciales de acceso invalidas durante la autenticacion.
     *
     * @param ex      excepcion de credenciales invalidas
     * @param request peticion HTTP actual
     * @return respuesta HTTP 401 Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Maneja intentos de acceso o autenticacion de cuentas desactivadas.
     *
     * @param ex      excepcion de usuario inactivo
     * @param request peticion HTTP actual
     * @return respuesta HTTP 401 Unauthorized
     */
    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ErrorResponse> handleInactiveUser(
            InactiveUserException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }


    /**
     * Maneja situaciones donde un recurso solicitado no existe en la base de datos.
     *
     * @param ex      excepcion de entidad no encontrada
     * @param request peticion HTTP actual
     * @return respuesta HTTP 404 Not Found
     */
    @ExceptionHandler({
            UserNotFoundException.class,
            CategoryNotFoundException.class,
            StoreNotFoundException.class,
            ProductNotFoundException.class,
            EntityNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }


    /**
     * Maneja el intento de registrar un correo electronico duplicado.
     *
     * @param ex      excepcion de correo duplicado
     * @param request peticion HTTP actual
     * @return respuesta HTTP 409 Conflict
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Maneja el intento de registrar una categoria duplicada.
     *
     * @param ex      excepcion de categoria duplicada
     * @param request peticion HTTP actual
     * @return respuesta HTTP 409 Conflict
     */
    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCategoryAlreadyExists(
            CategoryAlreadyExistsException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Maneja el intento de registrar una tienda duplicada.
     *
     * @param ex      excepcion de tienda duplicada
     * @param request peticion HTTP actual
     * @return respuesta HTTP 409 Conflict
     */
    @ExceptionHandler(StoreAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleStoreAlreadyExists(
            StoreAlreadyExistsException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Maneja operaciones sobre entidades asociadas a una categoria padre inactiva.
     *
     * @param ex      excepcion de padre inactivo
     * @param request peticion HTTP actual
     * @return respuesta HTTP 409 Conflict
     */
    @ExceptionHandler(InactiveParentException.class)
    public ResponseEntity<ErrorResponse> handleInactiveParent(
            InactiveParentException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }


    /**
     * Maneja cualquier otra excepcion no controlada expliticamente.
     *
     * @param ex      excepcion general
     * @param request peticion HTTP actual
     * @return respuesta HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Error interno del servidor")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}