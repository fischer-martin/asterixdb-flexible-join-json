package jsonjoin.labelintersection;

import jsonjoin.FlexibleJSONJoin;
import jsonjoin.jsontools.JEDIVerifier;
import jsonjoin.jsontools.JSONTreeConverterHelper;
import org.apache.asterix.runtime.evaluators.common.Node;
import org.apache.asterix.external.cartilage.base.Summary;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.*;

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
        JsonJoinConfiguration config = new JsonJoinConfiguration();
        JsonSummary combinedSummaries = new JsonSummary();

        // TODO: maybe optimize this mess

        combinedSummaries.add((JsonSummary) s1);
        combinedSummaries.add((JsonSummary) s2);

        // Create a list where the tuples are ordered by their inverted frequency
        // (i.e. tuples with a lower frequency come before tuples with a higher frequency).
        // In order to have the total order required for the prefix filter, we subsort (see labelTypeTuple.compareTo()) by (label, type)
        // (i.e. we have a lexicographic order that first looks at the frequency and then at the (label, type)).
        // We have to do this lexicographic ordering since if we didn't, then we would only have a total preorder instead.
        final List<LabelTypeTuple> invertedFrequencySortedLabelTypeTuples = combinedSummaries
                .getLabelTypeCounts().entrySet()
                .stream()
                .sorted(Map.Entry.<LabelTypeTuple, MutableInt>comparingByValue(Comparator.naturalOrder())
                        .thenComparing(Map.Entry.<LabelTypeTuple, MutableInt>comparingByKey()))
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // the first [0, threshold] buckets are reserved for trees with a size in [0, threshold]
        int bucketCounter = (int) THRESHOLD;

        // insert (label, type) tuples into a hash map and assign lower buckets to less frequent tuples
        for (LabelTypeTuple key : invertedFrequencySortedLabelTypeTuples)
            config.getBucketAssignments().put(key, ++bucketCounter);

        return config;
    }

    private class LabelTypeBucketTuple implements Comparable {
        private final LabelTypeTuple labelTypeTuple;
        private final Integer bucket;

        public LabelTypeBucketTuple(LabelTypeTuple labelTypeTuple, Integer bucket) {
            this.labelTypeTuple = labelTypeTuple;
            this.bucket = bucket;
        }

        public Integer getBucket() {
            return bucket;
        }

        @Override
        public int compareTo(Object o) {
            return bucket.compareTo(((LabelTypeBucketTuple) o).getBucket());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            LabelTypeBucketTuple that = (LabelTypeBucketTuple) o;
            // TODO: the way that we use this class (where for (label, type)-tuples a, b it holds that a == b iff bucket(a) == bucket(b)) should allow us to simplify this.
            return labelTypeTuple.equals(that.labelTypeTuple) && getBucket().equals(that.getBucket());
        }

        @Override
        public int hashCode() {
            return Objects.hash(labelTypeTuple, getBucket());
        }
    }

    @Override
    public int[] assign1(Object k1, JsonJoinConfiguration jsonJoinConfiguration) {
        List<Node> jsonTree;

        try {
            jsonTree = JSON_TREE_CONVERTER_HELPER.toTree((IVisitablePointable) k1);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        return assign1Parsed(jsonTree, jsonJoinConfiguration);
    }

    @Override
    public int[] assign1Parsed(List<Node> jsonTree, JsonJoinConfiguration jsonJoinConfiguration) {
        final int PREFIX_LENGTH = (int) THRESHOLD + 1;
        int[] buckets;

        // compute deduped (min(|T|, threshold + 1))-prefix
        if (jsonTree.size() <= THRESHOLD) {
            // Two trees T1, T2 will have JEDI(T1, T2) <= THRESHOLD if |T1| + |T2| <= THRESHOLD even if they don't
            // have a (label, type) tuple in common. => put tree T into bucket |T| and accommodate for this in match()
            TreeSet<LabelTypeBucketTuple> invertedFrequency = new TreeSet<>();

            for (Node node : jsonTree) {
                LabelTypeTuple ltt = new LabelTypeTuple(node.getLabel(), node.getType());
                // works since both TreeSet.add() and Map.get() use equals() internally (see their doc)
                invertedFrequency.add(new LabelTypeBucketTuple(ltt, jsonJoinConfiguration.getBucketAssignments().get(ltt)));
            }

            buckets = new int[invertedFrequency.size() + 1];
            buckets[0] = jsonTree.size();

            Iterator<LabelTypeBucketTuple> it = invertedFrequency.iterator();
            for (int i = 1; it.hasNext(); ++i) {
                LabelTypeBucketTuple labelTypeBucketTuple = it.next();
                buckets[i] = labelTypeBucketTuple.getBucket();
            }
        } else {
            PriorityQueue<LabelTypeBucketTuple> invertedFrequency = new PriorityQueue<>(jsonTree.size());

            for (Node node : jsonTree) {
                LabelTypeTuple ltt = new LabelTypeTuple(node.getLabel(), node.getType());
                invertedFrequency.add(new LabelTypeBucketTuple(ltt, jsonJoinConfiguration.getBucketAssignments().get(ltt)));
            }

            // We don't know yet if we will have any duplicate (label, type) tuples, so we will have to
            // allocate enough memory in case there won't be any duplicates.
            int[] bucketsOversized = new int[PREFIX_LENGTH];

            LabelTypeBucketTuple labelTypeBucketTuple = invertedFrequency.poll();
            LabelTypeBucketTuple prevLabelTypeBucketTuple = labelTypeBucketTuple;
            bucketsOversized[0] = labelTypeBucketTuple.getBucket();
            int nextBucketSlot = 1;
            for (int i = 1; i < PREFIX_LENGTH; ++i) {
                labelTypeBucketTuple = invertedFrequency.poll();

                if (!labelTypeBucketTuple.equals(prevLabelTypeBucketTuple)) {
                    bucketsOversized[nextBucketSlot] = labelTypeBucketTuple.getBucket();
                    ++nextBucketSlot;
                }
                // This could also be placed in the if-block, but it's cleaner this way.
                prevLabelTypeBucketTuple = labelTypeBucketTuple;
            }

            // The nextBucketSlot obviously also describes the number of buckets we assigned.
            int numAssignedBuckets = nextBucketSlot;
            if (numAssignedBuckets != bucketsOversized.length) {
                buckets = new int[numAssignedBuckets];
                System.arraycopy(bucketsOversized, 0, buckets, 0, numAssignedBuckets);
            } else
                buckets = bucketsOversized;
        }

        return buckets;
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
        return JEDI_VERIFIER.duplicateAvoidingVerify(this, b1, (IVisitablePointable) k1, b2, (IVisitablePointable) k2, c);
    }

    @Override
    public boolean match(int b1, int b2) {
        // guaranteed match through de- and reconstruction of both trees (buckets in [0, THRESHOLD] are reserved for
        // trees with a size in [0, THRESHOLD]).
        if (b1 + b2 <= THRESHOLD)
            return true;
        else
            return b1 == b2;
    }

}