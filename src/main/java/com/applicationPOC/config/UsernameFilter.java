package com.applicationPOC.config;

import java.io.IOException;

import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@Component
// A simple filter to add a demo username to each request and to show how to use filters in the application to manipulate requests.
public class UsernameFilter implements Filter {

 @Override
 public void doFilter(ServletRequest request,
                      ServletResponse response,
                      FilterChain chain) throws IOException, ServletException {
     request.setAttribute("username", "Nitin"); // demo user name
     chain.doFilter(request, response);
 }
}

