package template;

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
import java.util.Random;

public class ReactiveTemplate implements ReactiveBehavior
{

    private Random random;
    private double pPickup;

    private LinkedList<State> stateList = new LinkedList<>();
    private List<City> cityList;
    private City tempBestAction;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent)
    {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        Double discount = agent.readProperty("discount-factor", Double.class,
                0.95);

        this.random = new Random();
        this.pPickup = discount;
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask)
    {
        Action action;

        if (availableTask == null || random.nextDouble() > pPickup)
        {
            City currentCity = vehicle.getCurrentCity();
            action = new Move(currentCity.randomNeighbor(random));
        } else
        {
            action = new Pickup(availableTask);
        }
        return action;
    }

    private void initState(Topology t)
    {
        cityList = t.cities();
        for (City from : t)
        {
            for (City to : t)
            {
                stateList.add(new State(from, to));
            }
        }
    }

    private void valueIteration(TaskDistribution td, double discountFactor)
    {
        City currentCity;
        City taskDest;
        List<City> neighbourList;

        double reward;
        double q;
        do
        {
            for (State s : stateList)
            {
                currentCity = s.getFrom();
                taskDest = s.getTo();
                neighbourList = s.getFrom().neighbors();

                if (!currentCity.hasNeighbor(taskDest))
                {
                    neighbourList.add(taskDest);
                }

                double maxQ;

                maxQ = computeMaxQ(currentCity, taskDest, neighbourList, td, discountFactor);

                s.updateBestReward(maxQ, tempBestAction);
            }
        } while (!converge(0.01));
    }

    private double computeMaxQ(City currentCity, City taskDestCity, List<City> reachableCity, TaskDistribution td,
                               double discountFactor)
    {
        double maxQ = 0;

        for (City nextCity : reachableCity)
        {
            double sum = 0;
            for (City nextPossibleTaskDest : cityList)
            {
                State futureState = new State(nextCity, nextPossibleTaskDest);
                if (!nextPossibleTaskDest.equals(nextCity))
                {
                    sum += discountFactor * td.probability(nextCity, nextPossibleTaskDest) * getBestValue(futureState);
                } else
                {
                    sum += discountFactor * td.probability(nextCity, null) * getBestValue(futureState);
                }
            }
            if (taskDestCity.equals(nextCity))
            {
                sum += td.reward(currentCity, taskDestCity);
            }

            if (sum > maxQ)
            {
                maxQ = sum;
                tempBestAction = nextCity;
            }
        }
        return maxQ;
    }

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


    private boolean converge(double epsilon)
    {
        for (State s : stateList)
        {
            if (s.getBestReward() - s.getPre_bestReward() < epsilon)
            {
                return true;
            }
        }
        return false;
    }
}
