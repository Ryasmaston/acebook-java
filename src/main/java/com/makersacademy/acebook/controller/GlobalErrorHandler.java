package com.makersacademy.acebook.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView handleIllegalStateException(
            IllegalStateException exception) {

        ModelAndView modelAndView = new ModelAndView("/error");

        modelAndView.addObject("errorMessage", exception.getMessage());

        return modelAndView;
    }
}