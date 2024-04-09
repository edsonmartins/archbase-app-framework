package br.com.archbase.validation.validators;

public class TimeActionResponse<T> {
    private T response;

    private long time;

    public TimeActionResponse(T response, long time) {
        super();
        this.response = response;
        this.time = time;
    }


    public TimeActionResponse() {

    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
