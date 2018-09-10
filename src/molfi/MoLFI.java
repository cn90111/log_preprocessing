package molfi;

import java.util.ArrayList;
import java.util.HashMap;

import processor.PreprocessMethod;

public class MoLFI extends PreprocessMethod
{
	HashMap<Integer, ArrayList<String>> logContent;
	Solution solution;
	ArrayList<String> templateList;
	int contentSize;

	float initStarRate = 0.3f;

	int iter = 200;
	int population = 20;
	float crossoverRate = 0.7f;

	@Override
	protected ArrayList<String> getTemplate(String[] content)
	{
		logContent = preprocessing(content);
		contentSize = getContentSize(logContent);

		solution = nsga2(logContent, contentSize);
		templateList = toArrayList(solution);

		System.out.println("frequency:" + solution.getFreq());
		System.out.println("special:" + solution.getSpec());

		return templateList;
	}

	@Override
	protected void setTemplate(String[] template)
	{
		solution = new Solution();
		
		ArrayList<Template> group;
		int length; 
		
		for(String line : template)
		{
			length = line.split(" ").length;
			
			if(!solution.containsKey(length))
			{
				group = new ArrayList<Template>();
				solution.put(length, group);
			}
			
			group = solution.get(length);
			group.add(new Template(line));
		}
	}

	@Override
	protected String transform(String line)
	{
		ArrayList<Template> group;
		int groupLength;

		String[] tokens = splitLog(line);
		groupLength = tokens.length;
		group = solution.get(groupLength);
		
		if(group != null)
		{
			for (Template temp : group)
			{
				if (temp.compareTemplate(line))
				{
					return temp.getContent();
				}
			}
		}
		
		return line;
	}

	private Solution nsga2(HashMap<Integer, ArrayList<String>> content, int contentSize)
	{
		ArrayList<Solution> chromosome = initialize(content);
		Solution solution;
		for (int i = 0; i < iter; i++)
		{
			System.out.print("iter:");
			System.out.println(i);

			chromosome = selection(chromosome, content);
			chromosome = crossover(chromosome);
			chromosome = mutation(chromosome, content, contentSize);
		}
		solution = getBestSolution(chromosome);
		solution = postProcessing(solution);
		solution.update();
		return solution;
	}

	private int getContentSize(HashMap<Integer, ArrayList<String>> logContent)
	{
		int contentSize = 0;
		for (ArrayList<String> group : logContent.values())
		{
			contentSize = contentSize + group.size();
		}
		return contentSize;
	}

	private HashMap<Integer, ArrayList<String>> preprocessing(String[] content)
	{
		HashMap<Integer, ArrayList<String>> filterContent = replaceVariable(content);
		filterContent = deleteOverlap(filterContent);
		return filterContent;
	}

