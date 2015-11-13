package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.CROSSOVER;
import static fr.neatmonster.neato.Population.RANDOM;

import java.util.ArrayList;
import java.util.List;

public class Species {
    public final List<Individual> members   = new ArrayList<Individual>();
    public int                    topGlobal = 0;
    public double                 avgGlobal = 0.0;
    public int                    staleness = 0;

    public Individual breedChild() {
        final Individual child;
        if (RANDOM.nextDouble() < CROSSOVER) {
            final Individual mother = tournament();
            final Individual father = tournament();
            child = new Individual(mother, father);
        } else
            child = members.get(RANDOM.nextInt(members.size())).clone();
        child.mutate();
        child.generate();
        return child;
    }

    public void calcAvgGlobal() {
        double total = 0.0;
        for (final Individual member : members)
            total += member.global;
        avgGlobal = total / members.size();
    }

    public Individual tournament() {
        final Individual comp1 = members.get(RANDOM.nextInt(members.size()));
        final Individual comp2 = members.get(RANDOM.nextInt(members.size()));
        return comp1.global > comp2.global ? comp1 : comp2;
    }
}
