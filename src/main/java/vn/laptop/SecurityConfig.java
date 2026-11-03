package vn.laptop;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(AccountRepository repo) {
    return email -> {
      Account a =
          repo.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
              .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));
      return User.withUsername(a.email).password(a.passwordHash).roles(a.role).build();
    };
  }

  @Bean
  SecurityFilterChain security(HttpSecurity h) throws Exception {
    return h.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/",
                        "/products/**",
                        "/login",
                        "/register",
                        "/css/**",
                        "/img/**",
                        "/uploads/**",
                        "/webjars/**",
                        "/error",
                        "/payments/vnpay/ipn",
                        "/payments/momo/ipn",
                        "/payments/return",
                        "/api/products",
                        "/api/csrf")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
        .logout(l -> l.logoutSuccessUrl("/"))
        .csrf(c -> c.ignoringRequestMatchers("/payments/momo/ipn"))
        .build();
  }
}
