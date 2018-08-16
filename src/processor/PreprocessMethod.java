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

	private String outputFilePath = "./preprocess_file.txt";

	public void transformFile(String filePath) throws Exception
	{
		fileContent = readFile(filePath);
		lineArray = splitByline(fileContent);
		template = getTemplate(lineArray);
		writeFile(filePath);
	}

	private String readFile(String filePath) throws FileNotFoundException
	{
		File file = new File(filePath);
		Scanner reader = new Scanner(file);
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

		FileWriter writer = new FileWriter(outputFilePath);

		String line;
		while (buffer.ready())
		{
			line = buffer.readLine();
			writer.write(transform(line));
			if (buffer.ready())
			{
				writer.write("\n");
			}
			writer.flush();
		}
		writer.flush();
		buffer.close();
		reader.close();
		writer.close();
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