package jsonjoin;

import org.apache.asterix.external.cartilage.base.FlexibleJoin;
import org.apache.asterix.runtime.evaluators.common.Node;

import java.util.List;

public interface FlexibleJSONJoin<T, C> extends FlexibleJoin<T, C> {

    boolean verifyParsed(List<? extends Node> k1, List<? extends Node> k2);

    int[] assign1Parsed(List<? extends Node> k1, C config);

    default int[] assign2Parsed(List<? extends Node> k2, C config) {
        return assign1Parsed(k2, config);
    }

}
