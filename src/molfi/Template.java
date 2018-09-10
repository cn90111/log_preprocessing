package molfi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import processor.AbstractTemplate;
import processor.PreprocessMethod;

public class Template extends AbstractTemplate
{
	private int[] constantIndex;
	private int[] variableIndex;
	private ArrayList<String> matchLog;
	private HashMap<Integer, ArrayList<String>> logContent;
	private int logContentSize;
	private float freq;
	private float spec;

	public Template(String content, HashMap<Integer, ArrayList<String>> logContent, int logContentSize)
	{
		super(content);
		matchLog = new ArrayList<String>();
		this.logContentSize = logContentSize;
		this.logContent = logContent;
		update();
	}

	public Template(Template other)
	{
		super(other.getContent());
		matchLog = new ArrayList<String>();
		logContentSize = other.getLogContentSize();
		logContent = other.getLogContent();
		update();
	}
	
	public Template(String template)
	{
		super(template);
		tokens = content.split(" ");
	}

	public HashMap<Integer, ArrayList<String>> getLogContent()
	{
		return logContent;
	}

	public int getLogContentSize()
	{
		return logContentSize;
	}

	public float getFreq()
	{
		return freq;
	}

	public float getSpec()
	{
		return spec;
	}

	public int[] getConstantIndex()
	{
		return constantIndex;
	}

	public int[] getVariableIndex()
	{
		return variableIndex;
	}

	public int getMatchLogSize()
	{
		return matchLog.size();
	}

	protected void update()
	{
		tokens = content.split(" ");
		detectConstantIndex();
		detectVariableIndex();
		getMatchMessage();
		freqValue();
		specValue();
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
			randomIndex = (int) (Math.random() * variableIndex.length);
			randomMessage = (int) (Math.random() * matchLog.size());
			messageToken = matchLog.get(randomMessage).split(" ");
		}
		tokens[variableIndex[randomIndex]] = messageToken[variableIndex[randomIndex]];
		content = PreprocessMethod.combineTokens(tokens);
		update();
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

	private void freqValue()
	{
		freq = (float) matchLog.size() / logContentSize;
	}

	private void specValue()
	{
		spec = (float) constantIndex.length / tokens.length;
	}

	private void getMatchMessage()
	{
		int templateLength;
		templateLength = tokens.length;
		matchLog.clear();

		for (String log : logContent.get(templateLength))
		{
			if (compareTemplate(log))
			{
				matchLog.add(log);
			}
		}
	}
}
