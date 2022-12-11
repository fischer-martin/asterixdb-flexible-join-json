package jsonjoin.lengthfilter;

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
        // due to using an assign-one approach, there can't be any duplicates that need to be filtered out
        return verify(k1, k2);
    }

    @Override
    public boolean joFilterAndVerifyParsed(JOFilterTree k1, JOFilterTree k2) {
        return this.JEDI_VERIFIER.joFilterAndVerifyParsed(k1, k2, THRESHOLD);
    }

}
