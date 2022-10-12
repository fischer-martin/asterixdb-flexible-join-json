package jsonjoin.lengthfilter;

import org.apache.asterix.external.cartilage.base.Configuration;

public class JsonJoinConfiguration implements Configuration {
    private int minSize;

    public JsonJoinConfiguration(int minSize) {
        this.minSize = minSize;
    }

    public int getMinSize() {
        return minSize;
    }

}