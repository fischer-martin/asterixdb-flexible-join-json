package jsonjoin.jsontools;

import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.apache.asterix.builders.ArrayListFactory;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.util.container.IObjectPool;
import org.apache.asterix.om.util.container.ListObjectPool;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.asterix.runtime.evaluators.common.JSONTreeTransformator;
import org.apache.asterix.runtime.evaluators.common.Node;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

public class JSONTreeConverterHelper {

    private MutablePair<List<Node>, IntIntPair> transArg = new MutablePair<>();
    private IntIntPair transCnt = new IntIntMutablePair(0, 0);
    private MutableInt nodeCounter = new MutableInt();
    private final IObjectPool<List<Node>, ATypeTag> listAllocator = new ListObjectPool<>(new ArrayListFactory<Node>());
    private final JSONTreeTransformator treeTransformator = new JSONTreeTransformator();

    /**
     * Converts a given IVisitablePointable into a JSON tree as it is described in Huetter et al. (SIGMOD 2022).
     * Call {@link JSONTreeConverterHelper#reset()} to free all buffers that this method uses (including the returned lists).
     * @param input the IVisitablePointable that should be converted to a JSON tree
     * @return a postordered List of the JSON tree's nodes
     * @throws HyracksDataException
     */
    public List<Node> toTree(IVisitablePointable input) throws HyracksDataException {
        // Convert the given data items into JSON trees.
        List<Node> postorderedTree = listAllocator.allocate(null);
        postorderedTree.clear(); // ListObjectPool reuses Lists but does not clear them
        transArg.setLeft(postorderedTree);
        transCnt.first(0);
        transCnt.second(0);
        transArg.setRight(transCnt);

        return treeTransformator.toTree(input, transArg);
    }

    /**
     * Frees all buffers used by {@link JSONTreeConverterHelper#toTree(IVisitablePointable)} including the lists that
     * it returns.
     */
    public void reset() {
        // free buffers
        treeTransformator.reset();
        listAllocator.reset();
    }

    public int calculateTreeSize(IVisitablePointable input)throws HyracksDataException {
        nodeCounter.setValue(0);

        return treeTransformator.calculateTreeSize(input, nodeCounter);
    }

}