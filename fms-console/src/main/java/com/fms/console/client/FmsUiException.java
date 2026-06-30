package com.fms.console.client;

public class FmsUiException extends RuntimeException {

  private final int httpStatus;
  private final String errorCode;

  public FmsUiException(int httpStatus, String errorCode, String message) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
