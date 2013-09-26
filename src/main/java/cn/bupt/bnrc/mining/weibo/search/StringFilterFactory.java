package cn.bupt.bnrc.mining.weibo.search;

import cn.bupt.bnrc.mining.weibo.util.Utils;

public class StringFilterFactory {
	public static enum StringFilterType {DONOTHINGFILTER, REMOVEILLEGALCHARFILTER, LENGTHFILTER}

	public static final StringFilter DONOTHINGFILTER = new StringFilter(){
		public String filterAndHandle(String content) {
			return content;
		}		
	};
	
	public static final StringFilter REMOVEILLEGALCHARFILTER = new StringFilter(){
		public String filterAndHandle(String content) {
			content = content.replaceAll("[^a-zA-Z0-9\u4E00-\u9FA5]", " ").replaceAll(" +", " ").trim();
			return content;
		}		
	};
	
	public static final StringFilter LENGTHFILTER = new StringFilter(){
		public String filterAndHandle(String content){
			if (content.length() < IndexSearchConstants.STATUS_MIN_LENGTH){
				return null;
			}
			return content;
		}
	};
	
	public static final StringFilter EMOTICONFILTER = new StringFilter(){
		public String filterAndHandle(String content){
			if (Utils.isContainAnyEmoticons(content)){
				return content;
			}
			return null;
		}
	};
	
	public static StringFilter getFilter(StringFilterType type){
		switch(type){
		case DONOTHINGFILTER:
			return DONOTHINGFILTER;
		case REMOVEILLEGALCHARFILTER:
			return REMOVEILLEGALCHARFILTER;
		default:
			return DONOTHINGFILTER;
		}
	}
}
