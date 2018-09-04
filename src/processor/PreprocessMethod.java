package processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import molfi.Template;

public abstract class PreprocessMethod
{
	private String fileContent;
	private String[] lineArray;
	private ArrayList<String> template;

	private String outputFilePath = "./yarn_preprocess_file.txt";
	private String outputTemplatePath = "./yarn_preprocess_template.txt";

	public void transformFile(String filePath) throws Exception
	{
		fileContent = readFile(filePath);
		lineArray = splitByline(fileContent);
		template = getTemplate(lineArray);
		writeFile(filePath);
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

	protected abstract String transform(String line);

	private void writeFile(String filePath) throws IOException
	{
		FileReader reader = new FileReader(filePath);
		BufferedReader buffer = new BufferedReader(reader);

		FileWriter fileWriter = new FileWriter(outputFilePath);
		FileWriter templateWriter = new FileWriter(outputTemplatePath);

		for(String template : template)
		{
			templateWriter.write(template);
			templateWriter.write("\n");
			templateWriter.flush();
		}
		templateWriter.close();
		
		String line;
		while (buffer.ready())
		{
			line = buffer.readLine();
			fileWriter.write(transform(line));
			if (buffer.ready())
			{
				fileWriter.write("\n");
			}
			fileWriter.flush();
		}
		fileWriter.flush();
		buffer.close();
		reader.close();
		fileWriter.close();
	}

	public ArrayList<String> getTemplate()
	{
		return template;
	}

	public static boolean isNumber(String token)
	{
		String[] splitToken = token.split("[\\W]+");
		for (String temp : splitToken)
		{
			if (!temp.matches("^\\d+$"))
			{
				return false;
			}
		}
		return true;
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
		String splitToken = "[ 	]+";
		return log.split(splitToken);
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