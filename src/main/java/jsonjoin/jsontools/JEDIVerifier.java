package jsonjoin.jsontools;

import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.asterix.runtime.evaluators.common.Node;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

/**
 * Class to prevent code duplication of the same verify() method between FJ-implementations.
 */
public class JEDIVerifier {

    private final JSONTreeConverterHelper JSON_TREE_CONVERTER_HELPER = new JSONTreeConverterHelper();
    private final JEDICalculator JEDI_CALCULATOR = new JEDICalculator();

    public boolean verify(IVisitablePointable k1, IVisitablePointable k2, double threshold) {
        List<Node> jsonTree1;
        List<Node> jsonTree2;

        JSON_TREE_CONVERTER_HELPER.reset();

        try {
            jsonTree1 = JSON_TREE_CONVERTER_HELPER.toTree(k1);
            jsonTree2 = JSON_TREE_CONVERTER_HELPER.toTree(k2);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        // As we are using the unit cost model, if |T1| + |T2| <= threshold, we can guarantee that
        // jedi(T1, T2) <= threshold without calculating jed(T1, T2) since we can delete every node in T1 and insert
        // every node in T2 for a total cost of |T1| + |T2|.
        if (jsonTree1.size() + jsonTree2.size() <= threshold)
            return true;

        JEDI_CALCULATOR.prepareJEDI(jsonTree1, jsonTree2);

        return JEDI_CALCULATOR.jedi(jsonTree1, jsonTree2) <= threshold;
    }

}
