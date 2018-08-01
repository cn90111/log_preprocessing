package iplom;

import java.util.ArrayList;
import java.util.HashMap;

import processor.PreprocessMethod;

// IPLoM ─ https://ieeexplore.ieee.org/document/5936060/ 主
// IPLoM ─ https://dl.acm.org/citation.cfm?id=1557154 輔 (get rank position function)

public class IPLoM extends PreprocessMethod
{
	HashMap<Integer, ArrayList<String>> countMap;
	HashMap<Integer, HashMap<String, ArrayList<String>>> positionMap;
	HashMap<Integer, HashMap<String, HashMap<String, ArrayList<String>>>> bijectionMap;
	HashMap<Integer, HashMap<String, HashMap<String, String>>> templateTree;
	ArrayList<String> template;

	private final float FST = 0.2f; // File support threshold
	private final float PST = 0.01f; // Partition support threshold
	// set this parameter to very low values i.e. < 0.05 for optimum performance
	private final float UB = 0.8f; // Upper_bound, should > 0.5
	private final float LB = 0.2f; // Lower_bound, should < 0.5
	private final float CGT = 0.3f; // Cluster goodness threshold, optimal should in 0.3 − 0.6

	@Override
	protected ArrayList<String> getTemplate(String[] content)
	{
		countMap = partitionByEventSize(content);
		positionMap = partitionByTokenPosition(countMap, PST, FST);
		bijectionMap = partitionBySearchForBijection(positionMap, CGT, PST, UB, LB, FST);

		template = new ArrayList<String>();
		templateTree = discoverMessageType(bijectionMap);

		return template;
	}

	@Override
	protected String transform(String[] content)
	{
		StringBuilder template = new StringBuilder(2048);
		int lengthKey;
		boolean findTemplate;

		HashMap<String, HashMap<String, String>> positionMap;
		HashMap<String, String> bijectionMap;

		for (String line : content)
		{
			findTemplate = false;
			lengthKey = splitLog(line).length;
			positionMap = templateTree.get(lengthKey);
			for (String positionKey : positionMap.keySet())
			{
				if (line.contains(positionKey) || positionKey.equals("Outlier"))
				{
					bijectionMap = positionMap.get(positionKey);
					for (String bijectionKey : bijectionMap.keySet())
					{
						if (line.contains(bijectionKey))
						{
							template.append(bijectionMap.get(bijectionKey));
							template.append("\n");
							findTemplate = true;
							break;
						}
						else if (bijectionKey.equals("M-M") && compareTemplate(bijectionMap.get("M-M"), line))
						{
							template.append(bijectionMap.get("M-M"));
							template.append("\n");
							findTemplate = true;
							break;
						}
						else if (bijectionKey.equals("Outlier") && compareTemplate(bijectionMap.get("Outlier"), line))
						{
							template.append(bijectionMap.get("Outlier"));
							template.append("\n");
							findTemplate = true;
							break;
						}
					}

					if (findTemplate == true)
					{
						break;
					}
				}
			}

			if (findTemplate == false)
			{
				template.append(line);
				template.append("\n");
				findTemplate = true;
			}
		}
		return template.toString();
	}

	// count - log
	private HashMap<Integer, ArrayList<String>> partitionByEventSize(String[] content)
	{
		HashMap<Integer, ArrayList<String>> level1 = new HashMap<Integer, ArrayList<String>>();
		ArrayList<String> mapList;
		StringBuilder logTemplate = new StringBuilder(1024);
		int tokenCount;

		for (String line : content)
		{
			String[] token = splitLog(line);
			tokenCount = token.length;
			logTemplate.setLength(0);
			for (int i = 0; i < tokenCount; i++)
			{
				if (isNumber(token[i]))
				{
					token[i] = "*";
				}
			}
			for (String temp : token)
			{
				logTemplate.append(temp);
				logTemplate.append(" ");
			}
			if (!level1.containsKey(tokenCount))
			{
				level1.put(tokenCount, new ArrayList<String>());
			}
			mapList = level1.get(tokenCount);
			mapList.add(logTemplate.toString());
			level1.put(tokenCount, mapList);
		}

		return level1;
	}

