package jsonjoin.lengthfilter;

import jsonjoin.jsontools.JEDICalculator;
import jsonjoin.jsontools.JEDIVerifier;
import jsonjoin.jsontools.JSONTreeConverterHelper;
import org.apache.asterix.runtime.evaluators.common.Node;
import org.apache.asterix.external.cartilage.base.FlexibleJoin;
import org.apache.asterix.external.cartilage.base.Summary;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

// For some reason we can only use Object instead of IVisitablePointable or else the DB will throw
// HYR0066: Data pipeline protocol violation: fail() is not called by the upstream when there is a failure in the downstream [HyracksDataException]
// Maybe (sadly I am not a wizard who knows everything about Java) this happens due to IVisitablePointable being an interface.
public class JsonJoin implements FlexibleJoin<Object, JsonJoinConfiguration> {

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
        int max = Math.max(jsonSummary1.getMaxSize(), jsonSummary2.getMaxSize());
        // TODO: check again if this is correct
        int numBuckets = Math.max(1, max - min);

        return new JsonJoinConfiguration(min, max, numBuckets);
    }

    private int clamp(int val, int min, int max) {
        return Math.min(Math.max(min, val), max);
    }

    private int getBucket(double num, JsonJoinConfiguration jsonJoinConfiguration) {
        int minSize = jsonJoinConfiguration.getMinSize();
        int maxSize = jsonJoinConfiguration.getMaxSize();
        int numBuckets = jsonJoinConfiguration.getNumBuckets();
        int firstBucket = 1;
        int lastBucket = numBuckets;

        // needs to be clamped to get correct result if num == minSize or num > maxSize or minSize == maxSize
        int bucket = (int) Math.ceil(((num - minSize) * numBuckets) / (maxSize - minSize));

        return clamp(bucket, firstBucket, lastBucket);
    }

    @Override
    public int[] assign1(Object k1, JsonJoinConfiguration jsonJoinConfiguration) {
        int jsonTreeSize;

        try {
            jsonTreeSize = JSON_TREE_CONVERTER_HELPER.calculateTreeSize((IVisitablePointable) k1);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        int firstBucket = getBucket(jsonTreeSize, jsonJoinConfiguration);
        int lastBucket = getBucket(jsonTreeSize + THRESHOLD, jsonJoinConfiguration);
        int[] buckets = new int[lastBucket - firstBucket + 1];

        for (int i = 0; i < buckets.length; ++i)
            buckets[i] = firstBucket + i;

        return buckets;
    }

    @Override
    public boolean verify(Object k1, Object k2) {
        return JEDI_VERIFIER.verify((IVisitablePointable) k1, (IVisitablePointable) k2, THRESHOLD);
    }
}