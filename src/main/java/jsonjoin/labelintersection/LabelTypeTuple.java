package jsonjoin.labelintersection;

import org.apache.hyracks.data.std.api.IPointable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public final class LabelTypeTuple implements Serializable {
    final byte[] label;
    final int type; // 1: literal, 2: key, 3: object, 4: array, 5: multiset

    public LabelTypeTuple(IPointable label, int type) {
        // In theory, nodes in JSON trees only have a label if they are literal nodes or key nodes.
        if (type == 1 || type == 2) {
            // IPointable is not serializable.
            // The data type is preserved since its ID (see ATypeTag) is stored in the first byte of the byte array.
            this.label = Arrays.copyOfRange(label.getByteArray(), label.getStartOffset()
                    , label.getStartOffset() + label.getLength());
        } else {
            // In theory, the label is null for object, array, and multiset nodes
            // (even though JSONTreeVisitor does not actually set it to null, but we need it to be null here).
            this.label = null;
        }
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LabelTypeTuple that = (LabelTypeTuple) o;

        if (type == 1 || type == 2) {
            return type == that.type && java.util.Arrays.equals(label, that.label);
        } else {
            return type == that.type;
        }
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type);

        result = 31 * result + Arrays.hashCode(label);

        return result;
    }
}
