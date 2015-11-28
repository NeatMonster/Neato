package fr.neatmonster.neato;

import static fr.neatmonster.neato.Population.BIAS_MUT;
import static fr.neatmonster.neato.Population.BIAS_PERT;
import static fr.neatmonster.neato.Population.BIAS_STEP;
import static fr.neatmonster.neato.Population.CONNECT_MUT;
import static fr.neatmonster.neato.Population.CONNECT_PERT;
import static fr.neatmonster.neato.Population.CONNECT_STEP;
import static fr.neatmonster.neato.Population.DELTA_DISJOINT;
import static fr.neatmonster.neato.Population.DELTA_EXCESS;
import static fr.neatmonster.neato.Population.DELTA_THRESHOLD;
import static fr.neatmonster.neato.Population.DELTA_WEIGHTS;
import static fr.neatmonster.neato.Population.DISABLE_MUT;
import static fr.neatmonster.neato.Population.ENABLE_MUT;
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
import java.util.Map.Entry;

public class Individual {
    public final Map<Integer, Gene>   genotype = new HashMap<Integer, Gene>();
    public final Map<Integer, Double> biases   = new HashMap<Integer, Double>();

    public final List<Synapse> connects = new ArrayList<Synapse>();
    public final List<Neuron>  inputs   = new ArrayList<Neuron>();
    public final List<Neuron>  hidden   = new ArrayList<Neuron>();
    public final List<Neuron>  outputs  = new ArrayList<Neuron>();

    public double fitness;
//    public double[] fitness;
//    public int ranking;
//    public double distance;
//    public int global;

    public Individual() {}

    public Individual(final Individual mother, final Individual father) {
        for (final Entry<Integer, Gene> chromo : mother.genotype.entrySet()) {
            final Gene fatherChromo = father.genotype.get(chromo.getKey());
            if (fatherChromo == null || RANDOM.nextBoolean())
                genotype.put(chromo.getKey(), chromo.getValue().clone());
            else
                genotype.put(chromo.getKey(), fatherChromo.clone());
        }

        biases.putAll(father.biases);
        biases.putAll(mother.biases);
    }

    @Override
    public Individual clone() {
        final Individual individual = new Individual();

        for (final Entry<Integer, Gene> chromo : genotype.entrySet())
            individual.genotype.put(chromo.getKey(), chromo.getValue().clone());
        individual.biases.putAll(biases);

        return individual;
    }

    public double disjoint(final Individual other) {
        double disjointGenes = 0.0;

        for (final Integer innov : genotype.keySet())
            if (!other.genotype.containsKey(innov))
                ++disjointGenes;

        return disjointGenes / Math.max(genotype.size(), other.genotype.size());
    }

//    public boolean dominates(final Individual other) {
//        boolean dominates = false;
//        for (int i = 0; i < fitness.length; ++i)
//            if (i < 3) {
//                if (fitness[i] < other.fitness[i])
//                    return false;
//                else if (fitness[i] > other.fitness[i])
//                    dominates = true;
//            } else {
//                if (fitness[i] > other.fitness[i])
//                    return false;
//                else if (fitness[i] < other.fitness[i])
//                    dominates = true;
//            }
//        return dominates;
//    }

    public double excess(final Individual other) {
        if (genotype.isEmpty() && other.genotype.isEmpty())
            return 0;

        int last = 0;
        for (final Integer innov : genotype.keySet())
            if (innov > last)
                last = innov;

        int otherLast = 0;
        for (final Integer otherInnov : other.genotype.keySet())
            if (otherInnov > otherLast)
                otherLast = otherInnov;

        return Math.abs(last - otherLast)
                / Math.max(genotype.size(), other.genotype.size());
    }

