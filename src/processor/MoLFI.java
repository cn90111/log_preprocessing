package processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MoLFI extends PreprocessMethod
{
	HashMap<Integer, ArrayList<String>> contentGroup;
	HashMap<Integer, ArrayList<String>> templateGroup;
	ArrayList<String> templateList;

	int iter = 200;
	int population = 20;
	float crossoverRate = 0.7f;

	@Override
	protected ArrayList<String> getTemplate(String[] content)
	{
		contentGroup = preprocessing(content);

		templateGroup = nsga2(contentGroup);
		templateList = toArrayList(templateGroup);

		System.out.println("frequency:" + freqValue(templateGroup, contentGroup));
		System.out.println("special:" + specValue(templateGroup));

		return templateList;
	}

	@Override
	protected String transform(String[] content)
	{
		StringBuilder template = new StringBuilder(2048);
		ArrayList<String> group;
		int groupLength;

		for (String line : content)
		{
			String[] tokens = splitLog(line);
			groupLength = tokens.length;
			group = templateGroup.get(groupLength);

			for (String temp : group)
			{
				if (compareTemplate(temp, line))
				{
					template.append(temp);
					template.append("\n");
				}
			}
		}

		return template.toString();
	}

	private HashMap<Integer, ArrayList<String>> nsga2(HashMap<Integer, ArrayList<String>> content)
	{
		ArrayList<HashMap<Integer, ArrayList<String>>> chromosome = initialize(content);
		HashMap<Integer, ArrayList<String>> template;
		for (int j = 0; j < iter; j++)
		{
			chromosome = selection(chromosome, content);
			chromosome = crossover(chromosome);
			chromosome = mutation(chromosome, content);
		}
		template = getBestSolution(chromosome, content);

		return template;
	}

	private HashMap<Integer, ArrayList<String>> preprocessing(String[] content)
	{
		HashMap<Integer, ArrayList<String>> filterContent = new HashMap<Integer, ArrayList<String>>();
		ArrayList<String> group;
		int length;
		boolean overlap;
		for (String line : content)
		{
			String[] tokens = splitLog(line);
			length = tokens.length;
			overlap = false;

			if (!filterContent.containsKey(length))
			{
				filterContent.put(length, new ArrayList<String>());
			}
			group = filterContent.get(length);

			for (String temp : group)
			{
				if (line.equals(temp))
				{
					overlap = true;
					break;
				}
			}

			if (overlap == true)
			{
				continue;
			}

			for (int i = 0; i < length; i++)
			{
				if (isNumber(tokens[i]))
				{
					tokens[i] = "#spec#";
				}
			}
			group.add(combineTokens(tokens));
		}

		return filterContent;
	}

	private ArrayList<HashMap<Integer, ArrayList<String>>> initialize(HashMap<Integer, ArrayList<String>> content)
	{
		ArrayList<HashMap<Integer, ArrayList<String>>> chromosome = new ArrayList<HashMap<Integer, ArrayList<String>>>();
		HashMap<Integer, ArrayList<String>> templateGroup;
		ArrayList<String> template;

		for (int i = 0; i < population; i++)
		{
			templateGroup = new HashMap<Integer, ArrayList<String>>();

			for (Integer lengthKey : content.keySet())
			{
				// create group
				if (!templateGroup.containsKey(lengthKey))
				{
					templateGroup.put(lengthKey, new ArrayList<String>());
				}
				template = templateGroup.get(lengthKey);

				for (String line : content.get(lengthKey))
				{
					if (!compareTemplate(template, line))
					{
						template.add(changeConstantOfTemplate(line, detectConstantIndex(line)));
					}
				}
			}
			templateGroup = removeOverlap(templateGroup, content);
			chromosome.add(templateGroup);
		}
		return chromosome;
	}

	private ArrayList<HashMap<Integer, ArrayList<String>>> selection(
			ArrayList<HashMap<Integer, ArrayList<String>>> chromosome, HashMap<Integer, ArrayList<String>> content)
	{
		ArrayList<HashMap<Integer, ArrayList<String>>> newChromosome = new ArrayList<HashMap<Integer, ArrayList<String>>>();

		for (int i = 0; i < chromosome.size(); i++)
		{
			newChromosome.add(deepCopy(binaryTournament(chromosome, content)));
		}

		return newChromosome;
	}

	private ArrayList<HashMap<Integer, ArrayList<String>>> crossover(
			ArrayList<HashMap<Integer, ArrayList<String>>> chromosome)
	{
		HashMap<Integer, ArrayList<String>> solutionA;
		HashMap<Integer, ArrayList<String>> solutionB;
		ArrayList<String> temp;

		for (int i = 0; i < chromosome.size() / 2; i++)
		{
			solutionA = chromosome.get(i);
			solutionB = chromosome.get(chromosome.size() - 1 - i);

			if (Math.random() <= crossoverRate)
			{
				for (Integer templateLength : solutionA.keySet())
				{
					if (Math.random() > 0.5)
					{
						temp = (ArrayList<String>) solutionA.get(templateLength).clone();
						solutionA.put(templateLength, solutionB.get(templateLength));
						solutionB.put(templateLength, temp);
					}
				}
			}
		}
		return chromosome;
	}

	private ArrayList<HashMap<Integer, ArrayList<String>>> mutation(
			ArrayList<HashMap<Integer, ArrayList<String>>> chromosome, HashMap<Integer, ArrayList<String>> content)
	{
		float groupMutationRate;
		float templateMutationRate;

		HashMap<Integer, ArrayList<String>> solution;
		ArrayList<String> group;
		String template;

		int[] constantIndex;
		int[] variableIndex;

		for (int i = 0; i < chromosome.size(); i++)
		{
			solution = chromosome.get(i);
			groupMutationRate = 1.0f / solution.size();

			for (Integer templateLength : solution.keySet())
			{
				if (Math.random() <= groupMutationRate)
				{
					group = solution.get(templateLength);
					templateMutationRate = 1.0f / group.size();

					for (int j = 0; j < group.size(); j++)
					{
						if (Math.random() <= templateMutationRate)
						{
							template = group.get(j);

							constantIndex = detectConstantIndex(template);
							variableIndex = detectVariableIndex(template);

							if ((Math.random() > 0 || variableIndex.length == 0) && constantIndex.length != 0)
							{
								template = changeConstantOfTemplate(template, constantIndex);
							}
							else
							{
								template = changeVariableOfTemplate(template, variableIndex,
										getMatchMessage(template, content));
							}
							group.remove(j);
							group.add(j, template);
							solution = removeOverlap(solution, content);
						}
					}
				}
			}
		}

		return chromosome;
	}

	private HashMap<Integer, ArrayList<String>> binaryTournament(
			ArrayList<HashMap<Integer, ArrayList<String>>> chromosome, HashMap<Integer, ArrayList<String>> content)
	{
		int randomA;
		int randomB;
		HashMap<Integer, ArrayList<String>> solutionA;
		HashMap<Integer, ArrayList<String>> solutionB;

		randomA = (int) (Math.random() * chromosome.size());
		randomB = (int) (Math.random() * chromosome.size());
		solutionA = chromosome.get(randomA);
		solutionB = chromosome.get(randomB);

		if (aDominateB(solutionA, solutionB, content))
		{
			return solutionA;
		}
		else if (!aDominateB(solutionB, solutionA, content))
		{
			// not dominate
			if (solutionArea(chromosome, randomA, content) > solutionArea(chromosome, randomB, content))
			{
				return solutionA;
			}
		}
		return solutionB;
	}

	private float freqValue(HashMap<Integer, ArrayList<String>> solution, HashMap<Integer, ArrayList<String>> content)
	{
		int m = 0; // logCount
		int n = 0; // templateCount
		float sum = 0; // freq
		ArrayList<String> template;
		for (Integer contentLength : content.keySet())
		{
			template = solution.get(contentLength);
			for (String line : content.get(contentLength))
			{
				if (compareTemplate(template, line))
				{
					sum++;
				}
				m++;
			}
			n = n + solution.get(contentLength).size();
		}
		sum = (float) sum / (n * m);

		return sum;
	}

	private float specValue(HashMap<Integer, ArrayList<String>> solution)
	{
		float sum = 0; // spec
		int n = 0;
		int fixed = 0;
		for (ArrayList<String> temp : solution.values())
		{
			n = n + temp.size();
		}
		for (Integer templateLength : solution.keySet())
		{
			for (String template : solution.get(templateLength))
			{
				fixed = detectConstantIndex(template).length;
				sum = sum + (float) fixed / (n * templateLength);
			}
		}

		return sum;
	}

	private float solutionArea(ArrayList<HashMap<Integer, ArrayList<String>>> chromosome, int solutionIndex,
			HashMap<Integer, ArrayList<String>> content)
	{
		HashMap<Integer, ArrayList<String>> solution = chromosome.get(solutionIndex);
		HashMap<Integer, ArrayList<String>> nowSolution;
		float solutionFreq = freqValue(solution, content);
		float solutionSpec = specValue(solution);
		float leftFreq = -1;
		float leftSpec = -1;
		float rightFreq = -1;
		float rightSpec = -1;
		float nowFreq = -1;
		float nowSpec = -1;
		for (int i = 0; i < chromosome.size(); i++)
		{
			if (i != solutionIndex)
			{
				nowSolution = chromosome.get(i);
				nowFreq = freqValue(nowSolution, content);
				nowSpec = specValue(nowSolution);

				if (aDominateB(nowSolution, solution, content) || aDominateB(solution, nowSolution, content))
				{
					continue;
				}

				if (nowFreq > solutionFreq)
				{
					if ((nowFreq <= leftFreq && nowSpec >= leftSpec) || leftFreq == -1)
					{
						leftFreq = nowFreq;
						leftSpec = nowSpec;
					}
				}

				if (nowSpec > solutionSpec)
				{
					if ((nowFreq >= rightFreq && nowSpec <= rightSpec) || rightSpec == -1)
					{
						rightFreq = nowFreq;
						rightSpec = nowSpec;
					}
				}
			}
		}
		if (leftFreq == -1 || rightSpec == -1)
		{
			return Float.MAX_VALUE;
		}
		return (rightSpec - leftSpec) * (leftFreq - rightFreq);
	}

	private boolean aDominateB(HashMap<Integer, ArrayList<String>> solutionA,
			HashMap<Integer, ArrayList<String>> solutionB, HashMap<Integer, ArrayList<String>> content)
	{
		if (freqValue(solutionA, content) >= freqValue(solutionB, content))
		{
			if (specValue(solutionA) >= specValue(solutionB))
			{
				return true;
			}
		}
		return false;
	}

	private ArrayList<String> toArrayList(HashMap<Integer, ArrayList<String>> mapTemplate)
	{
		ArrayList<String> arrayListTemplate = new ArrayList<String>();

		for (ArrayList<String> temp : mapTemplate.values())
		{
			for (String template : temp)
			{
				arrayListTemplate.add(template);
			}
		}
		return arrayListTemplate;
	}

	private int[] convertIntegers(List<Integer> intList)
	{
		int[] intArray = new int[intList.size()];
		Iterator<Integer> iterator = intList.iterator();
		for (int i = 0; i < intArray.length; i++)
		{
			intArray[i] = iterator.next().intValue();
		}
		return intArray;
	}

	private int[] detectConstantIndex(String template)
	{
		ArrayList<Integer> indexList = new ArrayList<Integer>();

		String[] tokens = template.split(" ");

		for (int i = 0; i < tokens.length; i++)
		{
			if (!tokens[i].equals("*") && !tokens[i].equals("#spec#"))
			{
				indexList.add(i);
			}
		}
		return convertIntegers(indexList);
	}

	private int[] detectVariableIndex(String template)
	{
		ArrayList<Integer> indexList = new ArrayList<Integer>();

		String[] tokens = template.split(" ");

		for (int i = 0; i < tokens.length; i++)
		{
			if (tokens[i].equals("*"))
			{
				indexList.add(i);
			}
		}
		return convertIntegers(indexList);
	}

	private ArrayList<String> getMatchMessage(String template, HashMap<Integer, ArrayList<String>> content)
	{
		ArrayList<String> matchMessage = new ArrayList<String>();
		for (ArrayList<String> temp : content.values())
		{
			for (String log : temp)
			{
				if (compareTemplate(template, log))
				{
					matchMessage.add(log);
				}
			}
		}
		return matchMessage;
	}

	private String changeConstantOfTemplate(String template, int[] constantIndex)
	{
		int randomIndex = (int) (Math.random() * constantIndex.length);
		String[] tokens = template.split(" ");
		tokens[constantIndex[randomIndex]] = "*";

		return combineTokens(tokens);
	}

	private String changeVariableOfTemplate(String template, int[] variableIndex, ArrayList<String> matchMessage)
	{
		int randomIndex = (int) (Math.random() * variableIndex.length);
		int randomMessage = (int) (Math.random() * matchMessage.size());
		String[] messageToken = matchMessage.get(randomMessage).split(" ");

		while (messageToken[variableIndex[randomIndex]].equals("#spec#"))
		{
			randomMessage = (int) (Math.random() * matchMessage.size());
			messageToken = matchMessage.get(randomMessage).split(" ");
		}

		String[] tokens = template.split(" ");
		tokens[variableIndex[randomIndex]] = messageToken[variableIndex[randomIndex]];

		return combineTokens(tokens);
	}

	HashMap<Integer, ArrayList<String>> getBestSolution(ArrayList<HashMap<Integer, ArrayList<String>>> chromosome,
			HashMap<Integer, ArrayList<String>> content)
	{
		HashMap<Integer, ArrayList<String>> solution;
		HashMap<Integer, ArrayList<String>> bestSolution = null;
		float objectiveValue;
		float bestObjectiveValue = -1;

		for (int i = 0; i < chromosome.size(); i++)
		{
			solution = chromosome.get(i);
			objectiveValue = freqValue(solution, content) * specValue(solution);

			if (bestSolution == null)
			{
				bestSolution = solution;
				bestObjectiveValue = objectiveValue;
			}

			if (objectiveValue > bestObjectiveValue)
			{
				bestSolution = solution;
				bestObjectiveValue = objectiveValue;
			}
		}

		return bestSolution;
	}

	private String combineTokens(String[] tokens)
	{
		StringBuilder combineString = new StringBuilder(128);

		for (String token : tokens)
		{
			combineString.append(token);
			combineString.append(" ");
		}

		return combineString.toString();
	}

	private boolean matchRule(HashMap<Integer, ArrayList<String>> template,
			HashMap<Integer, ArrayList<String>> contentGroup)
	{
		int matchCount;

		for (Integer contentLength : contentGroup.keySet())
		{
			for (String line : contentGroup.get(contentLength))
			{
				matchCount = 0;

				for (String temp : template.get(contentLength))
				{
					if (compareTemplate(temp, line))
					{
						matchCount++;
					}
				}

				if (matchCount == 0 || matchCount > 1)
				{
					return false;
				}
			}
		}
		return true;
	}

	private HashMap<Integer, ArrayList<String>> removeOverlap(HashMap<Integer, ArrayList<String>> templateGroup,
			HashMap<Integer, ArrayList<String>> content)
	{
		ArrayList<String> template;
		ArrayList<Integer> overlapIndex = new ArrayList<Integer>();

		int saveIndex;
		int offset;

		while (!matchRule(templateGroup, content))
		{
			for (Integer contentLength : content.keySet())
			{
				template = templateGroup.get(contentLength);

				for (String line : content.get(contentLength))
				{
					overlapIndex.clear();

					// find overlap
					for (int i = 0; i < template.size(); i++)
					{
						if (compareTemplate(template.get(i), line))
						{
							overlapIndex.add(i);
						}
					}

					// delete overlap
					offset = 0;
					saveIndex = (int) (Math.random() * overlapIndex.size());
					for (int i = 0; i < overlapIndex.size(); i++)
					{
						if (i != saveIndex)
						{
							template.remove(overlapIndex.get(i) - offset);
							offset++;
						}
					}
				}

				// get new template
				for (String line : content.get(contentLength))
				{
					if (!compareTemplate(template, line))
					{
						template.add(changeConstantOfTemplate(line, detectConstantIndex(line)));
					}
				}
			}
		}
		return templateGroup;
	}

	private HashMap<Integer, ArrayList<String>> deepCopy(HashMap<Integer, ArrayList<String>> solution)
	{
		HashMap<Integer, ArrayList<String>> copy = new HashMap<Integer, ArrayList<String>>();

		for (Integer templateLength : solution.keySet())
		{
			copy.put(templateLength, (ArrayList<String>) solution.get(templateLength).clone());
		}

		return copy;
	}
}
