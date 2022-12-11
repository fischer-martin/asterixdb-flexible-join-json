package jsonjoin.labelintersection;

import jsonjoin.FlexibleJOFilterJSONJoin;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.asterix.runtime.evaluators.common.JOFilterTree;

public class JOFilterJsonJoin extends JsonJoin implements FlexibleJOFilterJSONJoin<Object, JsonJoinConfiguration> {

    public JOFilterJsonJoin(double threshold) {
        super(threshold);
    }

    @Override
    public boolean verify(Object k1, Object k2) {
        return JEDI_VERIFIER.joFilterAndVerify((IVisitablePointable) k1, (IVisitablePointable) k2, THRESHOLD);
    }

    @Override
    public boolean verify(int b1, Object k1, int b2, Object k2, JsonJoinConfiguration c) {
        return JEDI_VERIFIER.duplicateAvoidingJOFilterAndVerify(this, b1, (IVisitablePointable) k1, b2, (IVisitablePointable) k2, c);
    }

    @Override
    public boolean joFilterAndVerifyParsed(JOFilterTree k1, JOFilterTree k2) {
        return this.JEDI_VERIFIER.joFilterAndVerifyParsed(k1, k2, THRESHOLD);
    }

}
