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

    private int counter = 0;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent)
    {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        Double discount = agent.readProperty("discount-factor", Double.class,
                0.95);

        this.random = new Random();
        this.pPickup = discount;

        this.initState(topology);
        this.valueIteration(td, agent, discount);

        //Print out checks
        System.out.println("counter = " + counter);

        for (State s : stateList)
        {
            System.out.println(s);
        }


    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask)
    {
        Action action;

        State currentState;

        if (availableTask == null)
        {
            currentState = new State(vehicle.getCurrentCity(), vehicle.getCurrentCity());

            State completeState;
            completeState = lookUpState (currentState);

            action = new Move(completeState.getBestAction());

            //Printout check!
            System.out.println("No task:\n" + currentState.toString() + "\n" + action.toString());
        }
        else
        {
            currentState = new State(vehicle.getCurrentCity(), availableTask.deliveryCity);

            State completeState = lookUpState (currentState);

            assert completeState != null;
            if (availableTask.deliveryCity.equals(completeState.getBestAction()))
            {
                action = new Pickup(availableTask);
            }
            else
            {
                action = new Move(completeState.getBestAction());
            }

            //Printout check!
            System.out.println("Task available:\n" + currentState.toString() + "\n" + action.toString());
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

            counter++;
        } while (!converge(1));
    }

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
                State futureState = new State(nextCity, nextPossibleTaskDest);
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

        return (maxDiff < epsilon);
    }

    private State lookUpState (State DesiredState){
        for (State state : stateList)
        {
            if (DesiredState.equals(state)) {
                return state;
            }
        }
        System.out.println("ERROR: No matching state found!!!");
        return null;
    }
}
