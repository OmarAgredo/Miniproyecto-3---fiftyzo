package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.ai.DecisionTreeStrategy;
import com.project.fiftyzo.model.ai.MachineStrategy;

/**
 * Represents an automated participant in the model layer.
 * The machine delegates move selection to a pluggable {@link MachineStrategy}.
 */
public final class MachinePlayer extends Player {
    private final MachineStrategy strategy;

    /**
     * Creates a machine player using the default decision-tree strategy.
     *
     * @param name display name for the machine player
     */
    public MachinePlayer(String name) { this(name, new DecisionTreeStrategy()); }

    /**
     * Creates a machine player using the supplied strategy.
     *
     * @param name display name for the machine player
     * @param strategy strategy used to choose legal moves
     */
    public MachinePlayer(String name, MachineStrategy strategy) { super(name); this.strategy = java.util.Objects.requireNonNull(strategy); }

    /**
     * Chooses the machine's next move for the current table sum.
     *
     * @param currentSum current table sum
     * @return selected playable move
     * @throws NoPlayableCardException if the machine has no legal move
     */
    public PlayableMove chooseMove(int currentSum) throws NoPlayableCardException { return strategy.chooseMove(getHandSnapshot(), currentSum); }
}
