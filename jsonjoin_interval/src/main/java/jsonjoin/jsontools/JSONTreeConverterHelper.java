package jsonjoin.jsontools;

import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.apache.asterix.builders.ArrayListFactory;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.util.container.IObjectPool;
import org.apache.asterix.om.util.container.ListObjectPool;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

public class JSONTreeConverterHelper {

    private MutablePair<List<Node>, IntIntPair> transArg = new MutablePair<>();
    private IntIntPair transCnt = new IntIntMutablePair(0, 0);
    private final IObjectPool<List<Node>, ATypeTag> listAllocator = new ListObjectPool<>(new ArrayListFactory<Node>());
    private final JSONTreeTransformator treeTransformator = new JSONTreeTransformator();

    public List<Node> toTree(IVisitablePointable input) throws HyracksDataException {
        // Convert the given data items into JSON trees.
        List<Node> postToNode1 = listAllocator.allocate(null);
        transArg.setLeft(postToNode1);
        transCnt.first(0);
        transCnt.second(0);
        transArg.setRight(transCnt);
        return treeTransformator.toTree(input, transArg);
    }

    // TODO: maybe use this somewhere
    // TODO: also reset listAllocator?
    public void reset() {
        treeTransformator.reset();
    }

}
