import java.util.ArrayList;

import iplom.IPLoM;
import molfi.MoLFI;
import processor.PreprocessMethod;

public class Launch
{
	private static final int FIND_TEMPLATE = 1;
	private static final int TRANSFORM_FILE = 2;

	private static final int MOLFI = 1;
	private static final int IPLOM = 2;
	
	public static void main(String[] arg) throws Exception
	{
		int method = MOLFI;
		int mode = TRANSFORM_FILE;
		String filePath = "./yarn-hduser-nodemanager-master-filter-simple2.txt";
		String templatePath = "./yarn_preprocess_template.txt";
		
		ArrayList<String> template;
		PreprocessMethod preprocess = null;
		
		switch(method)
		{
			case MOLFI:
				preprocess = new MoLFI();
				break;
			case IPLOM:
				preprocess = new IPLoM();
				break;
		}
		
		switch(mode)
		{
			case FIND_TEMPLATE:
				preprocess.transformFile(filePath);
				break;
			case TRANSFORM_FILE:
				preprocess.transformFile(filePath, templatePath);
				break;
		}
		template = preprocess.getTemplate();
		
		for (String line : template)
		{
			System.out.println(line);
		}
		System.exit(0);
	}
}
