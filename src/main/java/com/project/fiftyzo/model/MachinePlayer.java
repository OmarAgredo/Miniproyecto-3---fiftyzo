package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.ai.DecisionTreeStrategy;
import com.project.fiftyzo.model.ai.MachineStrategy;

/** A participant that delegates card selection to a strategy. */
public final class MachinePlayer extends Player {
    private final MachineStrategy strategy;
    public MachinePlayer(String name) { this(name, new DecisionTreeStrategy()); }
    public MachinePlayer(String name, MachineStrategy strategy) { super(name); this.strategy = java.util.Objects.requireNonNull(strategy); }
    public PlayableMove chooseMove(int currentSum) throws NoPlayableCardException { return strategy.chooseMove(getHandSnapshot(), currentSum); }
}
