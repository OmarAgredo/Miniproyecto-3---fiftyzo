package com.project.fiftyzo.model.ai;

import com.project.fiftyzo.model.PlayableMove;
import java.util.ArrayList;
import java.util.List;

/** Node in the machine's shallow move decision tree. */
public final class DecisionNode {
    private final PlayableMove move;
    private final List<DecisionNode> children = new ArrayList<>();
    private int score;
    public DecisionNode(PlayableMove move) { this.move = move; }
    public void addChild(DecisionNode child) { children.add(child); }
    public PlayableMove getMove() { return move; }
    public List<DecisionNode> getChildren() { return List.copyOf(children); }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
