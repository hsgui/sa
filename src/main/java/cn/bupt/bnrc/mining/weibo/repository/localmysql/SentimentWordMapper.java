package cn.bupt.bnrc.mining.weibo.repository.localmysql;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;

public interface SentimentWordMapper {
	
	@Select("select * from sentiment_words")
	public List<Map<String, Object>> getAllSentimentWords();
}