	// count - partition - log
	private HashMap<Integer, HashMap<String, ArrayList<String>>> partitionByTokenPosition(
			HashMap<Integer, ArrayList<String>> countMap, float pst, float fst)
	{
		HashMap<Integer, HashMap<String, ArrayList<String>>> level1 = new HashMap<Integer, HashMap<String, ArrayList<String>>>();
		HashMap<Integer, ArrayList<String>> tokenMap = new HashMap<Integer, ArrayList<String>>();
		HashMap<String, ArrayList<String>> level2;
		ArrayList<String> tokenList;
		ArrayList<String> lineList;

		int minimalKindsIndex, minimalSize;

		// Determine token position P with lowest cardinality with respect to set of
		// unique tokens.
		for (Integer key : countMap.keySet())
		{
			tokenMap.clear();
			for (int i = 0; i < key; i++)
			{
				tokenMap.put(i, new ArrayList<String>());
			}
			for (String line : countMap.get(key))
			{
				String[] token = line.split(" ");
				for (int i = 0; i < token.length; i++)
				{
					tokenList = tokenMap.get(i);

					if (!tokenList.contains(token[i]))
					{
						tokenList.add(token[i]);
						tokenMap.put(i, tokenList);
					}
				}
			}

			minimalKindsIndex = -1;
			minimalSize = -1;
			for (int i = 0; i < key; i++)
			{
				tokenList = tokenMap.get(i);
				// init
				if (minimalKindsIndex == -1 && isUniqueToken(tokenList.toArray(new String[0])))
				{
					minimalKindsIndex = i;
					minimalSize = tokenList.size();
				}

				if (minimalSize > tokenList.size() && isUniqueToken(tokenList.toArray(new String[0])))
				{
					minimalKindsIndex = i;
					minimalSize = tokenMap.get(minimalKindsIndex).size();
				}
			}

			// Create a partition for each token value in the set of unique tokens that
			// appear in position P.
			level2 = new HashMap<String, ArrayList<String>>();
			for (String line : countMap.get(key))
			{
				String[] token = line.split(" ");
				if (!level2.containsKey(token[minimalKindsIndex]))
				{
					level2.put(token[minimalKindsIndex], new ArrayList<String>());
				}
				lineList = level2.get(token[minimalKindsIndex]);

				// Separate contents of partition based on unique token values in token position
				// P. into separate partitions.
				lineList.add(line);
				level2.put(token[minimalKindsIndex], lineList);
			}
			// if PSR < PS
			level2 = usePartitionSupportRatio(level2, pst);
			level1.put(key, level2);
		}

		// Option, File_Prune() {Input is the collection of newly created partitions}
		for (Integer key : level1.keySet())
		{
			level1.put(key, filePrune(level1.get(key), fst));
		}
		return level1;
	}

