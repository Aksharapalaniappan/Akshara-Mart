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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * GET    /cart?action=view            -> JSON array of the logged-in user's cart items
 * POST   /cart?action=add             -> params: productId, quantity
 * POST   /cart?action=remove          -> params: cartItemId
 */
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please log in");
            return;
        }
        int userId = (int) session.getAttribute("userId");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String sql = "SELECT c.cart_item_id, c.quantity, p.product_id, p.name, p.price, p.image_url " +
                     "FROM cart_items c JOIN products p ON c.product_id = p.product_id " +
                     "WHERE c.user_id = ?";

        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;
                    json.append("{")
                        .append("\"cartItemId\":").append(rs.getInt("cart_item_id")).append(",")
                        .append("\"productId\":").append(rs.getInt("product_id")).append(",")
                        .append("\"name\":\"").append(rs.getString("name").replace("\"", "\\\"")).append("\",")
                        .append("\"price\":").append(rs.getBigDecimal("price")).append(",")
                        .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                        .append("\"imageUrl\":\"").append(rs.getString("image_url") == null ? "" : rs.getString("image_url")).append("\"")
                        .append("}");
                }
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
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please log in");
            return;
        }
        int userId = (int) session.getAttribute("userId");
        String action = req.getParameter("action");

        try (Connection conn = DBConnection.getConnection()) {

            if ("add".equals(action)) {
                int productId = Integer.parseInt(req.getParameter("productId"));
                int quantity = Integer.parseInt(req.getParameter("quantity"));

                String sql = "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.setInt(3, quantity);
                    ps.executeUpdate();
                }
                resp.setStatus(HttpServletResponse.SC_OK);

            } else if ("remove".equals(action)) {
                int cartItemId = Integer.parseInt(req.getParameter("cartItemId"));
                String sql = "DELETE FROM cart_items WHERE cart_item_id = ? AND user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, cartItemId);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
                resp.setStatus(HttpServletResponse.SC_OK);

            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
            }

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
