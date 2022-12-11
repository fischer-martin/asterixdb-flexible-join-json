package jsonjoin.jsontools;

import jsonjoin.FlexibleJOFilterJSONJoin;
import jsonjoin.FlexibleJSONJoin;
import org.apache.asterix.external.cartilage.base.Configuration;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.asterix.runtime.evaluators.common.JOFilterTree;
import org.apache.asterix.runtime.evaluators.common.Node;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

/**
 * Class to prevent code duplication of the same verify() method between FJ-implementations.
 */
public class JEDIVerifier {

    private final JSONTreeConverterHelper JSON_TREE_CONVERTER_HELPER = new JSONTreeConverterHelper();
    private final JEDICalculator JEDI_CALCULATOR = new JEDICalculator();
    private final JOFilterCalculator JOFILTER_CALCULATOR = new JOFilterCalculator();
    JOFilterTree t1 = new JOFilterTree();
    JOFilterTree t2 = new JOFilterTree();

    public boolean verifyParsed(List<? extends Node> jsonTree1, List<? extends Node> jsonTree2, double threshold) {
        // As we are using the unit cost model, if |T1| + |T2| <= threshold, we can guarantee that
        // jedi(T1, T2) <= threshold without calculating jed(T1, T2) since we can delete every node in T1 and insert
        // every node in T2 for a total cost of |T1| + |T2|.
        if (jsonTree1.size() + jsonTree2.size() <= threshold) {
            return true;
        }

        JEDI_CALCULATOR.prepareJEDI(jsonTree1, jsonTree2);

        return JEDI_CALCULATOR.jedi(jsonTree1, jsonTree2) <= threshold;
    }

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

        return verifyParsed(jsonTree1, jsonTree2, threshold);
    }

    public boolean duplicateAvoidingVerify(FlexibleJSONJoin join, int b1, IVisitablePointable k1, int b2
            , IVisitablePointable k2, Configuration c) {
        List<Node> jsonTree1;
        List<Node> jsonTree2;

        JSON_TREE_CONVERTER_HELPER.reset();

        try {
            jsonTree1 = JSON_TREE_CONVERTER_HELPER.toTree(k1);
            jsonTree2 = JSON_TREE_CONVERTER_HELPER.toTree(k2);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        // Slightly rewritten default implementation of the duplicate avoidance where we verify a pair only if it is
        // absolutely necessary but at the cost of always calculating two bucket assignments instead.
        int[] buckets1DA = join.assign1Parsed(jsonTree1, c);
        int[] buckets2DA = join.assign2Parsed(jsonTree2, c);
        int i = 0;
        int j = 0;

        while (i < buckets1DA.length && j < buckets2DA.length) {
            if (buckets1DA[i] == buckets2DA[j]) {
                return buckets1DA[i] == b1 && buckets2DA[j] == b2 && join.verifyParsed(jsonTree1, jsonTree2);
            } else {
                if (buckets1DA[i] > buckets2DA[j]) {
                    j++;
                } else {
                    i++;
                }
            }
        }

        return false;
    }

    public boolean joFilterAndVerifyParsed(JOFilterTree k1, JOFilterTree k2, double threshold) {
        if (JOFILTER_CALCULATOR.joFilter(k1, k2, threshold)) {
            return true;
        } else {
            return verifyParsed(k1.getPostorderedTree(), k2.getPostorderedTree(), threshold);
        }
    }

    public boolean joFilterAndVerify(IVisitablePointable k1, IVisitablePointable k2, double threshold) {
        JSON_TREE_CONVERTER_HELPER.reset();

        try {
            t1 = JSON_TREE_CONVERTER_HELPER.toJOFilterTree(k1, t1);
            t2 = JSON_TREE_CONVERTER_HELPER.toJOFilterTree(k2, t2);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        return joFilterAndVerifyParsed(t1, t2, threshold);
    }

    public boolean duplicateAvoidingJOFilterAndVerify(FlexibleJOFilterJSONJoin join, int b1, IVisitablePointable k1
            , int b2, IVisitablePointable k2, Configuration c) {
        JSON_TREE_CONVERTER_HELPER.reset();

        try {
            t1 = JSON_TREE_CONVERTER_HELPER.toJOFilterTree(k1, t1);
            t2 = JSON_TREE_CONVERTER_HELPER.toJOFilterTree(k2, t2);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        // Slightly rewritten default implementation of the duplicate avoidance where we verify a pair only if it is
        // absolutely necessary but at the cost of always calculating two bucket assignments instead.
        int[] buckets1DA = join.assign1Parsed(t1.getPostorderedTree(), c);
        int[] buckets2DA = join.assign2Parsed(t2.getPostorderedTree(), c);
        int i = 0;
        int j = 0;

        while (i < buckets1DA.length && j < buckets2DA.length) {
            if (buckets1DA[i] == buckets2DA[j]) {
                return buckets1DA[i] == b1 && buckets2DA[j] == b2 && join.joFilterAndVerifyParsed(t1, t2);
            } else {
                if (buckets1DA[i] > buckets2DA[j]) {
                    j++;
                } else {
                    i++;
                }
            }
        }

        return false;
    }

}
