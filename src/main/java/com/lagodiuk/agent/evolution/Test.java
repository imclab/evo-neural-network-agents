package com.lagodiuk.agent.evolution;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.lagodiuk.agent.Agent;
import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.AgentsEnvironmentListener;
import com.lagodiuk.agent.Fish;
import com.lagodiuk.agent.Food;
import com.lagodiuk.agent.Visualizator;
import com.lagodiuk.ga.Fitness;
import com.lagodiuk.ga.GeneticAlgorithm;
import com.lagodiuk.ga.IterartionListener;
import com.lagodiuk.ga.Population;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

public class Test {

	private static Random random = new Random();

	public static void main(String[] args) throws Exception {
		OptimizableNeuralNetwork bestBrain = evolveBestBrain(300);
		System.out.println(bestBrain);

		int environmentWidth = 600;
		int environmentHeight = 400;
		int fishesCount = 15;
		int foodCount = 10;

		AgentsEnvironment environment = new AgentsEnvironment(environmentWidth, environmentHeight);
		environment.addListener(new TournamentListener());

		for (int i = 0; i < fishesCount; i++) {
			NeuralNetworkDrivenFish fish =
					new NeuralNetworkDrivenFish(random.nextInt(environmentWidth), random.nextInt(environmentHeight), random.nextDouble() * 2 * Math.PI);
			fish.setBrain(bestBrain);
			environment.addAgent(fish);
		}

		for (int i = 0; i < foodCount; i++) {
			Food food = new Food(random.nextInt(environmentWidth), random.nextInt(environmentHeight));
			environment.addAgent(food);
		}

		final BufferedImage bufferedImage = new BufferedImage(environmentWidth, environmentHeight, BufferedImage.TYPE_INT_RGB);

		Graphics2D canvas = (Graphics2D) bufferedImage.getGraphics();
		canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final JFrame frame = new JFrame("Testing fishes visualizator");
		frame.setSize(environmentWidth + 100, environmentHeight + 100);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		for (;;) {
			Thread.sleep(50);
			environment.timeStep();
			Visualizator.paintEnvironment(canvas, environment);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					frame.getGraphics().drawImage(bufferedImage, 30, 30, null);
				}
			});
		}
	}

	private static OptimizableNeuralNetwork evolveBestBrain(int iterationsCount) {
		Population<OptimizableNeuralNetwork> brains = new Population<OptimizableNeuralNetwork>();
		int populationSize = 5;
		for (int i = 0; i < populationSize; i++) {
			brains.addChromosome(NeuralNetworkDrivenFish.randomNeuralNetworkBrain());
		}

		Fitness<OptimizableNeuralNetwork, Double> fit =
				new Fitness<OptimizableNeuralNetwork, Double>() {
					@Override
					public Double calculate(OptimizableNeuralNetwork chromosome) {
						int w = 200;
						int h = 200;
						AgentsEnvironment env = new AgentsEnvironment(w, h);
						for (int i = 0; i < 10; i++) {
							NeuralNetworkDrivenFish fish =
									new NeuralNetworkDrivenFish(random.nextInt(w), random.nextInt(h), 2 * Math.PI * random.nextDouble());
							fish.setBrain(chromosome);
							env.addAgent(fish);
						}
						for (int i = 0; i < 5; i++) {
							Food food = new Food(random.nextInt(w), random.nextInt(h));
							env.addAgent(food);
						}
						TournamentListener tournamentListener = new TournamentListener();
						env.addListener(tournamentListener);
						for (int i = 0; i < 50; i++) {
							env.timeStep();
						}

						double score = tournamentListener.getScore();

						return 1.0 / score;
					}
				};

		GeneticAlgorithm<OptimizableNeuralNetwork, Double> ga =
				new GeneticAlgorithm<OptimizableNeuralNetwork, Double>(brains, fit);

		ga.addIterationListener(new IterartionListener<OptimizableNeuralNetwork, Double>() {
			@Override
			public void update(GeneticAlgorithm<OptimizableNeuralNetwork, Double> ga) {
				OptimizableNeuralNetwork bestBrain = ga.getBest();
				Double fit = ga.fitness(bestBrain);
				System.out.println(ga.getIteration() + "\t" + fit);

				ga.clearCache();
			}
		});

		ga.setParentChromosomesSurviveCount(1);

		ga.iterate(iterationsCount);

		OptimizableNeuralNetwork bestBrain = ga.getBest();
		return bestBrain;
	}

	public static class TournamentListener implements AgentsEnvironmentListener {

		protected static final double minEatDistance = 5;

		protected static final double maxFishesDistance = 5;

		private Random random = new Random();

		private double score = 0;

		@Override
		public void notify(AgentsEnvironment env) {
			List<Food> eatenFood = this.getEatenFood(env);
			this.score += eatenFood.size();

			List<Fish> collidedFishes = this.getCollidedFishes(env);
			this.score -= collidedFishes.size() * 0.5;

			this.removeEatenAndCreateNewFood(env, eatenFood);
		}

		private List<Fish> getCollidedFishes(AgentsEnvironment env) {
			List<Fish> collidedFishes = new LinkedList<Fish>();

			List<Fish> allFishes = this.getFishes(env);
			int fishesCount = allFishes.size();

			for (int i = 0; i < (fishesCount - 1); i++) {
				Fish firstFish = allFishes.get(i);
				for (int j = i + 1; j < fishesCount; j++) {
					Fish secondFish = allFishes.get(j);
					double distanceToSecondFish = this.module(firstFish.getX() - secondFish.getX(), firstFish.getY() - secondFish.getY());
					if (distanceToSecondFish < maxFishesDistance) {
						collidedFishes.add(secondFish);
						// this.score -= 0.5;
					}
				}
			}
			return collidedFishes;
		}

		private List<Food> getEatenFood(AgentsEnvironment env) {
			List<Food> eatenFood = new LinkedList<Food>();

			F: for (Food food : this.getFood(env)) {
				for (Fish fish : this.getFishes(env)) {
					double distanceToFood = this.module(food.getX() - fish.getX(), food.getY() - fish.getY());
					if (distanceToFood < minEatDistance) {
						// this.score++;
						eatenFood.add(food);
						continue F;
					}
				}
			}
			return eatenFood;
		}

		private void removeEatenAndCreateNewFood(AgentsEnvironment env, List<Food> eatenFood) {
			for (Food food : eatenFood) {
				env.removeAgent(food);

				Food newFood = new Food(this.random.nextInt(env.getWidth()), this.random.nextInt(env.getHeight()));
				env.addAgent(newFood);
			}
		}

		private List<Food> getFood(AgentsEnvironment env) {
			// TODO use Guava
			List<Food> food = new ArrayList<Food>();
			for (Agent agent : env.getAgents()) {
				if (agent instanceof Food) {
					food.add((Food) agent);
				}
			}
			return food;
		}

		private List<Fish> getFishes(AgentsEnvironment env) {
			// TODO use Guava
			List<Fish> fishes = new ArrayList<Fish>();
			for (Agent agent : env.getAgents()) {
				if (agent instanceof Fish) {
					fishes.add((Fish) agent);
				}
			}
			return fishes;
		}

		public double getScore() {
			if (this.score < 0) {
				return 0;
			}
			return this.score;
		}

		protected double module(double vx1, double vy1) {
			return Math.sqrt((vx1 * vx1) + (vy1 * vy1));
		}
	}

}
