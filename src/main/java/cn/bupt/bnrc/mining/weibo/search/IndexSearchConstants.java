package cn.bupt.bnrc.mining.weibo.search;

import java.nio.charset.Charset;


public class IndexSearchConstants {

	public static String STATUS_INDEX = "index";
	public static String EMOTICON_INDEX = "emoticon-index";
	
	public static String CONTENT_FIELD = "content";
	public static String EMOTICON_FIELD = "emoticons";
	
	public static final int STATUS_MIN_LENGTH = 4;
	
	public static Charset defaultCharset = Charset.forName("utf-8");
}
