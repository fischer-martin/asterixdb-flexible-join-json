package jsonjoin;

import jsonjoin.jsontools.Node;
import jsonjoin.jsontools.JSONTreeConverterHelper;
import org.apache.asterix.external.cartilage.base.Summary;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

public class JsonSummary implements Summary<Object> {

    private int minSize = Integer.MIN_VALUE;
    private int maxSize = Integer.MAX_VALUE;

    public void add(Object k) {
        IVisitablePointable key = (IVisitablePointable) k;
        List<Node> jsonTree;
        JSONTreeConverterHelper JSONTreeConverterHelper = new JSONTreeConverterHelper();

        try {
            jsonTree = JSONTreeConverterHelper.toTree(key);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        if (jsonTree.size() < minSize) {
            minSize = jsonTree.size();
        }
        if (jsonTree.size() > maxSize) {
            maxSize = jsonTree.size();
        }
    }

    @Override
    public void add(Summary<Object> s) {
        JsonSummary jsonSummary = (JsonSummary) s;

        if (jsonSummary.minSize < minSize) {
            minSize = jsonSummary.minSize;
        }
        if (jsonSummary.maxSize > maxSize) {
            maxSize = jsonSummary.maxSize;
        }
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

}