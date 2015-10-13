package template;

import com.sun.istack.internal.NotNull;
import logist.agent.Agent;
import logist.topology.Topology.City;


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

    public State(City from, City to, City home)
    {
        this.from = from;
        this.to = to;
        this.bestAction = home;

        bestAction = from;
        bestReward = pre_bestReward = 0;
    }

    @NotNull public City getBestAction()
    {
        return bestAction;
    }

    public void setBestAction(City bestAction)
    {
        this.bestAction = bestAction;
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

    public void setPre_bestReward(double pre_bestReward)
    {
        this.pre_bestReward = pre_bestReward;
    }

    @NotNull public double getBestReward()
    {
        return bestReward;
    }

    public void setBestReward(double bestReward)
    {
        this.bestReward = bestReward;
    }

    public boolean updateBestReward(double newBest, City newBestAction)
    {
        if (newBest > bestReward)
        {
            pre_bestReward = bestReward;
            bestReward = newBest;
            bestAction = newBestAction;

//            System.out.println("After:" + pre_bestReward + "; " + bestReward + "; " + bestAction.toString());

            return true;
        }

        return false;
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
