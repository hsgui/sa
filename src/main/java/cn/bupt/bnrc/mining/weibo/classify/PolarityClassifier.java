package cn.bupt.bnrc.mining.weibo.classify;

import java.util.List;

import cn.bupt.bnrc.mining.weibo.util.Constants;
import cn.bupt.bnrc.mining.weibo.util.NLPIRTools;

public class PolarityClassifier {

	public double classify(String status){
		List<String> segmentedWords = NLPIRTools.segmentSentenceWithPOSTaged(status);
		double score = 0;
		for (String segmentedWord : segmentedWords){
			if (segmentedWord.contains("/qgcp")){
				String positiveWord = segmentedWord.split("/")[0];
				score += lexicon.getPositiveScore(positiveWord);
			}else if (segmentedWord.contains("/qgcn")){
				String negativeWord = segmentedWord.split("/")[0];
				score += lexicon.getNegativeScore(negativeWord);
			}
		}
		
		return score;
	}
	
	public int classifyPolarity(String status){
		double score = this.classify(status);
		if (score > 0) return Constants.POSITIVE_FLAG;
		else return Constants.NEGATIVE_FLAG;
	}
	
	public int isNegativeStatus(String status){
		return -1;
	}
	
	public int isPositiveStatus(String status){
		return -1;
	}
	
	private Lexicon lexicon = Lexicon.getInstance();
}
