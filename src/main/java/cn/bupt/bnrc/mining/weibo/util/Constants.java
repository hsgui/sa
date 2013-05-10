package cn.bupt.bnrc.mining.weibo.util;

import java.nio.charset.Charset;

public class Constants {

	public static String INDEX = "index";
	public static String EMOTICON_INDEX = "emoticon-index";
	public static String FILED_NAME = "content";
	
	public static Charset defaultCharset = Charset.forName("utf-8");
	
	public static final String RESOURCES_PREFIX = Constants.class.getClassLoader().getResource("").getPath().substring(1);
	
	
	public static final int UNKOWN = 0;
	public static final int POSITIVE_FLAG = 1;
	public static final int NEGATIVE_FLAG = 2;
	public static final int NETURAL_FLAG = 3;
	public static final int BOTH_FLAG = 4;
	
	public static final int OBJECTIVE = 5;
	public static final int SUBJECTIVE = 6;
	
 }
