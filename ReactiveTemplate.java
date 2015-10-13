package template;

import java.util.Random;

import com.sun.istack.internal.NotNull;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.LinkedList;
import java.util.List;

public class ReactiveTemplate implements ReactiveBehavior
{

    private final LinkedList<State> stateList = new LinkedList<>(); // All possible states
    private List<City> cityList;                                    // All reachable cities
    private City tempBestAction;                                    // temp best action corresponding to an iteration of maxQ
    private Random random;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent)
    {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        Double discount = agent.readProperty("discount-factor", Double.class,
                0.95);

        this.initState(topology, agent);
        this.valueIteration(td, agent, discount);

        System.out.println("All possible states: ");
        for (State s : stateList)
        {
            System.out.println(s);
        }
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask)
    {
        Action action = null;

        State currentState;

        if (availableTask == null)
        {
            currentState = new State(vehicle.getCurrentCity(), vehicle.getCurrentCity(), vehicle.homeCity());

            State completeState = lookUpState(currentState);
            if (completeState != null)
            {
                action = new Move(completeState.getBestAction());

            } else {System.out.println("ERROR: No corresponding state found!");}
        } else
        {
            currentState = new State(vehicle.getCurrentCity(), availableTask.deliveryCity, vehicle.homeCity());

            State completeState = lookUpState(currentState);

            if (completeState != null)
            {
                City bestAction = completeState.getBestAction();

                if (availableTask.deliveryCity.equals(bestAction)) {
                    action = new Pickup(availableTask);
                }
                else
                {
                    action = new Move(completeState.getBestAction());
                }
            } else {System.out.println("ERROR: No corresponding state found!");}

        }
        return action;
    }

    /**
     * initialize all possible states and reachable cities.
     *
     * @param t topology
     */
    private void initState(Topology t, Agent agent)
    {
        this.random = new Random();
        cityList = t.cities();
        for (City from : t)
        {
            for (City to : t)
            {
                stateList.add(new State(from, to, agent.vehicles().get(0).homeCity()));
            }
        }
    }

    /**
     * Markov decision process, Compute best policy.
     *
     * @param td TaskDistribution
     * @param agent agent which travels and deliver tasks
     * @param discountFactor discount the future state reward
     */
    private void valueIteration(TaskDistribution td, Agent agent, double discountFactor)
    {
        City currentCity;
        City taskDest;
        List<City> neighbourList;

        do
        {
            for (State s : stateList)
            {
                currentCity = s.getFrom();
                taskDest = s.getTo();
                neighbourList = s.getFrom().neighbors();

                if (!currentCity.hasNeighbor(taskDest))
                {
                    List<City> newList = new LinkedList<>(neighbourList);
                    newList.add(taskDest);
                }

                double maxQ;

                maxQ = computeMaxQ(currentCity, taskDest, neighbourList, td, agent, discountFactor);

                s.updateBestReward(maxQ, tempBestAction);
            }

        } while (!converge(0.0001));
    }

    /**
     * Compute maxQ for a given state.
     *
     * @param currentCity current city
     * @param taskDestCity task destination city
     * @param reachableCity reachable city list after legal moves performed by the agent
     * @param td task distrivution
     * @param agent agent
     * @param discountFactor sicount factor of the future reward
     *
     * @return Q(s)
     */
    private double computeMaxQ(City currentCity, City taskDestCity, List<City> reachableCity, TaskDistribution td,
                               Agent agent, double discountFactor)
    {
        double maxQ = 0;

        for (City nextCity : reachableCity)
        {
            double sum = 0;
            double cost;
            for (City nextPossibleTaskDest : cityList)
            {
                State futureState = new State(nextCity, nextPossibleTaskDest, agent.vehicles().get(0).homeCity());
                if (!nextPossibleTaskDest.equals(nextCity))
                {
                    sum += discountFactor * td.probability(nextCity, nextPossibleTaskDest) * getBestValue(futureState);
                } else
                {
                    sum += discountFactor * td.probability(nextCity, null) * getBestValue(futureState);
                }
            }
            //Add reward if the package is taken.
            if (taskDestCity.equals(nextCity))
            {
                sum += td.reward(currentCity, taskDestCity);
            }

            //Subtract the cost of the trip
            List<Vehicle> vehicles = agent.vehicles();
            cost = currentCity.distanceTo(nextCity) * vehicles.get(0).costPerKm();
            sum -= cost;

            if (sum > maxQ)
            {
                maxQ = sum;
                tempBestAction = nextCity;
            }
        }
        return maxQ;
    }

    /**
     * Get the bestValue V(s) stored in the stateList
     *
     * @param s State
     *
     * @return V(s) of a given state
     */
    private double getBestValue(State s)
    {
        for (State state : stateList)
        {
            if (state.equals(s))
            {
                return state.getBestReward();
            }
        }
        return 0;
    }


    /**
     * Check whether the results after n iteration is good enough, difference between the previous iteration and current
     * iteration is smaller than epsilon
     *
     * @param epsilon max difference between pre maxQ and current maxQ after n iterations
     *
     * @return true if diff is smaller than epsilon
     */
    private boolean converge(double epsilon)
    {
        double maxDiff = 0;
        double diff;
        for (State s : stateList)
        {
            diff = Math.abs(s.getBestReward() - s.getPre_bestReward());
            if (diff > maxDiff)
            {
                maxDiff = diff;
            }
        }

        return maxDiff < epsilon;
    }

    /**
     * Find the corresponding state in the precomputed stateList
     *
     * @param DesiredState state to be searched
     *
     * @return complete state
     */
    @NotNull private State lookUpState(State DesiredState)
    {
        for (State state : stateList)
        {
            if (DesiredState.equals(state))
            {
                return state;
            }
        }
        return null;
    }
}
