package jsonjoin.labelintersection;

import org.apache.asterix.dataflow.data.nontagged.serde.AObjectSerializerDeserializer;
import org.apache.asterix.om.base.IAObject;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.api.IPointable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public final class LabelTypeTuple implements Serializable, Comparable<LabelTypeTuple> {
    final byte[] label; // null if type is object, array, or multiset
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

    /**
     * Lexicographical order of (label, type) tuples by first looking at the type and then at the label.
     *
     * @param other (label, type) that this (label, type) should be compared to
     * @return  a negative integer, zero, or a positive integer if this (label, type) is less than, equal to, or greater
     *          than another (label, type)
     */
    @Override
    public int compareTo(LabelTypeTuple other) {
        if (type == other.type) {
            if (type == 1 || type == 2) {
                return java.util.Arrays.compare(label, other.label);
            }
            return 0;
        }
        return type - other.type;
    }

    private static String typeToString(int type) {
        switch (type) {
            case 1:
                return "LITERAL";
            case 2:
                return "KEY";
            case 3:
                return "OBJECT";
            case 4:
                return "ARRAY";
            case 5:
                return "MULTISET";
            default:
                throw new IllegalArgumentException("type with ID " + type + " does not exist");
        }
    }

    // Implemented for debugging purposes.
    @Override
    public String toString() {
        AObjectSerializerDeserializer serDe = AObjectSerializerDeserializer.INSTANCE;
        IAObject deserializedLabel = null;

        if (label != null) {
            try {
                deserializedLabel = serDe.deserialize(new DataInputStream(new ByteArrayInputStream(label)));
            } catch (HyracksDataException exc) {
                throw new RuntimeException(exc);
            }
        }

        return "(" + (label == null ? "null" : deserializedLabel.toString()) + ", " + typeToString(type) + ")";
    }

}
