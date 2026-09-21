package com.aksharamart.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * GET /session-info -> JSON with the logged-in user's basic info,
 * or {"loggedIn": false} if there is no active session.
 * Used by frontend JS to decide which UI elements to show (e.g. "Add Product" for sellers).
 */
@WebServlet("/session-info")
public class SessionInfoServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        try (PrintWriter out = resp.getWriter()) {
            if (session == null || session.getAttribute("userId") == null) {
                out.print("{\"loggedIn\": false}");
            } else {
                out.print("{"
                        + "\"loggedIn\": true,"
                        + "\"userId\": " + session.getAttribute("userId") + ","
                        + "\"fullName\": \"" + session.getAttribute("fullName") + "\","
                        + "\"role\": \"" + session.getAttribute("role") + "\""
                        + "}");
            }
        }
    }
}
