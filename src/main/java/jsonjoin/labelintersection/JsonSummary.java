package jsonjoin.labelintersection;

import org.apache.asterix.runtime.evaluators.common.Node;
import jsonjoin.jsontools.JSONTreeConverterHelper;
import org.apache.asterix.external.cartilage.base.Summary;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSummary implements Summary<Object> {

    // stores how many times a given (label, type) occurs
    private Map<LabelTypeTuple, MutableInt> labelTypeCounts = new HashMap<>();

    public void add(Object k) {
        IVisitablePointable key = (IVisitablePointable) k;
        List<Node> jsonTree;
        // would be cool to have this as a member variable in order to be able to reuse buffers but sadly
        // a Summary has to be serializable
        JSONTreeConverterHelper JSONTreeConverterHelper = new JSONTreeConverterHelper();

        try {
            jsonTree = JSONTreeConverterHelper.toTree(key);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        for (Node node : jsonTree) {
            LabelTypeTuple lookupKey = new LabelTypeTuple(node.getLabel(), node.getType());
            MutableInt count = labelTypeCounts.get(key);

            if (count == null) {
                labelTypeCounts.put(lookupKey, new MutableInt(1));
            } else {
                count.increment();
            }
        }
    }

    @Override
    public void add(Summary<Object> s) {
        JsonSummary jsonSummary = (JsonSummary) s;
        var otherLabelTypeCounts = jsonSummary.labelTypeCounts;

        for (var ltc : otherLabelTypeCounts.entrySet()) {
            LabelTypeTuple lookupKey = ltc.getKey();
            MutableInt count = labelTypeCounts.get(lookupKey);
            MutableInt otherCount = ltc.getValue();

            //labelTypeCounts.merge(ltc.getKey(), ltc.getValue(), (x, y) -> new MutableInt(x.getValue() + y.getValue()));
            if (count == null) {
                labelTypeCounts.put(lookupKey, new MutableInt(otherCount.getValue()));
            } else {
                count.add(otherCount.getValue());
            }
        }
    }

    public Map<LabelTypeTuple, MutableInt> getLabelTypeCounts() {
        return labelTypeCounts;
    }
}