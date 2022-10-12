package jsonjoin.lengthfilter;

import jsonjoin.FlexibleJSONJoin;
import jsonjoin.jsontools.JEDIVerifier;
import jsonjoin.jsontools.JSONTreeConverterHelper;
import org.apache.asterix.external.cartilage.base.Summary;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.asterix.runtime.evaluators.common.Node;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

// For some reason we can only use Object instead of IVisitablePointable or else the DB will throw
// HYR0066: Data pipeline protocol violation: fail() is not called by the upstream when there is a failure in the downstream [HyracksDataException]
// Maybe (sadly I am not a wizard who knows everything about Java) this happens due to IVisitablePointable being an interface.
public class JsonJoin implements FlexibleJSONJoin<Object, JsonJoinConfiguration> {

    private final double THRESHOLD;
    private final JSONTreeConverterHelper JSON_TREE_CONVERTER_HELPER = new JSONTreeConverterHelper();
    private final JEDIVerifier JEDI_VERIFIER = new JEDIVerifier();

    public JsonJoin(double threshold) {
        this.THRESHOLD = threshold;
    }

    @Override
    public Summary<Object> createSummarizer1() {
        return new JsonSummary();
    }

    @Override
    public JsonJoinConfiguration divide(Summary<Object> s1, Summary<Object> s2) {
        JsonSummary jsonSummary1 = (JsonSummary) s1;
        JsonSummary jsonSummary2 = (JsonSummary) s2;

        int min = Math.min(jsonSummary1.getMinSize(), jsonSummary2.getMinSize());

        return new JsonJoinConfiguration(min);
    }

    private int[] getBucketForTreeSize(int treeSize, JsonJoinConfiguration jsonJoinConfiguration) {
        // normalize buckets since idk if it is less efficient if our buckets don't start at 0
        return new int[]{treeSize - jsonJoinConfiguration.getMinSize()};
    }

    @Override
    public int[] assign1Parsed(List<Node> k1, JsonJoinConfiguration jsonJoinConfiguration) {
        return getBucketForTreeSize(k1.size(), jsonJoinConfiguration);
    }

    @Override
    public int[] assign1(Object k1, JsonJoinConfiguration jsonJoinConfiguration) {
        int jsonTreeSize;

        try {
            jsonTreeSize = JSON_TREE_CONVERTER_HELPER.calculateTreeSize((IVisitablePointable) k1);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        return getBucketForTreeSize(jsonTreeSize, jsonJoinConfiguration);
    }

    @Override
    public boolean match(int b1, int b2) {
        // buckets represent (normalized) tree sizes => can easily apply size filter
        return Math.abs(b1 - b2) <= THRESHOLD;
    }

    @Override
    public boolean verifyParsed(List<Node> k1, List<Node> k2) {
        return JEDI_VERIFIER.verifyParsed(k1, k2, THRESHOLD);
    }

    @Override
    public boolean verify(Object k1, Object k2) {
        return JEDI_VERIFIER.verify((IVisitablePointable) k1, (IVisitablePointable) k2, THRESHOLD);
    }

    @Override
    public boolean verify(int b1, Object k1, int b2, Object k2, JsonJoinConfiguration c) {
        // due to using an assign-one approach, there can't be any duplicates that need to be filtered out
        return verify(k1, k2);
    }
}