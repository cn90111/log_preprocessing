package processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import molfi.Template;

public abstract class PreprocessMethod
{
	private String fileContent;
	private String[] lineArray;
	private ArrayList<String> template;

	private String outputFilePath = "./preprocess_file.txt";

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
		Files.write(Paths.get(outputFilePath), fileContent.getBytes());
	}

	public ArrayList<String> getTemplate()
	{
		return template;
	}

	protected boolean isNumber(String token)
	{
		return token.matches("^\\d+$");
	}

	protected boolean isUniqueToken(String[] tokens)
	{
		for (String token : tokens)
		{
			if (token.equals("*") || token.equals("#spec#"))
			{
				return false;
			}
		}
		return true;
	}

	public static String[] splitLog(String log)
	{
		String anySymbol = "[\\W]+";
		return log.split(anySymbol);
	}

	public static String combineTokens(String[] tokens)
	{
		StringBuilder combineString = new StringBuilder(128);
		for (String token : tokens)
		{
			combineString.append(token);
			combineString.append(" ");
		}
		return combineString.toString();
	}

	public static boolean compareTemplate(ArrayList<Template> template, String log)
	{
		for (Template temp : template)
		{
			if (temp.compareTemplate(log))
			{
				return true;
			}
		}
		return false;
	}
}