package jsonjoin.labelintersection;

import org.apache.asterix.runtime.evaluators.common.Node;

import java.util.List;

public class DedupedCandidateReturner extends JsonJoin {

    public DedupedCandidateReturner(double threshold) {
        super(threshold);
    }

    @Override
    public boolean verifyParsed(List<Node> k1, List<Node> k2) {
        return true;
    }

    @Override
    public boolean verify(Object k1, Object k2) {
        // Overriding this method isn't actually necessary since the JEDIVerifier.duplicateAvoidingVerify(
        // FlexibleJSONJoin join, int b1, IVisitablePointable k1, int b2, IVisitablePointable k2, Configuration c)
        // method only cares about the verifyParsed(List<Node> k1, List<Node> k2) method, but I'm still doing
        // it for the sake of completeness,
        return true;
    }

}
