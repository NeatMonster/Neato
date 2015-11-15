package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.CONNECT_MUT;
import static fr.neatmonster.neato.Population.CONNECT_PERT;
import static fr.neatmonster.neato.Population.DISABLE_MUT;
import static fr.neatmonster.neato.Population.ENABLE_MUT;
import static fr.neatmonster.neato.Population.FITNESS;
import static fr.neatmonster.neato.Population.INPUTS;
import static fr.neatmonster.neato.Population.LINK_MUT;
import static fr.neatmonster.neato.Population.NODE_MUT;
import static fr.neatmonster.neato.Population.OUTPUTS;
import static fr.neatmonster.neato.Population.RANDOM;
import static fr.neatmonster.neato.Population.isInput;
import static fr.neatmonster.neato.Population.isOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Individual {
    public final List<Gene> genotype = new ArrayList<Gene>();

    public final List<Synapse> connects   = new ArrayList<Synapse>();
    public final List<Neuron>  inputs     = new ArrayList<Neuron>();
    public final List<Neuron>  hidden     = new ArrayList<Neuron>();
    public final List<Neuron>  outputs    = new ArrayList<Neuron>();
    public int                 nextNeuron = INPUTS + OUTPUTS;

    public double[] fitness;
    public int      ranking  = 0;
    public double   distance = 0.0;

    public Individual() {}

    @Override
    public Individual clone() {
        final Individual individual = new Individual();
        individual.nextNeuron = nextNeuron;
        for (final Gene gene : genotype)
            individual.genotype.add(gene.clone());
        return individual;
    }

    public boolean dominates(final Individual other) {
        boolean dominates = false;
        for (int i = 0; i < FITNESS.length; ++i)
            if (FITNESS[i]) {
                if (fitness[i] > other.fitness[i])
                    return false;
                else if (fitness[i] < other.fitness[i])
                    dominates = true;
            } else if (fitness[i] < other.fitness[i])
                return false;
            else if (fitness[i] > other.fitness[i])
                dominates = true;
        return dominates;
    }

    public void feedForward() {
        for (final Neuron input : inputs)
            input.feedForward();
    }

    public void generate() {
        final Map<Integer, Neuron> neurons = new HashMap<Integer, Neuron>();

        for (int i = 0; i < INPUTS; ++i) {
            final Neuron input = new Neuron(i);
            inputs.add(input);
            neurons.put(input.neuronId, input);
        }

        for (int i = 0; i < OUTPUTS; ++i) {
            final Neuron output = new Neuron(INPUTS + i);
            outputs.add(output);
            neurons.put(output.neuronId, output);
        }

        for (final Gene gene : genotype) {
            if (!neurons.containsKey(gene.input)) {
                final Neuron neuron = new Neuron(gene.input);
                neurons.put(neuron.neuronId, neuron);
                hidden.add(neuron);
            }
            final Neuron input = neurons.get(gene.input);

            if (!neurons.containsKey(gene.output)) {
                final Neuron neuron = new Neuron(gene.output);
                neurons.put(neuron.neuronId, neuron);
                hidden.add(neuron);
            }
            final Neuron output = neurons.get(gene.output);

            final Synapse connect = new Synapse();
            connect.input = input;
            connect.output = output;
            connect.weight = gene.weight;
            connect.enabled = gene.enabled;
            connect.innovation = gene.innovation;
            connects.add(connect);

            input.outputs.add(connect);
            output.inputs.add(connect);
        }
    }

    public double[] getOutput() {
        final double[] output = new double[OUTPUTS];
        for (int i = 0; i < output.length; ++i)
            output[i] = outputs.get(i).value;
        return output;
    }

    public void mutate() {
        if (RANDOM.nextDouble() < CONNECT_MUT)
            for (final Gene gene : genotype)
                if (RANDOM.nextDouble() < CONNECT_PERT)
                    gene.weight = 2.0 * RANDOM.nextDouble() - 1.0;
                else if (RANDOM.nextDouble() < DISABLE_MUT)
                    gene.enabled = false;
                else if (RANDOM.nextDouble() < ENABLE_MUT)
                    gene.enabled = true;

        if (RANDOM.nextDouble() < LINK_MUT) {
            int input, output;
            do {
                input = randomNeuron(true);
                output = randomNeuron(false);
            } while (input == output || isOutput(input) && isOutput(output));

            final Gene gene = new Gene();
            gene.input = input;
            gene.output = output;
            gene.innovation = ++Population.nextInnovation;
            genotype.add(gene);
        }

        if (RANDOM.nextDouble() < NODE_MUT) {
            final List<Gene> genes = new ArrayList<Gene>();
            for (final Gene gene : genotype)
                if (gene.enabled)
                    genes.add(gene);

            if (!genes.isEmpty()) {
                final Gene gene = genes.get(RANDOM.nextInt(genes.size()));
                gene.enabled = false;

                final Gene inputGene = new Gene();
                inputGene.input = gene.input;
                inputGene.output = nextNeuron;
                inputGene.weight = 1.0;
                inputGene.innovation = ++Population.nextInnovation;

                final Gene outputGene = new Gene();
                outputGene.input = nextNeuron;
                outputGene.output = gene.output;
                outputGene.weight = gene.weight;
                outputGene.innovation = ++Population.nextInnovation;

                ++nextNeuron;

                genotype.add(inputGene);
                genotype.add(outputGene);
            }
        }
    }

    private int randomNeuron(final boolean addInput) {
        final List<Integer> neurons = new ArrayList<Integer>();

        if (addInput)
            for (int i = 0; i < INPUTS; ++i)
                neurons.add(i);

        for (int i = 0; i < OUTPUTS; ++i)
            neurons.add(INPUTS + i);

        for (final Gene gene : genotype) {
            if (addInput || !isInput(gene.input))
                neurons.add(gene.input);
            if (addInput || !isInput(gene.output))
                neurons.add(gene.output);
        }

        return neurons.get(RANDOM.nextInt(neurons.size()));
    }

    public void setInput(final double[] input) {
        for (int i = 0; i < INPUTS; ++i)
            inputs.get(i).value = input[i];
    }
}
