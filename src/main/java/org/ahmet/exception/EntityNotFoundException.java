package org.ahmet.exception;

/**
 * Exception thrown when a requested entity is not found.
 */
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String entityType, Object id) {
        super(String.format("%s with id '%s' not found", entityType, id));
    }
}