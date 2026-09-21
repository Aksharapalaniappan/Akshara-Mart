package com.aksharamart.servlet;

import com.aksharamart.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * POST /checkout -> converts the logged-in user's cart into an order,
 * decrements stock, empties the cart, then redirects to order-success.html
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect("login.html");
            return;
        }
        int userId = (int) session.getAttribute("userId");

        String selectCartSql =
            "SELECT c.cart_item_id, c.product_id, c.quantity, p.name, p.price, p.stock_quantity " +
            "FROM cart_items c JOIN products p ON c.product_id = p.product_id WHERE c.user_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            BigDecimal total = BigDecimal.ZERO;
            java.util.List<Object[]> lines = new java.util.ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(selectCartSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int productId = rs.getInt("product_id");
                        int qty = rs.getInt("quantity");
                        String name = rs.getString("name");
                        BigDecimal price = rs.getBigDecimal("price");
                        int stock = rs.getInt("stock_quantity");

                        if (qty > stock) {
                            conn.rollback();
                            resp.sendRedirect("cart.html?error=insufficient_stock");
                            return;
                        }

                        total = total.add(price.multiply(BigDecimal.valueOf(qty)));
                        lines.add(new Object[]{productId, name, qty, price});
                    }
                }
            }

            if (lines.isEmpty()) {
                conn.rollback();
                resp.sendRedirect("cart.html?error=empty_cart");
                return;
            }

            int orderId;
            String insertOrderSql = "INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, 'PLACED')";
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setBigDecimal(2, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }
            }

            String insertItemSql =
                "INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price) VALUES (?, ?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

            try (PreparedStatement itemPs = conn.prepareStatement(insertItemSql);
                 PreparedStatement stockPs = conn.prepareStatement(updateStockSql)) {

                for (Object[] line : lines) {
                    int productId = (int) line[0];
                    String name = (String) line[1];
                    int qty = (int) line[2];
                    BigDecimal price = (BigDecimal) line[3];

                    itemPs.setInt(1, orderId);
                    itemPs.setInt(2, productId);
                    itemPs.setString(3, name);
                    itemPs.setInt(4, qty);
                    itemPs.setBigDecimal(5, price);
                    itemPs.addBatch();

                    stockPs.setInt(1, qty);
                    stockPs.setInt(2, productId);
                    stockPs.addBatch();
                }
                itemPs.executeBatch();
                stockPs.executeBatch();
            }

            try (PreparedStatement clearCart = conn.prepareStatement("DELETE FROM cart_items WHERE user_id = ?")) {
                clearCart.setInt(1, userId);
                clearCart.executeUpdate();
            }

            conn.commit();
            resp.sendRedirect("order-success.html?orderId=" + orderId);

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                resp.sendRedirect("cart.html?error=server_error");
            } catch (IOException ignored) {}
        }
    }
}
