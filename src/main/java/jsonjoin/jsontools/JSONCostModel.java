package jsonjoin.jsontools;

import org.apache.asterix.om.pointables.AFlatValuePointable;
import org.apache.asterix.runtime.evaluators.common.Node;

class JSONCostModel {
    double ins;
    double del;
    double ren;

    public JSONCostModel(double ins, double del, double ren) {
        this.ins = ins;
        this.del = del;
        this.ren = ren;
    }

    public double ins(Node n) {
        return ins;
    }

    public double del(Node n) {
        return del;
    }

    public double ren(Node m, Node n) {
        if (m.getType() == n.getType()) {
            if (m.getLabel().getClass().getName().compareTo(n.getLabel().getClass().getName()) == 0) {
                if (m.getLabel() instanceof AFlatValuePointable) {
                    if (m.getLabel().equals(n.getLabel())) {
                        return 0; // 0 cost if the node type, data type, and values are identical.
                    }
                    return ren; // Rename cost if values are different.
                } else {
                    return 0; // Objects, arrays, and multisets always match.
                }
            } else { // Comment else to disable renames of different literal types.
                return ren; // Rename cost if data types are different.
            }
        }
        return Double.POSITIVE_INFINITY; // Different node types.
    }
}
