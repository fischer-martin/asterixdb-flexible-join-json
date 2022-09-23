package jsonjoin.labelintersection;

import org.apache.asterix.external.cartilage.base.Configuration;

import java.util.HashMap;
import java.util.Map;

public class JsonJoinConfiguration implements Configuration {

    // key: (label, type), value: bucket
    private Map<LabelTypeTuple, Integer> bucketAssignments = new HashMap<>();

    /**
     * Returns a map that assigns each (label, type) to a bucket (ordered according to a total order).
     * A configuration generated by {@link JsonJoin#divide} is guaranteed to have bucket assignments such that for
     * (label, type)-tuples a, b it holds that if bucket(a) < bucket(b) then frequency(a) <= frequency(b).
     * If frequency(a) = frequency(b), the order will still always be deterministic; i.e. for all possible
     * configurations where a and b have the same frequency, bucket(a) < bucket(b) will still hold.
     *
     * @return a map containing assignments from (label, type)-tuples to buckets
     */
    public Map<LabelTypeTuple, Integer> getBucketAssignments() {
        return bucketAssignments;
    }

}