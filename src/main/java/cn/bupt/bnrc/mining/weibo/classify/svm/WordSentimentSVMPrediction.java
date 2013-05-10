package cn.bupt.bnrc.mining.weibo.classify.svm;

import java.io.DataOutputStream;
import java.io.IOException;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class WordSentimentSVMPrediction {

	public static double[] predict(svm_model model, svm_problem problem, int predictProbability, DataOutputStream output) throws IOException{
		int svm_type = svm.svm_get_svm_type(model);
		int nr_class = svm.svm_get_nr_class(model);
		double[] prob_estimates=null;
		double[] predictValues = new double[problem.l];

		if(predictProbability == 1)
		{
			if(svm_type == svm_parameter.EPSILON_SVR || svm_type == svm_parameter.NU_SVR){
			}
			else{
				int[] labels=new int[nr_class];
				svm.svm_get_labels(model,labels);
				prob_estimates = new double[nr_class];
				output.writeBytes("labels");
				for(int j=0;j<nr_class;j++)
					output.writeBytes(" "+labels[j]);
				output.writeBytes("\n");
			}
		}		
		for (int i = 0; i < problem.l; i++){
			svm_node[] node = problem.x[i];
			double v;
			if (predictProbability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC)){
				v = svm.svm_predict_probability(model, node, prob_estimates);
				output.writeBytes(v+" ");
				for(int j=0;j<nr_class;j++)
					output.writeBytes(prob_estimates[j]+" ");
				output.writeBytes("\n");
			}
			else
			{
				v = svm.svm_predict(model, node);
				System.out.print(v+"\n");
			}
			predictValues[i] = v;
		}
		
		return predictValues;
	}	
}
