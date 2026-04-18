package com.example.gamelog;

import com.google.gson.annotations.SerializedName;

public class UpdateTaskStatusRequest {

    @SerializedName("task_status")
    private final String taskStatus;

    public UpdateTaskStatusRequest(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getTaskStatus() {
        return taskStatus;
    }
}
