// src/validation/ValidationResult.java
package validation;

import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private final boolean isValid;
    private final List<String> errors;

    public ValidationResult(boolean isValid, List<String> errors) {
        this.isValid = isValid;
        this.errors = Collections.unmodifiableList(errors);
    }

    public boolean isValid() { return isValid; }
    public List<String> getErrors() { return errors; }

    public void throwIfInvalid() {
        if (!isValid) {
            throw new ValidationException(String.join(", ", errors));
        }
    }
}