package jsonjoin;

import org.apache.asterix.runtime.evaluators.common.JOFilterTree;

public interface FlexibleJOFilterJSONJoin<T, C> extends FlexibleJSONJoin<T, C> {

    boolean joFilterAndVerifyParsed(JOFilterTree k1, JOFilterTree k2);

}
