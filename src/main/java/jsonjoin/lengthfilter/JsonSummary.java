package jsonjoin.lengthfilter;

import org.apache.asterix.external.cartilage.base.Summary;

/**
 * This class doesn't serve any actual use (apart from being required by the framework) since we don't need to
 * store any information in order to use the length filter.
 */
public class JsonSummary implements Summary<Object> {

    public void add(Object k) {
        // nop
    }

    @Override
    public void add(Summary<Object> s) {
        // nop
    }

}