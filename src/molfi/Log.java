package molfi;

import java.util.ArrayList;

public class Log
{
	private String content;
	private String[] token;
	private ArrayList<String> matchTemplate;

	public Log(String content)
	{
		this.content = content;
		update();

	}

	private void update()
	{
		token = content.split(" ");
	}

	public String getContent()
	{
		return content;
	}

	public void addMatchTemplate(String template)
	{
		matchTemplate.add(template);
	}

	public void removeMatchTemplate(String template)
	{
		for (int i = 0; i < matchTemplate.size(); i++)
		{
			if (matchTemplate.get(i).equals(template))
			{
				matchTemplate.remove(i);
				break;
			}
		}
	}

	public ArrayList<String> getMatchTemplate()
	{
		ArrayList<String> temp = new ArrayList<String>(matchTemplate);
		return temp;
	}
}
