package jsonjoin.jsontools;

import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.apache.asterix.builders.ArrayListFactory;
import org.apache.asterix.om.pointables.base.IVisitablePointable;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.util.container.IObjectPool;
import org.apache.asterix.om.util.container.ListObjectPool;
import org.apache.asterix.runtime.evaluators.common.JOFilterTree;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.asterix.runtime.evaluators.common.JSONTreeTransformator;
import org.apache.asterix.runtime.evaluators.common.Node;
import org.apache.hyracks.api.exceptions.HyracksDataException;

import java.util.List;

public class JSONTreeConverterHelper {

    private MutablePair<List<Node>, IntIntPair> transArgJSONTree = new MutablePair<>();
    private IntIntPair transCntJSONTree = new IntIntMutablePair(0, 0);
    private MutablePair<JOFilterTree, int[]> transArgJOFilterTree = new MutablePair<>();
    private int[] transCntJOFilterTree = new int[3];
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
        transArgJSONTree.setLeft(postorderedTree);
        transCntJSONTree.first(0);
        transCntJSONTree.second(0);
        transArgJSONTree.setRight(transCntJSONTree);

        return treeTransformator.toTree(input, transArgJSONTree);
    }

    public JOFilterTree toJOFilterTree(IVisitablePointable input, JOFilterTree tree) throws HyracksDataException {
        tree.reset();
        transArgJOFilterTree.setLeft(tree);
        transCntJOFilterTree[0] = 0;
        transCntJOFilterTree[1] = 0;
        transCntJOFilterTree[2] = 0;
        transArgJOFilterTree.setRight(transCntJOFilterTree);

        return treeTransformator.toJOFilterTree(input, transArgJOFilterTree);
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
