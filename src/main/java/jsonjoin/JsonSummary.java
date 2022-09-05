package jsonjoin;

import jsonjoin.jsontools.JSONTreeConverterHelper;
import org.apache.asterix.external.cartilage.base.Summary;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.hyracks.api.exceptions.HyracksDataException;

public class JsonSummary implements Summary<Object> {

    private int minSize = Integer.MIN_VALUE;
    private int maxSize = Integer.MAX_VALUE;

    public void add(Object k) {
        IVisitablePointable key = (IVisitablePointable) k;
        int jsonTreeSize;
        JSONTreeConverterHelper JSONTreeConverterHelper = new JSONTreeConverterHelper();

        try {
            jsonTreeSize = JSONTreeConverterHelper.calculateTreeSize(key);
        } catch (HyracksDataException e) {
            throw new RuntimeException(e);
        }

        if (jsonTreeSize < minSize) {
            minSize = jsonTreeSize;
        }
        if (jsonTreeSize > maxSize) {
            maxSize = jsonTreeSize;
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