package com.project.fiftyzo.model.ai;

import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.Card;
import com.project.fiftyzo.model.PlayableMove;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Heuristic strategy that evaluates every legal move in a shallow decision tree. */
public final class DecisionTreeStrategy implements MachineStrategy {
    @Override public PlayableMove chooseMove(List<Card> hand, int currentSum) throws NoPlayableCardException {
        DecisionNode root = buildTree(hand, currentSum);
        return root.getChildren().stream().max(Comparator.comparingInt(DecisionNode::getScore)).map(DecisionNode::getMove)
                .orElseThrow(() -> new NoPlayableCardException("The machine player has no playable card."));
    }
    public DecisionNode buildTree(List<Card> hand, int currentSum) {
        DecisionNode root = new DecisionNode(null);
        for (Card card : hand) for (int value : card.getPossibleValues()) {
            int result = currentSum + value;
            if (result <= 50) { DecisionNode child = new DecisionNode(new PlayableMove(card, value, result)); child.setScore(evaluateMove(child.getMove(), currentSum)); root.addChild(child); }
        }
        return root;
    }
    public int evaluateMove(PlayableMove move, int currentSum) {
        int result = move.getResultingSum();
        int score = 0;
        if (result >= 25 && result <= 40) score += 40;
        else score -= Math.abs(32 - result);
        if (result >= 45) score -= 45;
        if (currentSum >= 40 && move.getValue() < 0) score += 35;
        if (move.getCard().isNine()) score += currentSum >= 25 && currentSum <= 40 ? 12 : 4;
        if (move.getValue() < 0) score += 8;
        return score;
    }
}
