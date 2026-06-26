package com.project.fiftyzo.model.ai;

import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.Card;
import com.project.fiftyzo.model.PlayableMove;
import java.util.List;

/**
 * Strategy contract for choosing a valid move for a machine player.
 * Implementations evaluate the machine hand without modifying game state.
 */
public interface MachineStrategy {
    /**
     * Chooses one playable move from the supplied hand.
     *
     * @param hand immutable snapshot of the machine player's hand
     * @param currentSum current table sum
     * @return selected playable move
     * @throws NoPlayableCardException if the hand contains no legal move
     */
    PlayableMove chooseMove(List<Card> hand, int currentSum) throws NoPlayableCardException;
}