	// count - partition - bijection - log
	private HashMap<Integer, HashMap<String, HashMap<String, ArrayList<String>>>> partitionBySearchForBijection(
			HashMap<Integer, HashMap<String, ArrayList<String>>> positionMap, float cgt, float pst, float ub, float lb,
			float fst)
	{
		HashMap<Integer, HashMap<String, HashMap<String, ArrayList<String>>>> level1Map = new HashMap<Integer, HashMap<String, HashMap<String, ArrayList<String>>>>();
		HashMap<String, HashMap<String, ArrayList<String>>> level2Map;
		HashMap<String, ArrayList<String>> level3Map;

		HashMap<String, ArrayList<String>> tempMap;
		HashMap<String, ArrayList<String>> p1MappingMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> p2MappingMap = new HashMap<String, ArrayList<String>>();

		ArrayList<String> tempList;
		ArrayList<String> partitionList;
		ArrayList<ArrayList<String>> tokenCountList = new ArrayList<ArrayList<String>>();

		int uniqueTokenInPartition;
		int partitionSize;
		int p1;
		int p2;
		int cardinalityOfS;
		float cgr;
		float distance;
		boolean p1M, p2M;

		for (Integer logLength : positionMap.keySet())
		{
			tempMap = positionMap.get(logLength);
			partitionSize = logLength;
			tokenCountList.clear();
			level2Map = new HashMap<String, HashMap<String, ArrayList<String>>>();
			for (int i = 0; i < partitionSize; i++)
			{
				tokenCountList.add(new ArrayList<String>());
			}
			for (String partitionToken : tempMap.keySet())
			{
				for (int i = 0; i < tokenCountList.size(); i++)
				{
					tokenCountList.get(i).clear();
				}
				uniqueTokenInPartition = 0;
				partitionList = tempMap.get(partitionToken);
				p1MappingMap.clear();
				p2MappingMap.clear();
				level3Map = new HashMap<String, ArrayList<String>>();

				p1 = 0;
				p2 = 0;

				p1M = false;
				p2M = false;
				for (String line : partitionList)
				{
					String[] token = line.split(" ");
					for (int i = 0; i < token.length; i++)
					{
						if (!tokenCountList.get(i).contains(token[i]))
						{
							tokenCountList.get(i).add(token[i]);
						}
					}
				}
				// p1,p2 init = 0
				for (int i = 1; i < tokenCountList.size(); i++)
				{
					if (isUniqueToken(tokenCountList.get(i).toArray(new String[0])))
					{
						if (!isUniqueToken(tokenCountList.get(p1).toArray(new String[0])))
						{
							p1 = i;
						}
						else
						{
							p2 = i;
							break;
						}
					}
				}
				for (int i = 0; i < tokenCountList.size(); i++)
				{
					ArrayList<String> temp = tokenCountList.get(i);
					if (temp.size() == 1)
					{
						uniqueTokenInPartition = uniqueTokenInPartition + 1;
					}

					if (temp.size() > tokenCountList.get(p1).size() || temp.size() > tokenCountList.get(p2).size())
					{
						if (isUniqueToken(temp.toArray(new String[0])))
						{
							if (tokenCountList.get(p1).size() <= tokenCountList.get(p2).size() && i != p2)
							{
								p1 = p2;
							}
							p2 = i;
						}
					}
				}

				cgr = (float) uniqueTokenInPartition / partitionSize;

				if (cgr < cgt)
				{
					tempMap.remove(partitionToken);
					continue;
				}

				// Determine mappings of unique token values P1 in respect of token values in P2
				// and vice versa.
				for (String line : partitionList)
				{
					String[] token = line.split(" ");
					if (!p1MappingMap.containsKey(token[p1]))
					{
						p1MappingMap.put(token[p1], new ArrayList<String>());
					}
					if (!p1MappingMap.get(token[p1]).contains(token[p2]))
					{
						p1MappingMap.get(token[p1]).add(token[p2]);
					}

					if (!p2MappingMap.containsKey(token[p2]))
					{
						p2MappingMap.put(token[p2], new ArrayList<String>());
					}
					if (!p2MappingMap.get(token[p2]).contains(token[p1]))
					{
						p2MappingMap.get(token[p2]).add(token[p1]);
					}
				}

				for (ArrayList<String> value : p1MappingMap.values())
				{
					if (value.size() > 1)
					{
						p2M = true;
					}
				}
				for (ArrayList<String> value : p2MappingMap.values())
				{
					if (value.size() > 1)
					{
						p1M = true;
					}
				}

				// split_pos = P1
				if (p1M == false && p2M == false)
				{
					for (String p1Token : p1MappingMap.keySet())
					{
						level3Map.put(p1Token, new ArrayList<String>());
					}
					for (String line : partitionList)
					{
						String[] token = line.split(" ");
						tempList = level3Map.get(token[p1]);
						tempList.add(line);
						level3Map.put(token[p1], tempList);
					}
				}
				else if (p1M ^ p2M == true) // 1-M or M-1
				{
					if (p1M == true)
					{
						cardinalityOfS = p1MappingMap.size();
					}
					else // p2M == true
					{
						cardinalityOfS = p2MappingMap.size();
					}

					distance = (float) cardinalityOfS / partitionList.size();

					if (distance <= lb)
					{
						if (p2M == true) // 1-M
						{
							for (String p2Token : p2MappingMap.keySet())
							{
								level3Map.put(p2Token, new ArrayList<String>());
							}
							for (String line : partitionList)
							{
								String[] token = line.split(" ");
								tempList = level3Map.get(token[p2]);
								tempList.add(line);
								level3Map.put(token[p2], tempList);
							}
						}
						else // M-1
						{
							for (String p1Token : p1MappingMap.keySet())
							{
								level3Map.put(p1Token, new ArrayList<String>());
							}
							for (String line : partitionList)
							{
								String[] token = line.split(" ");
								tempList = level3Map.get(token[p1]);
								tempList.add(line);
								level3Map.put(token[p1], tempList);
							}
						}
					}
					else if (distance >= ub)
					{
						if (p2M == true) // 1-M
						{
							for (String p1Token : p1MappingMap.keySet())
							{
								level3Map.put(p1Token, new ArrayList<String>());
							}
							for (String line : partitionList)
							{
								String[] token = line.split(" ");
								tempList = level3Map.get(token[p1]);
								tempList.add(line);
								level3Map.put(token[p1], tempList);
							}
						}
						else // M-1
						{
							for (String p2Token : p2MappingMap.keySet())
							{
								level3Map.put(p2Token, new ArrayList<String>());
							}
							for (String line : partitionList)
							{
								String[] token = line.split(" ");
								tempList = level3Map.get(token[p2]);
								tempList.add(line);
								level3Map.put(token[p2], tempList);
							}
						}
					}
					else
					{
						if (p2M == true) // 1-M
						{
							for (String p1Token : p1MappingMap.keySet())
							{
								level3Map.put(p1Token, new ArrayList<String>());
							}
							for (String line : partitionList)
							{
								String[] token = line.split(" ");
								tempList = level3Map.get(token[p1]);
								tempList.add(line);
								level3Map.put(token[p1], tempList);
							}
						}
						else // M-1
						{
							for (String p2Token : p2MappingMap.keySet())
							{
								level3Map.put(p2Token, new ArrayList<String>());
							}
							for (String line : partitionList)
							{
								String[] token = line.split(" ");
								tempList = level3Map.get(token[p2]);
								tempList.add(line);
								level3Map.put(token[p2], tempList);
							}
						}
					}
				}
				else
				{
					if (!level3Map.containsKey("M-M"))
					{
						level3Map.put("M-M", new ArrayList<String>());
					}
					tempList = level3Map.get("M-M");
					for (String line : partitionList)
					{
						tempList.add(line);
					}
					level3Map.put("M-M", tempList);
				}

				// if PSR < PS
				level3Map = usePartitionSupportRatio(level3Map, pst);

				level2Map.put(partitionToken, level3Map);

				// Option, File_Prune() {Input is the collection of newly created partitions}
				for (String key : level2Map.keySet())
				{
					level2Map.put(key, filePrune(level2Map.get(key), fst));
				}
			}
			level1Map.put(logLength, level2Map);
		}
		return level1Map;
	}

