package molfi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class Solution
{
	private HashMap<Integer, ArrayList<Template>> solution;
	private float freq;
	private float spec;

	public Solution()
	{
		solution = new HashMap<Integer, ArrayList<Template>>();
	}

	public Solution(Solution other)
	{
		solution = new HashMap<Integer, ArrayList<Template>>();
		ArrayList<Template> temp;
		for (Integer logLength : other.keySet())
		{
			temp = new ArrayList<Template>();
			for (Template template : other.get(logLength))
			{
				temp.add(new Template(template));
			}
			solution.put(logLength, temp);
		}
		freq = other.getFreq();
		spec = other.getSpec();
	}

	public void update()
	{
		freqValue();
		specValue();
	}

	public void remove(Integer key)
	{
		solution.remove(key);
	}

	public void put(Integer key, ArrayList<Template> value)
	{
		solution.put(key, value);
	}

	public ArrayList<Template> get(Integer key)
	{
		return solution.get(key);
	}

	public Collection<ArrayList<Template>> values()
	{
		return solution.values();
	}

	public int size()
	{
		return solution.size();
	}

	public Set<Integer> keySet()
	{
		return solution.keySet();
	}

	public boolean containsKey(Integer key)
	{
		return solution.containsKey(key);
	}

	public float getFreq()
	{
		return freq;
	}

	public float getSpec()
	{
		return spec;
	}

	private void freqValue()
	{
		int n = 0; // templateCount
		float sum = 0; // freq
		ArrayList<Template> group;
		for (Integer templateLength : solution.keySet())
		{
			group = solution.get(templateLength);

			for (Template template : group)
			{
				sum = sum + template.getFreq();
			}

			n = n + group.size();
		}

		freq = sum / n;
	}

	private void specValue()
	{
		int n = 0; // templateCount
		float sum = 0; // spec
		ArrayList<Template> group;
		for (Integer templateLength : solution.keySet())
		{
			group = solution.get(templateLength);
			for (Template template : group)
			{
				sum = sum + template.getSpec();
			}
			n = n + group.size();
		}
		spec = sum / n;
	}

	public boolean isDominate(Solution other)
	{
		if (freq >= other.getFreq())
		{
			if (spec >= other.getSpec())
			{
				return true;
			}
		}
		return false;
	}
}
