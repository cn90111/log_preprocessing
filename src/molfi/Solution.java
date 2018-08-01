package molfi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import processor.PreprocessMethod;

public class Solution
{
	private HashMap<Integer, ArrayList<Template>> solution;
	private HashMap<Integer, ArrayList<String>> logContent;
	private float freq;
	private float spec;

	public Solution(HashMap<Integer, ArrayList<String>> logContent)
	{
		solution = new HashMap<Integer, ArrayList<Template>>();
		this.logContent = logContent;
	}

	public Solution(Solution other)
	{
		solution = new HashMap<Integer, ArrayList<Template>>();
		for (Integer logLength : other.keySet())
		{
			solution.put(logLength, new ArrayList<Template>(other.get(logLength)));
		}
		logContent = other.getLogContent();
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

	public HashMap<Integer, ArrayList<String>> getLogContent()
	{
		return logContent;
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
		int m = 0; // logCount
		int n = 0; // templateCount
		float sum = 0; // freq
		ArrayList<Template> template;
		for (Integer contentLength : logContent.keySet())
		{
			template = solution.get(contentLength);
			for (String line : logContent.get(contentLength))
			{
				if (PreprocessMethod.compareTemplate(template, line))
				{
					sum++;
				}
				m++;
			}
			n = n + solution.get(contentLength).size();
		}

		freq = (float) sum / (n * m);
	}

	private void specValue()
	{
		float sum = 0; // spec
		int n = 0;
		int fixed = 0;
		for (ArrayList<Template> temp : solution.values())
		{
			n = n + temp.size();
		}
		for (Integer templateLength : solution.keySet())
		{
			for (Template template : solution.get(templateLength))
			{
				fixed = template.getConstantIndex().length;
				sum = sum + (float) fixed / (n * templateLength);
			}
		}

		spec = sum;
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
