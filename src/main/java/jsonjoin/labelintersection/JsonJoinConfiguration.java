package jsonjoin.labelintersection;

import org.apache.asterix.external.cartilage.base.Configuration;

import java.util.HashMap;
import java.util.Map;

public class JsonJoinConfiguration implements Configuration {

    // key: (label, type), value: bucket
    private Map<LabelTypeTuple, Integer> bucketAssignments = new HashMap<>();

    public Map<LabelTypeTuple, Integer> getBucketAssignments() {
        return bucketAssignments;
    }

}