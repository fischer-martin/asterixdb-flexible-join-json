package jsonjoin.labelintersection;

public class ShadowJsonJoin extends JsonJoin {

    public ShadowJsonJoin(double threshold) {
        super(threshold);
    }

    @Override
    public boolean verify(int b1, Object k1, int b2, Object k2, JsonJoinConfiguration c) {
        return verify(k1, k2);
    }

}
