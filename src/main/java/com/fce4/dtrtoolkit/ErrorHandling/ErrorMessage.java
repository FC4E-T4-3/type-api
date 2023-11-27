package com.fce4.dtrtoolkit.ErrorHandling;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.underscore.U;

public class ErrorMessage {
  private int statusCode;
  private Date timestamp;
  private String message;
  private String description;

  public ErrorMessage(int statusCode, Date timestamp, String message, String description) {
    this.statusCode = statusCode;
    this.timestamp = timestamp;
    this.message = message;
    this.description = description;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public String getMessage() {
    return message;
  }

  public String getDescription() {
    return description;
  }

  public JsonNode serialize() {
    ObjectMapper mapper = new ObjectMapper(); 
    JsonNode node = mapper.convertValue(this, JsonNode.class);
    return node;
  }

  public String toXML() {
    return U.jsonToXml(serialize().toString());
  }

}