    public void feedForward() {
        for (final Neuron neuron : inputs)
            neuron.updated = false;
        for (final Neuron neuron : hidden)
            neuron.updated = false;
        for (final Neuron neuron : outputs)
            neuron.updated = false;
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

        for (final Gene gene : genotype.values()) {
            if (!neurons.containsKey(gene.input)) {
                final Neuron neuron = new Neuron(gene.input);
                // Highly experimental
                neuron.bias = biases.get(gene.input);
                neurons.put(neuron.neuronId, neuron);
                hidden.add(neuron);
            }
            final Neuron input = neurons.get(gene.input);

            if (!neurons.containsKey(gene.output)) {
                final Neuron neuron = new Neuron(gene.output);
                // Highly experimental
                neuron.bias = biases.get(gene.output);
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

    public boolean hasConnection(final int in, final int out) {
        for (final Gene gene : genotype.values())
            if (gene.input == in && gene.output == out)
                return true;
        return false;
    }

    public void mutate() {
        if (RANDOM.nextDouble() < CONNECT_MUT)
            for (final Gene gene : genotype.values())
                if (RANDOM.nextDouble() < CONNECT_PERT)
                    gene.weight += 2.0 * RANDOM.nextDouble() * CONNECT_STEP
                            - CONNECT_STEP;
                else
                    gene.weight = 2.0 * RANDOM.nextDouble() - 1.0;

        for (final Gene gene : genotype.values())
            if (gene.enabled && RANDOM.nextDouble() < DISABLE_MUT)
                gene.enabled = false;

        for (final Gene gene : genotype.values())
            if (!gene.enabled && RANDOM.nextDouble() < ENABLE_MUT)
                gene.enabled = true;

        // Highly experimental
        if (RANDOM.nextDouble() < BIAS_MUT)
            for (final int neuron : biases.keySet())
                if (RANDOM.nextDouble() < BIAS_PERT)
                    biases.put(neuron,
                            biases.get(neuron)
                                    + 2.0 * RANDOM.nextDouble() * BIAS_STEP
                                    - BIAS_STEP);
                else
                    biases.put(neuron, 2.0 * RANDOM.nextDouble() - 1.0);

        if (RANDOM.nextDouble() < LINK_MUT) {
            int input, output, tries = 32;
            do {
                input = randomNeuron(true);
                output = randomNeuron(false);
            } while ((input == output || isOutput(input) && isOutput(output)
                    || hasConnection(input, output)) && --tries > 0);

            if (tries > 0) {
                int innov = Innovations.checkInnovation(input, output, false);
                if (innov == -1)
                    innov = Innovations.addLink(input, output);

                final Gene gene = new Gene();
                gene.input = input;
                gene.output = output;
                gene.innovation = innov;
                genotype.put(innov, gene);
            }
        }

        if (RANDOM.nextDouble() < NODE_MUT) {
            final List<Gene> genes = new ArrayList<Gene>();
            for (final Gene gene : genotype.values())
                if (gene.enabled)
                    genes.add(gene);

            if (!genes.isEmpty()) {
                final Gene gene = genes.get(RANDOM.nextInt(genes.size()));
                gene.enabled = false;

                int neuron, input, output;
                final int innovId = Innovations.checkInnovation(gene.input,
                        gene.output, true);
                if (innovId == -1) {
                    neuron = Innovations.addNeuron(gene.input, gene.output);
                    input = Innovations.addLink(gene.input, neuron);
                    output = Innovations.addLink(neuron, gene.output);
                } else {
                    neuron = Innovations.findNeuron(gene.input, gene.output);
                    input = Innovations.checkInnovation(gene.input, neuron,
                            false);
                    output = Innovations.checkInnovation(neuron, gene.output,
                            false);
                }

                // Highly experimental
                biases.put(neuron, 2.0 * RANDOM.nextDouble() - 1.0);

                final Gene inputGene = new Gene();
                inputGene.input = gene.input;
                inputGene.output = neuron;
                inputGene.weight = 1.0;
                inputGene.innovation = input;

                final Gene outputGene = new Gene();
                outputGene.input = neuron;
                outputGene.output = gene.output;
                outputGene.weight = gene.weight;
                outputGene.innovation = output;

                genotype.put(input, inputGene);
                genotype.put(output, outputGene);
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

        for (final Gene gene : genotype.values()) {
            if (addInput || !isInput(gene.input))
                neurons.add(gene.input);
            if (addInput || !isInput(gene.output))
                neurons.add(gene.output);
        }

        return neurons.get(RANDOM.nextInt(neurons.size()));
    }

    public boolean sameSpecies(final Individual other) {
        final double de = DELTA_EXCESS * excess(other);
        final double dd = DELTA_DISJOINT * disjoint(other);
        final double dw = DELTA_WEIGHTS * weights(other);
        return de + dd + dw < DELTA_THRESHOLD;
    }

    public void setInput(final double[] input) {
        for (int i = 0; i < INPUTS; ++i)
            inputs.get(i).value = input[i];
    }

    public double weights(final Individual other) {
        double sum = 0.0, coincident = 0.0;

        for (final Entry<Integer, Gene> chromo : genotype.entrySet()) {
            final Gene otherChromo = other.genotype.get(chromo.getKey());
            if (otherChromo != null) {
                sum += Math.abs(chromo.getValue().weight - otherChromo.weight);
                ++coincident;
            }
        }

        return sum / coincident;
    }
}
