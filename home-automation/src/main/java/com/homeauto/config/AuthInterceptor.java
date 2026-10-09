package com.homeauto.config;

import com.homeauto.model.User;
import com.homeauto.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

// Runs before every /admin and /home page: makes sure the user is logged in
// and is only looking at pages for their own role.
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public AuthInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession(false);
        Object userId = (session == null) ? null : session.getAttribute("userId");

        if (userId == null) {
            response.sendRedirect("/login");
            return false;
        }

        Optional<User> user = userRepository.findById((Long) userId);
        if (user.isEmpty()) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        String role = user.get().getRole();
        String path = request.getRequestURI();

        if (path.startsWith("/admin") && !"ADMIN".equals(role)) {
            response.sendRedirect("/home");
            return false;
        }
        if (path.startsWith("/home") && !"HOMEOWNER".equals(role)) {
            response.sendRedirect("/admin");
            return false;
        }
        return true;
    }
}
