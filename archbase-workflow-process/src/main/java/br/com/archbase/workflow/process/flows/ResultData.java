package br.com.archbase.workflow.process.flows;

public class ResultData {

    private Object result;

    private ResultData(Object result) {
        this.result = result;
    }

    public static ResultData of(Object result) {
        return new ResultData(result);
    }

    public Object getResult() {
        return result;
    }
}
