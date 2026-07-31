package zm.organization.common;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the names of the fields a PATCH request actually carried.
 *
 * <p>The audit trail records which fields changed, never what they changed to
 * — an audit table full of tax ids and phone numbers is a second copy of the
 * personal data, with none of the protections the first copy has.
 *
 * <p>Reflection over the record components is what makes this work for any
 * update DTO without a per-DTO list that would silently rot as fields are
 * added.
 */
public final class ChangedFields {

    private ChangedFields() {
    }

    public static List<String> of(Record updateRequest) {
        List<String> present = new ArrayList<>();
        for (RecordComponent component : updateRequest.getClass().getRecordComponents()) {
            try {
                if (component.getAccessor().invoke(updateRequest) != null) {
                    present.add(component.getName());
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Could not read " + component.getName() + " for the audit trail", e);
            }
        }
        return present;
    }
}
