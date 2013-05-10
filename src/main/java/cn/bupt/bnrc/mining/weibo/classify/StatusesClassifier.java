package cn.bupt.bnrc.mining.weibo.classify;

import cn.bupt.bnrc.mining.weibo.util.Constants;

public class StatusesClassifier {

	private PolarityClassifier polarityClassifier = new PolarityClassifier();
	private SubObjClassifier subObjClassifier = new SubObjClassifier();
	
	public int classify(String status){
		int flag = Constants.UNKOWN;
		if (subObjClassifier.classify(status) == Constants.SUBJECTIVE){
			flag = polarityClassifier.classifyPolarity(status);
		}else{
			flag = Constants.OBJECTIVE;
		}
		
		return flag;
	}
}
