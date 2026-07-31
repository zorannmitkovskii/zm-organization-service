package zm.organization.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates a tax id against a configurable pattern.
 *
 * <p>The pattern lives in configuration rather than in this annotation because
 * the format is country-specific: the Macedonian ЕДБ is 13 digits, but the
 * registry is expected to hold companies from other jurisdictions later.
 * See {@code organization.tax-id.pattern}.
 *
 * <p>A null tax id is valid — organizations may register before they have one.
 * Uniqueness is enforced by the database, not here.
 */
@Documented
@Constraint(validatedBy = TaxIdValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface ValidTaxId {

    String message() default "must match the configured tax id format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
