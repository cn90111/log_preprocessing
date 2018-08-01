package molfi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import processor.AbstractTemplate;
import processor.PreprocessMethod;

public class Template extends AbstractTemplate
{
	private int[] constantIndex;
	private int[] variableIndex;
	private ArrayList<String> matchLog;

	public Template(String content)
	{
		super(content);
		matchLog = new ArrayList<String>();
	}

	public int[] getConstantIndex()
	{
		return constantIndex;
	}

	public int[] getVariableIndex()
	{
		return variableIndex;
	}

	protected void update()
	{
		tokens = content.split(" ");
		detectConstantIndex();
		detectVariableIndex();
	}

	public void fixAllStarTemplate()
	{
		boolean allStar = true;
		for (String token : tokens)
		{
			if (!token.equals("*") && !token.equals("#spec#"))
			{
				allStar = false;
			}
		}

		if (allStar == true)
		{
			changeVariableOfTemplate();
		}
	}

	public void postProcessing()
	{
		boolean isUniqueToken;
		String beforeToken;
		int templateLength = tokens.length;

		String[][] logTokens = new String[matchLog.size()][templateLength];

		for (int i = 0; i < logTokens.length; i++)
		{
			logTokens[i] = matchLog.get(i).split(" ");
		}

		for (int i = 0; i < templateLength; i++)
		{
			if (tokens[i].equals("*"))
			{
				isUniqueToken = true;
				beforeToken = null;

				for (int j = 0; j < logTokens.length; j++)
				{
					if (beforeToken == null)
					{
						beforeToken = logTokens[j][i];
					}

					if (!beforeToken.equals(logTokens[j][i]))
					{
						isUniqueToken = false;
					}
				}

				if (isUniqueToken == true)
				{
					tokens[i] = beforeToken;
					content = PreprocessMethod.combineTokens(tokens);
				}
			}
		}
		update();
	}

	private void detectConstantIndex()
	{
		ArrayList<Integer> indexList = new ArrayList<Integer>();

		for (int i = 0; i < tokens.length; i++)
		{
			if (!tokens[i].equals("*") && !tokens[i].equals("#spec#"))
			{
				indexList.add(i);
			}
		}
		constantIndex = convertIntegers(indexList);
	}

	private void detectVariableIndex()
	{
		ArrayList<Integer> indexList = new ArrayList<Integer>();

		for (int i = 0; i < tokens.length; i++)
		{
			if (tokens[i].equals("*"))
			{
				indexList.add(i);
			}
		}
		variableIndex = convertIntegers(indexList);
	}

	public void changeConstantOfTemplate()
	{
		int randomIndex = (int) (Math.random() * constantIndex.length);
		tokens[constantIndex[randomIndex]] = "*";
		content = PreprocessMethod.combineTokens(tokens);
		update();
	}

	public void changeVariableOfTemplate()
	{
		int randomIndex = (int) (Math.random() * variableIndex.length);
		int randomMessage = (int) (Math.random() * matchLog.size());
		String[] messageToken = matchLog.get(randomMessage).split(" ");

		while (messageToken[variableIndex[randomIndex]].equals("#spec#"))
		{
			randomMessage = (int) (Math.random() * matchLog.size());
			messageToken = matchLog.get(randomMessage).split(" ");
		}
		tokens[variableIndex[randomIndex]] = messageToken[variableIndex[randomIndex]];
		content = PreprocessMethod.combineTokens(tokens);
		update();
	}

	public void clearMatchLog()
	{
		matchLog.clear();
	}

	public void addMatchLog(String log)
	{
		matchLog.add(log);
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
}