	private HashMap<Integer, ArrayList<String>> replaceVariable(String[] content)
	{
		HashMap<Integer, ArrayList<String>> filterContent = new HashMap<Integer, ArrayList<String>>();
		ArrayList<String> group;
		int length;

		for (String line : content)
		{
			String[] tokens = splitLog(line);
			length = tokens.length;

			if (!filterContent.containsKey(length))
			{
				filterContent.put(length, new ArrayList<String>());
			}
			group = filterContent.get(length);

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

	private HashMap<Integer, ArrayList<String>> deleteOverlap(HashMap<Integer, ArrayList<String>> content)
	{
		HashMap<Integer, ArrayList<String>> simpleMap = new HashMap<Integer, ArrayList<String>>();
		ArrayList<String> group;
		boolean overlap;

		for (Integer logLength : content.keySet())
		{
			if (!simpleMap.containsKey(logLength))
			{
				simpleMap.put(logLength, new ArrayList<String>());
			}
			group = simpleMap.get(logLength);

			for (String line : content.get(logLength))
			{
				overlap = false;
				for (String temp : group)
				{
					if (temp.equals(line))
					{
						overlap = true;
						break;
					}
				}
				if (overlap == false)
				{
					group.add(line);
				}
			}
		}
		return simpleMap;
	}

	private ArrayList<Solution> initialize(HashMap<Integer, ArrayList<String>> content)
	{
		ArrayList<Solution> chromosome = new ArrayList<Solution>();
		Solution solution;
		ArrayList<Template> group;

		for (int i = 0; i < population; i++)
		{
			solution = new Solution();

			for (Integer logLength : content.keySet())
			{
				group = new ArrayList<Template>();

				for (String line : content.get(logLength))
				{
					if (!compareTemplate(group, line))
					{
						Template template = new Template(line, content, contentSize);

						for (int j = 0; j < template.getConstantIndex().length * initStarRate; j++)
						{
							template.changeConstantOfTemplate();
						}

						group.add(template);
					}
				}
				solution.put(logLength, group);
			}
			solution = removeOverlap(solution, content, contentSize);
			solution.update();
			chromosome.add(solution);
		}
		return chromosome;
	}

	private ArrayList<Solution> selection(ArrayList<Solution> chromosome, HashMap<Integer, ArrayList<String>> content)
	{
		ArrayList<Solution> newChromosome = new ArrayList<Solution>();

		for (int i = 0; i < chromosome.size(); i++)
		{
			newChromosome.add(new Solution(binaryTournament(chromosome)));
		}

		return newChromosome;
	}

	private ArrayList<Solution> crossover(ArrayList<Solution> chromosome)
	{
		Solution solutionA;
		Solution solutionB;
		ArrayList<Template> temp;

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
						temp = solutionA.get(templateLength);
						solutionA.put(templateLength, solutionB.get(templateLength));
						solutionB.put(templateLength, temp);
					}
				}
				solutionA.update();
				solutionB.update();
			}
		}
		return chromosome;
	}

	private ArrayList<Solution> mutation(ArrayList<Solution> chromosome, HashMap<Integer, ArrayList<String>> content,
			int contentSize)
	{
		float groupMutationRate;
		float templateMutationRate;

		Solution solution;
		ArrayList<Template> group;
		Template template;

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

							if (Math.random() > 0.5 || template.getVariableIndex().length == 0)
							{
								template.changeConstantOfTemplate();
								template.fixAllStarTemplate();
							}
							else
							{
								template.changeVariableOfTemplate();
							}
							group = removeOverlap(group, content, contentSize, templateLength);
							solution.update();
						}
					}
				}
			}
		}
		return chromosome;
	}

	private Solution postProcessing(Solution solution)
	{
		ArrayList<Template> group;
		for (Integer keyLength : solution.keySet())
		{
			group = solution.get(keyLength);
			for (Template template : group)
			{
				template.postProcessing();
			}
		}
		return solution;
	}

	private Solution binaryTournament(ArrayList<Solution> chromosome)
	{
		int randomA;
		int randomB;
		Solution solutionA;
		Solution solutionB;

		randomA = (int) (Math.random() * chromosome.size());
		randomB = (int) (Math.random() * chromosome.size());
		solutionA = chromosome.get(randomA);
		solutionB = chromosome.get(randomB);

		if (solutionA.isDominate(solutionB))
		{
			return solutionA;
		}
		else if (!solutionB.isDominate(solutionA))
		{
			// not dominate
			if (solutionArea(chromosome, randomA) > solutionArea(chromosome, randomB))
			{
				return solutionA;
			}
		}
		return solutionB;
	}

	private float solutionArea(ArrayList<Solution> chromosome, int solutionIndex)
	{
		Solution solution = chromosome.get(solutionIndex);
		Solution nowSolution;
		float solutionFreq = solution.getFreq();
		float solutionSpec = solution.getSpec();
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
				nowFreq = nowSolution.getFreq();
				nowSpec = nowSolution.getSpec();

				if (nowSolution.isDominate(solution) || solution.isDominate(nowSolution))
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

	private ArrayList<String> toArrayList(Solution mapTemplate)
	{
		ArrayList<String> arrayListTemplate = new ArrayList<String>();

		for (ArrayList<Template> temp : mapTemplate.values())
		{
			for (Template template : temp)
			{
				arrayListTemplate.add(template.getContent());
			}
		}
		return arrayListTemplate;
	}

	private Solution getBestSolution(ArrayList<Solution> chromosome)
	{
		Solution solution;
		Solution bestSolution = null;
		float objectiveValue;
		float bestObjectiveValue = -1;

		for (int i = 0; i < chromosome.size(); i++)
		{
			solution = chromosome.get(i);
			objectiveValue = solution.getFreq() * solution.getSpec();

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
	
	private boolean matchRule(ArrayList<Template> group, HashMap<Integer, ArrayList<String>> contentGroup,
			int checkLength)
	{
		int matchCount;

		for (String line : contentGroup.get(checkLength))
		{
			matchCount = 0;

			for (Template temp : group)
			{
				if (temp.compareTemplate(line))
				{
					matchCount++;
				}

				if (matchCount > 1)
				{
					return false;
				}
			}
			if (matchCount == 0)
			{
				return false;
			}
		}
		return true;
	}

	private Solution removeOverlap(Solution solution, HashMap<Integer, ArrayList<String>> content, int contentSize)
	{
		ArrayList<Template> group;

		for (Integer templateLength : solution.keySet())
		{
			group = solution.get(templateLength);
			group = removeOverlap(group, content, contentSize, templateLength);
		}

		return solution;
	}

	private ArrayList<Template> removeOverlap(ArrayList<Template> group, HashMap<Integer, ArrayList<String>> content,
			int contentSize, int checkLength)
	{
		ArrayList<Integer> overlapIndex = new ArrayList<Integer>();

		int saveIndex;
		int offset;

		while (!matchRule(group, content, checkLength))
		{
			for (String line : content.get(checkLength))
			{
				overlapIndex.clear();

				// find overlap
				for (int i = 0; i < group.size(); i++)
				{
					if (group.get(i).compareTemplate(line))
					{
						overlapIndex.add(i);
					}
				}

				// delete overlap
				if (overlapIndex.size() > 1)
				{
					offset = 0;
					saveIndex = selectSaveIndex(group, overlapIndex);
					for (int i = 0; i < overlapIndex.size(); i++)
					{
						if (i != saveIndex)
						{
							group.remove(overlapIndex.get(i) - offset);
							offset++;
						}
					}
				}
			}

			// get new template
			for (String line : content.get(checkLength))
			{
				if (!compareTemplate(group, line))
				{
					Template template = new Template(line, content, contentSize);
					template.changeConstantOfTemplate();
					group.add(template);
				}
			}
		}
		return group;
	}

	private int selectSaveIndex(ArrayList<Template> group, ArrayList<Integer> overlapIndex)
	{
		Template template;
		boolean isSameValue = true;

		template = group.get(overlapIndex.get(0));
		int saveIndex = 0;
		float bestValue = template.getFreq() * template.getSpec();
		float nowValue;

		for (int i = 1; i < overlapIndex.size(); i++)
		{
			template = group.get(overlapIndex.get(i));
			nowValue = template.getFreq() * template.getSpec();
			if (nowValue > bestValue)
			{
				bestValue = nowValue;
				saveIndex = i;
				isSameValue = false;
			}
			else if (nowValue < bestValue)
			{
				isSameValue = false;
			}
		}

		if (isSameValue == true)
		{
			saveIndex = (int) (Math.random() * overlapIndex.size());
		}

		return saveIndex;
	}
}