	private HashMap<Integer, HashMap<String, HashMap<String, String>>> discoverMessageType(
			HashMap<Integer, HashMap<String, HashMap<String, ArrayList<String>>>> splitFinishMap)
	{
		HashMap<String, HashMap<String, ArrayList<String>>> partitionMap;
		HashMap<String, ArrayList<String>> bijectionMap;
		ArrayList<String> bijectionLog;

		HashMap<Integer, HashMap<String, HashMap<String, String>>> level1 = new HashMap<Integer, HashMap<String, HashMap<String, String>>>();
		HashMap<String, HashMap<String, String>> level2;
		HashMap<String, String> level3;

		ArrayList<String> templateToken = new ArrayList<String>();

		StringBuilder templateString = new StringBuilder(512);

		for (Integer logLength : splitFinishMap.keySet())
		{
			partitionMap = splitFinishMap.get(logLength);
			level2 = new HashMap<String, HashMap<String, String>>();
			for (String partitionToken : partitionMap.keySet())
			{
				level3 = new HashMap<String, String>();
				bijectionMap = partitionMap.get(partitionToken);
				for (String bijectionToken : bijectionMap.keySet())
				{
					templateToken.clear();
					templateString.setLength(0);
					bijectionLog = bijectionMap.get(bijectionToken);
					for (String log : bijectionLog)
					{
						String[] splitLog = log.split(" ");

						if (templateToken.isEmpty())
						{
							for (int i = 0; i < splitLog.length; i++)
							{
								templateToken.add(splitLog[i]);
							}
						}
						else
						{
							for (int i = 0; i < splitLog.length; i++)
							{
								if (!templateToken.get(i).equals(splitLog[i]))
								{
									templateToken.set(i, "*");
								}
							}
						}
					}
					for (int i = 0; i < templateToken.size(); i++)
					{
						templateString.append(templateToken.get(i));
						templateString.append(" ");
					}
					level3.put(bijectionToken, templateString.toString());
					template.add(templateString.toString());
				}
				level2.put(partitionToken, level3);
			}
			level1.put(logLength, level2);
		}

		return level1;
	}

