import javax.swing.tree.TreeNode;
import java.util.*;

public class DecisionTree<E>{
    public DecisionTreeNode<E> root;

    public DecisionTree(DataFrameInterface<E> df, Map<Integer, Set<E>> possibleAttributes){
        this.root = new DecisionTreeNode<E>();
        this.DecisionTreeLearning(this.root, df, possibleAttributes);
    }

    private void DecisionTreeLearning(DecisionTree<E>.DecisionTreeNode<E> node, DataFrameInterface<E> df, Map<Integer, Set<E>> possibleAttributes){
        node.prediction = node.getNodePrediction(df);

        if (node.checkIfLeafNode(df) || possibleAttributes.isEmpty()){ // stopping criterion
            node.isLeaf = true;
        }

        else {

            // Choose best attribute for split
            Map<Integer, Double> attributesInfoGainMap = new HashMap<>();
            double currentEntropy = getLabelsEntropy(df);

            for (int attribute: possibleAttributes.keySet()){
                Set<E> attributeValues = possibleAttributes.get(attribute);
                double attributeGain = this.getAttributeInformationGain(df, attribute, attributeValues, currentEntropy);

                attributesInfoGainMap.put(attribute, attributeGain);
            }

            int bestAttribute = -1;
            double bestInfoGain = -1.0;
            for (int attribute: attributesInfoGainMap.keySet()){
                double attributeGain = attributesInfoGainMap.get(attribute);
                if (attributeGain > bestInfoGain){
                    bestInfoGain = attributeGain;
                    bestAttribute = attribute;
                }
            }

            // split using the best attribute
            Set<E> attributeValues = possibleAttributes.get(bestAttribute);
            for (E value: attributeValues){
                DataFrameInterface<E> splitDf = df.filterRowsByColumnValue(bestAttribute, value);
                if (!splitDf.isEmpty()){
                    Map<Integer, Set<E>> splitPossibleAttributes = new HashMap<>(possibleAttributes);
                    splitPossibleAttributes.remove(bestAttribute);
                    DecisionTree<E>.DecisionTreeNode<E> splitNode = new DecisionTreeNode<>(bestAttribute, value, splitDf);

                    // continue recursively and append branch to tree
                    DecisionTreeLearning(splitNode, splitDf, splitPossibleAttributes);
                    node.childs.add(splitNode);
                }
            }
        }
    }

    private double getAttributeInformationGain(DataFrameInterface<E> df, int attribute, Set<E> attributePossibleValues, double currentEntropy){
        double attributeGain = currentEntropy;
        for (E value: attributePossibleValues){
            DataFrameInterface<E> attributeDf = df.filterRowsByColumnValue(attribute, value);
            if (!attributeDf.isEmpty()){
                double valueEntropy = this.getLabelsEntropy(attributeDf);
                double valueProportion = (double) attributeDf.getNumRows() / df.getNumRows();
                attributeGain -= valueProportion * valueEntropy;
            }
        }
        return attributeGain;
    }

    public E predictForDataSample(SeriesInterface<E> row){
        boolean stopAtCurrentNode = false;
        DecisionTreeNode<E> currentNode = this.root;
        while(!stopAtCurrentNode){

            if (currentNode.isLeaf){
                stopAtCurrentNode = true;
            }

            else {
                boolean traversedToNextNode = false;
                for (DecisionTreeNode<E> childNode: currentNode.childs){
                    int childNodeIndex = childNode.attributeIndex;
                    E childNodeValue = childNode.attributeValue;
                    if (row.getElement(childNodeIndex).equals(childNodeValue)){ // continue traversing the tree
                        currentNode = childNode;
                        traversedToNextNode = true;
                        break;
                    }
                }
                if (!traversedToNextNode){
                    stopAtCurrentNode = true;
                }
            }
        }
        return currentNode.prediction;
    }

    private double getLabelsEntropy(DataFrameInterface<E> df){
        SeriesInterface<E> labelColumn = df.getCol(df.getNumCols() - 1);
        double entropy = 0;
        int numSamples = labelColumn.getLength();
        Map<E, Integer> labelsCount = labelColumn.getValueCounts();
        for (E label: labelsCount.keySet()){
            int labelCount = labelsCount.get(label);
            double labelProb = (double) labelCount / numSamples;
            entropy += -1 * labelProb * (Math.log(labelProb) / Math.log(2.0));
        }
        return entropy;
    }


    public class DecisionTreeNode<E>{
        private int attributeIndex;
        private E attributeValue;
        private boolean isLeaf;
        private boolean isRoot;
        private E prediction;
        private List<DecisionTreeNode<E>> childs;

        public DecisionTreeNode(int attributeIndex, E attributeValue, DataFrameInterface<E> df){
            this.attributeIndex = attributeIndex;
            this.attributeValue = attributeValue;
            this.isLeaf = false;
            this.isRoot = false;
            this.childs = new LinkedList<>();
        }

        public DecisionTreeNode(){
            this.isLeaf = false;
            this.isRoot = true;
            this.childs = new LinkedList<>();
        }

        private boolean checkIfLeafNode(DataFrameInterface<E> df){
            if (df.isEmpty()){
                return true;
            }

            SeriesInterface<E> labelsColumn = df.getCol(df.getNumCols() - 1);
            Set<E> uniqueLabels = labelsColumn.getUniqueValues();

            if (uniqueLabels.size() == 1){
                return true;
            }

            return false;
        }

        private E getNodePrediction(DataFrameInterface<E> df){
            SeriesInterface<E> labelsColumn = df.getCol(df.getNumCols() - 1);
            Map<E, Integer> labelsCount = labelsColumn.getValueCounts();

            return AbstractClassifier.getKeyForMaxValue(labelsCount);
        }
    }
}
