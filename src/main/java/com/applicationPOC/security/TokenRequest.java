package com.applicationPOC.security;

/// Simple DTO to represent a JSON request body containing a token string
/// Example JSON
/// {
///  "token":
/// "
/// eyJhbGciOi
/// JIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE2OTQ4ODg0MDAsImV4cCI6MTY5NDg5MjAwMH0.abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567890"
/// }
/// This class is used to deserialize the JSON request body into a Java object that contains the token string. It has a single field `token` with getter and setter methods, and a default constructor for Jackson to use during deserialization.
public class TokenRequest {
    private String token;

    // Default constructor for Jackson (required)
    public TokenRequest() {}

    public TokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
