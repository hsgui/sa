package cn.bupt.bnrc.mining.weibo.classify.svm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.util.Constants;

import com.google.common.io.Files;

public class ProblemConstructor {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public List<String> readAllTrainingSet(String filePath){
		File file = new File(filePath);
		try{
			List<String> lines = Files.readLines(file, Constants.defaultCharset);
			logger.info("read training set -- num={}, file={}", lines.size(), file.getName());
			return lines;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return new ArrayList<String>();
	}
	
	public List<SampleEntry> parseTrainingSet(List<String> rawSet){
		List<SampleEntry> set = new ArrayList<SampleEntry>(rawSet.size());
		
		for (String line : rawSet){
			String[] pair = line.split(",");
			SampleEntry sample = new SampleEntry();
			sample.word = pair[0];
			if (pair.length > 1){
				sample.label = new Integer(pair[1]);
			}else{
				sample.label = -100;
				logger.info("cannot split string: {}", line);
			}
			set.add(sample);
		}
		return set;
	}
}
