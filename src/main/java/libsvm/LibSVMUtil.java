package libsvm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;

public class LibSVMUtil {

	public static void writeProblemToFile(svm_problem problem, String filename){
		Formatter formatter = new Formatter(new StringBuilder());
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(filename));
		} catch(IOException e) {
			System.err.println("can't open file " + filename);
			System.exit(1);
		}
		for (int i=0; i<problem.l; i++){
			double y = problem.y[i];
			svm_node[] x = problem.x[i];
			formatter.format("%f ", y);
			for (int j=0; j<x.length;j++){
				formatter.format("%d:%f ", x[j].index, x[j].value);
			}
			formatter.format("\n");
		}
		
		try {
			writer.write(formatter.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static svm_parameter findBestCGParameter(svm_problem problem, svm_parameter parameter){
		int cBegin = -5, cEnd = 15, cStep = 2;
		int gBegin = 3, gEnd = -15, gStep = -2;
		int nrFold = 5;
		CrossValidationResult result;
		
		double bestRate = 0.0f;
		int bestC = -5, bestG = 3;
		for (int c = cBegin; c <= cEnd; c+=cStep){
			for (int g = gBegin; g >= gEnd; g+=gStep){
				parameter.C = Math.exp(c);
				parameter.gamma = Math.exp(g);
				result = LibSVMUtil.doCrossValidation(problem, parameter, nrFold);
				if ((result.accuracy > bestRate) || (bestRate == result.accuracy && g == bestG && c < bestC)){
					bestRate = result.accuracy;
					bestC = c;
					bestG = g;
					System.out.println(String.format("get a best result: %d, %d, %f", bestC, bestG, bestRate));
				}
			}
		}
		parameter.C = Math.exp(bestC);
		parameter.gamma = Math.exp(bestG);
		return parameter;
	}
	
	public static CrossValidationResult doCrossValidation(svm_problem prob, svm_parameter param, int nrFold)
	{
		int i;
		int total_correct = 0;
		double total_error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
		double[] target = new double[prob.l];
		
		CrossValidationResult result = new CrossValidationResult();

		svm.svm_cross_validation(prob, param, nrFold, target);
		if(param.svm_type == svm_parameter.EPSILON_SVR ||
		   param.svm_type == svm_parameter.NU_SVR)
		{
			for(i=0;i<prob.l;i++)
			{
				double y = prob.y[i];
				double v = target[i];
				total_error += (v-y)*(v-y);
				sumv += v;
				sumy += y;
				sumvv += v*v;
				sumyy += y*y;
				sumvy += v*y;
			}
			
			result.meanSquaredError = total_error/prob.l;
			result.squaredCorrelationCoeffient = ((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
					((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy));
			
			//System.out.print("Cross Validation Mean squared error = "+ result.meanSquaredError +"\n");
			//System.out.print("Cross Validation Squared correlation coefficient = "+result.squaredCorrelationCoeffient+"\n");
		}
		else
		{
			for(i=0;i<prob.l;i++)
				if(Math.abs(target[i] - prob.y[i]) < 0.1)
					++total_correct;
			result.accuracy = (double)total_correct/prob.l;
			//System.out.print("Cross Validation Accuracy = " + 100.0*result.accuracy + "%\n");
		}
		
		return result;
	}
	
	public static class CrossValidationResult{
		public double meanSquaredError;
		public double squaredCorrelationCoeffient;
		public double accuracy;
	}
}
