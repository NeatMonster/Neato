package fr.neatmonster.neato;

public class Gene {
    public int     input      = 0;
    public int     output     = 0;
    public double  weight     = 0.0;
    public boolean enabled    = true;
    public int     innovation = 0;

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