	private HashMap<String, ArrayList<String>> filePrune(HashMap<String, ArrayList<String>> partition, float fst)
	{
		int length = partition.size();
		int linesInCollection = 0;
		int i = 0;
		float supp;

		// ArrayList<String> temp;
		String[] mapKey = new String[length];
		int[] linesInPartition = new int[length];

		for (String key : partition.keySet())
		{
			mapKey[i] = key;
			linesInPartition[i] = partition.get(key).size();
			i++;
		}
		for (i = 0; i < linesInPartition.length; i++)
		{
			linesInCollection = linesInCollection + linesInPartition[i];
		}
		for (i = 0; i < mapKey.length; i++)
		{
			supp = (float) linesInPartition[i] / linesInCollection;

			if (supp <= fst)
			{
				// if (!partition.containsKey("Outlier"))
				// {
				// partition.put("Outlier", new ArrayList<String>());
				// }
				// temp = new ArrayList<String>(partition.get("Outlier"));
				// for (String log : partition.get(mapKey[i]))
				// {
				// temp.add(log);
				// }
				// partition.put("Outlier", temp);
				partition.remove(mapKey[i]);
			}
		}
		return partition;
	}

	private HashMap<String, ArrayList<String>> usePartitionSupportRatio(HashMap<String, ArrayList<String>> partition,
			float pst)
	{
		int length = partition.size();
		int linesInCollection = 0;
		int i = 0;
		float supp;

		ArrayList<String> temp;
		String[] mapKey = new String[length];
		int[] linesInPartition = new int[length];

		for (String key : partition.keySet())
		{
			mapKey[i] = key;
			linesInPartition[i] = partition.get(key).size();
			i++;
		}
		for (i = 0; i < linesInPartition.length; i++)
		{
			linesInCollection = linesInCollection + linesInPartition[i];
		}
		for (i = 0; i < mapKey.length; i++)
		{
			supp = (float) linesInPartition[i] / linesInCollection;

			if (supp <= pst)
			{
				if (!partition.containsKey("Outlier"))
				{
					partition.put("Outlier", new ArrayList<String>());
				}
				temp = new ArrayList<String>(partition.get("Outlier"));
				for (String log : partition.get(mapKey[i]))
				{
					temp.add(log);
				}
				partition.put("Outlier", temp);
				partition.remove(mapKey[i]);
			}
		}
		return partition;
	}
}
