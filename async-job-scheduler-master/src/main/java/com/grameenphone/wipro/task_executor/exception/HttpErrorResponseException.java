package com.grameenphone.wipro.task_executor.exception;

import java.io.IOException;

public class HttpErrorResponseException extends IOException {
	private int status;
	private String response;

	private String reason;

	public String getReason() {
		return reason;
	}

	public HttpErrorResponseException(int status, String response) {
		this.status = status;
		this.response = response;
	}

	public int getStatus() {
		return status;
	}

	public String getResponse() {
		return response;
	}

	@Override
	public String getMessage() {
		return getResponse();
	}
}