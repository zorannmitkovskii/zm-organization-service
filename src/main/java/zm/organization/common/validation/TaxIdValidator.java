package zm.organization.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.regex.Pattern;

/**
 * Applies the configured tax id pattern. Spring injects the value because
 * Hibernate Validator resolves constraint validators through the application
 * context when Spring Boot is on the classpath.
 */
public class TaxIdValidator implements ConstraintValidator<ValidTaxId, String> {

    private final Pattern pattern;

    public TaxIdValidator(@Value("${organization.tax-id.pattern:^[0-9]{13}$}") String configuredPattern) {
        this.pattern = Pattern.compile(configuredPattern);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return pattern.matcher(value).matches();
    }
}
