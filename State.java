package template;

import com.sun.istack.internal.NotNull;
import logist.topology.Topology.City;

import java.util.Random;

/**
 * reactive Created by samsara on 09/10/2015.
 */
public class State
{
    private City from;
    private City to;
    private City bestAction;
    private double bestReward;
    private double pre_bestReward;


    public State(City from, City to)
    {
        this.from = from;
        this.to = to;

        Random random = new Random();

        this.bestAction = from.randomNeighbor(random);
        this.bestReward = - Double.MAX_VALUE;
        this.pre_bestReward = - Double.MAX_VALUE;
    }

    @NotNull public City getBestAction()
    {
        return bestAction;
    }

    public City getFrom()
    {
        return from;
    }

    public City getTo()
    {
        return to;
    }

    public double getPre_bestReward()
    {
        return pre_bestReward;
    }


    @NotNull public double getBestReward()
    {
        return bestReward;
    }

    public void updateBestReward(double newBest, City newBestAction)
    {
        pre_bestReward = bestReward;
        bestReward = newBest;
        bestAction = newBestAction;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        return to.equals(state.to) && from.equals(state.from);

    }

    @Override
    public int hashCode()
    {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "State{" +
                "from=" + from +
                ", to=" + to +
                ", bestAction=" + bestAction +
                ", bestReward=" + bestReward +
                ", pre_bestReward=" + pre_bestReward +
                '}';
    }

}
