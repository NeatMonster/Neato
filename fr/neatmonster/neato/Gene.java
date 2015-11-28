package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.RANDOM;

public class Gene {
    public boolean enabled    = true;
    public int     innovation = 0;
    public int     input      = 0;
    public int     output     = 0;
    public double  weight     = 2.0 * RANDOM.nextDouble() - 1.0;

    @Override
    public Gene clone() {
        final Gene gene = new Gene();
        gene.input = input;
        gene.output = output;
        gene.weight = weight;
        gene.enabled = enabled;
        gene.innovation = innovation;
        return gene;
    }
}
