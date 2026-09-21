package com.aksharamart.servlet;

import com.aksharamart.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String role = req.getParameter("role"); // "BUYER" or "SELLER"

        if (fullName == null || email == null || password == null || role == null
                || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            resp.sendRedirect("register.html?error=missing_fields");
            return;
        }

        String sql = "INSERT INTO users (full_name, email, password, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, password); // NOTE: hash this in production (e.g. BCrypt)
            ps.setString(4, role.toUpperCase());
            ps.executeUpdate();

            resp.sendRedirect("login.html?registered=true");

        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                // duplicate email (unique constraint)
                resp.sendRedirect("register.html?error=email_exists");
            } else {
                e.printStackTrace();
                resp.sendRedirect("register.html?error=server_error");
            }
        }
    }
}
