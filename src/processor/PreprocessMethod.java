package processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public abstract class PreprocessMethod
{
	private String fileContent;
	private String[] lineArray;
	private ArrayList<String> template;

	public void transformFile(String filePath) throws Exception
	{
		fileContent = readFile(filePath);
		lineArray = splitByline(fileContent);
		template = getTemplate(lineArray);
		fileContent = transform(lineArray);
		writeFile(fileContent);
	}

	private String readFile(String filePath) throws FileNotFoundException
	{
		Scanner reader = new Scanner(new File(filePath));
		String fileContent = reader.useDelimiter("\\Z").next();
		reader.close();
		return fileContent;
	}

	private String[] splitByline(String content)
	{
		String nextLine = "\r?\n";
		String[] lineArray = content.split(nextLine);
		return lineArray;
	}

	protected abstract ArrayList<String> getTemplate(String[] content);

	protected abstract String transform(String[] content);

	private void writeFile(String content) throws IOException
	{
		Files.write(Paths.get("./preprocess_file.txt"), fileContent.getBytes());
	}

	protected String[] splitLog(String log)
	{
		String anySymbol = "[\\W]+";
		return log.split(anySymbol);
	}

	public ArrayList<String> getTemplate()
	{
		return template;
	}

	protected boolean compareTemplate(String template, String log)
	{
		String[] templateTokens = template.split(" ");
		String[] logTokens = splitLog(log);

		int offset = 0;
		
		for (int i = 0; i < templateTokens.length; i++)
		{
			try
			{
				if (logTokens[i + offset].equals(""))
				{
					offset++;
				}	
			}
			catch(Exception e)
			{
				System.out.println("123");
			}
			if (templateTokens[i].equals("*") || templateTokens[i].equals("#spec#"))
			{
				continue;
			}
			if (!templateTokens[i].equals(logTokens[i + offset]))
			{
				return false;
			}
		}
		return true;
	}

	protected boolean compareTemplate(ArrayList<String> template, String log)
	{
		for (String temp : template)
		{
			if (compareTemplate(temp, log))
			{
				return true;
			}
		}
		return false;
	}

	protected boolean isNumber(String token)
	{
		return token.matches("^\\d+$");
	}

	protected boolean isUniqueToken(String[] tokens)
	{
		for (String token : tokens)
		{
			if (token.equals("*"))
			{
				return false;
			}
		}
		return true;
	}
}
