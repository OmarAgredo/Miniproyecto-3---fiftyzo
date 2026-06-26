package com.project.fiftyzo.model.ai;

import com.project.fiftyzo.model.PlayableMove;
import java.util.ArrayList;
import java.util.List;

/**
 * Node in the machine player's shallow move decision tree.
 * The root node has no move; child nodes represent playable card-value options.
 */
public final class DecisionNode {
    private final PlayableMove move;
    private final List<DecisionNode> children = new ArrayList<>();
    private int score;

    /**
     * Creates a decision node for a possible move.
     *
     * @param move playable move represented by this node, or null for the root
     */
    public DecisionNode(PlayableMove move) { this.move = move; }

    /**
     * Adds a child move node.
     *
     * @param child child node to append
     */
    public void addChild(DecisionNode child) { children.add(child); }
    public PlayableMove getMove() { return move; }

    /**
     * Returns child nodes without exposing the mutable list.
     *
     * @return immutable snapshot of children
     */
    public List<DecisionNode> getChildren() { return List.copyOf(children); }
    public int getScore() { return score; }

    /**
     * Stores the heuristic score assigned to this node.
     *
     * @param score heuristic score
     */
    public void setScore(int score) { this.score = score; }
}
