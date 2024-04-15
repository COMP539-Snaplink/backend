package com.example.comp539_team2_backend;
import lombok.Data;
import java.io.Serializable;

@Data
public class JSONResult<T> implements Serializable {
    private String status;
    private T data;

    public JSONResult() {
    }

    public JSONResult(Throwable eMessage) {
        this.status = eMessage.getMessage();
    }


    public JSONResult(String message, T data) {
        this.status = message;
        this.data = data;
    }
}


