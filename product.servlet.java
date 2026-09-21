package com.aksharamart.servlet;

import com.aksharamart.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * GET  /products  -> returns JSON array of all products (any logged-in user)
 * POST /products   -> adds a new product (SELLER role only)
 */
@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String sql = "SELECT product_id, name, description, category, price, stock_quantity, image_url FROM products ORDER BY created_at DESC";

        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                json.append("{")
                    .append("\"productId\":").append(rs.getInt("product_id")).append(",")
                    .append("\"name\":").append(jsonString(rs.getString("name"))).append(",")
                    .append("\"description\":").append(jsonString(rs.getString("description"))).append(",")
                    .append("\"category\":").append(jsonString(rs.getString("category"))).append(",")
                    .append("\"price\":").append(rs.getBigDecimal("price")).append(",")
                    .append("\"stockQuantity\":").append(rs.getInt("stock_quantity")).append(",")
                    .append("\"imageUrl\":").append(jsonString(rs.getString("image_url")))
                    .append("}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        json.append("]");

        try (PrintWriter out = resp.getWriter()) {
            out.print(json.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || !"SELLER".equals(session.getAttribute("role"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Only sellers can add products");
            return;
        }

        int sellerId = (int) session.getAttribute("userId");
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String category = req.getParameter("category");
        String imageUrl = req.getParameter("imageUrl");
        BigDecimal price;
        int stock;

        try {
            price = new BigDecimal(req.getParameter("price"));
            stock = Integer.parseInt(req.getParameter("stockQuantity"));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid price or stock quantity");
            return;
        }

        String sql = "INSERT INTO products (seller_id, name, description, category, price, stock_quantity, image_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sellerId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, category);
            ps.setBigDecimal(5, price);
            ps.setInt(6, stock);
            ps.setString(7, imageUrl);
            ps.executeUpdate();

            resp.sendRedirect("products.html?added=true");

        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
