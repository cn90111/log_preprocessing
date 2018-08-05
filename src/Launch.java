import java.util.ArrayList;

import molfi.MoLFI;
import processor.PreprocessMethod;

public class Launch
{
	public static void main(String[] arg) throws Exception
	{
		ArrayList<String> template;
		PreprocessMethod preprocess = new MoLFI();
		preprocess.transformFile("./yarn-hduser-nodemanager-master-filter.txt");
		template = preprocess.getTemplate();

		for (String line : template)
		{
			System.out.println(line);
		}
		System.exit(0);
	}
}
