package jsonjoin;

import org.apache.asterix.external.cartilage.base.Configuration;

public class JsonJoinConfiguration implements Configuration {
    private int minSize;
    private int maxSize;
    private int numBuckets;

    public JsonJoinConfiguration(int minSize, int maxSize, int numBuckets) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.numBuckets = numBuckets;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getNumBuckets() {
        return numBuckets;
    }
}