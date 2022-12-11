package jsonjoin.lengthfilter;

import org.apache.asterix.runtime.evaluators.common.Node;

import java.util.List;

public class CandidateReturner extends JsonJoin {

    public CandidateReturner(double threshold) {
        super(threshold);
    }

    @Override
    public boolean verifyParsed(List<? extends Node> k1, List<? extends Node> k2) {
        // Overriding this method isn't actually necessary since the framework actually only cares about the
        // verify(int b1, Object k1, int b2, Object k2, JsonJoinConfiguration c) method, but I'm still doing
        // it for the sake of completeness,
        return true;
    }

    @Override
    public boolean verify(Object k1, Object k2) {
        // Overriding this method isn't actually necessary since the framework actually only cares about the
        // verify(int b1, Object k1, int b2, Object k2, JsonJoinConfiguration c) method, but I'm still doing
        // it for the sake of completeness,
        return true;
    }

    @Override
    public boolean verify(int b1, Object k1, int b2, Object k2, JsonJoinConfiguration c) {
        // skipping the actual verification step gives us a candidate
        return true;
    }

}
