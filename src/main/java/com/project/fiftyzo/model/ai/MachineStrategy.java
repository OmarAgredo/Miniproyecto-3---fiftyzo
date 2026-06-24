package com.project.fiftyzo.model.ai;

import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.Card;
import com.project.fiftyzo.model.PlayableMove;
import java.util.List;

/** Chooses a valid move for a machine player. */
public interface MachineStrategy { PlayableMove chooseMove(List<Card> hand, int currentSum) throws NoPlayableCardException; }
