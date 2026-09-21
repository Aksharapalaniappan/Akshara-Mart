package com.aksharamart.servlet;

import com.aksharamart.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String role = req.getParameter("role"); // "BUYER" or "SELLER" - which login tab was used

        String sql = "SELECT user_id, full_name, email, role FROM users " +
                     "WHERE email = ? AND password = ? AND role = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password); // NOTE: compare hashed password in production
            ps.setString(3, role.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HttpSession session = req.getSession(true);
                    session.setAttribute("userId", rs.getInt("user_id"));
                    session.setAttribute("fullName", rs.getString("full_name"));
                    session.setAttribute("email", rs.getString("email"));
                    session.setAttribute("role", rs.getString("role"));

                    resp.sendRedirect("products.html");
                } else {
                    resp.sendRedirect("login.html?error=invalid_credentials");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendRedirect("login.html?error=server_error");
        }
    }
}
