package edu.eci.arsw.blueprints.controllers.dto;

public record ApiResponse<T>(int code, String message, T data) {}
