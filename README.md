# Neural network and genetic algorithm demo written in Java

Simple demo on evolving a car with genetic algorithm to finish a randomly drawn race track.
Created with LibGDX and Box2D. 
This repository only contains the core project - no assets and no Android, iOS, Desktop or Web launcher projects.

Neural network has one hidden layer with 40 neurons and the network is fed with seven ray cast inputs. 
There are four outputs: steer right, steer left, accelerate and brake.

Genetic algorithm has a population size of 15 with 645 genes on each children.
It usually takes about 15-20 generations for the first one to finish the track. By further improving the fitness function this should be even less. Currently it is still pretty primitive.


