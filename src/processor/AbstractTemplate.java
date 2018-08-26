package processor;

public abstract class AbstractTemplate
{
	protected String content;
	protected String[] tokens;

	protected AbstractTemplate(String content)
	{
		this.content = content;
	}

	protected abstract void update();

	public String getContent()
	{
		return content;
	}

	public String[] getTokens()
	{
		return tokens;
	}

	public boolean compareTemplate(String log)
	{
		String[] logTokens = PreprocessMethod.splitLog(log);

		int offset = 0;

		for (int i = 0; i < tokens.length; i++)
		{
			if (logTokens[i + offset].equals(""))
			{
				offset++;
			}
			if (tokens[i].equals("*"))
			{
				continue;
			}
			if (tokens[i].equals("#spec#"))
			{
				if (!(logTokens[i + offset].equals("#spec#") || PreprocessMethod.isNumber(logTokens[i + offset])))
				{
					return false;
				}
			}
			else if (!tokens[i].equals(logTokens[i + offset]))
			{
				return false;
			}
		}
		return true;
	}
}