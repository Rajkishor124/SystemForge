package com.systemforge.backend.playground.template.impl;

import com.systemforge.backend.playground.template.ServiceTemplate;
import com.systemforge.backend.playground.template.TemplateStructure;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MVP Template: AUTH + JWT
 *
 * <p>Provides base Spring Boot security code with placeholder tokens
 * for feature modules to inject into. All dynamic sections use
 * {@code {{PLACEHOLDER}}} syntax.
 */
@Component
public class JwtAuthTemplate implements ServiceTemplate {

    @Override
    public String getKey() {
        return "AUTH_JWT";
    }

    @Override
    public TemplateStructure getTemplateStructure() {
        return TemplateStructure.builder()
                .controllerTemplate(CONTROLLER_TEMPLATE)
                .controllerImports(java.util.Set.of(
                        "org.springframework.web.bind.annotation.*",
                        "org.springframework.http.ResponseEntity",
                        "jakarta.validation.Valid",
                        "lombok.RequiredArgsConstructor"
                ))
                .serviceTemplate(SERVICE_TEMPLATE)
                .serviceImports(java.util.Set.of(
                        "org.springframework.stereotype.Service",
                        "org.springframework.security.authentication.BadCredentialsException",
                        "org.springframework.security.crypto.password.PasswordEncoder",
                        "lombok.RequiredArgsConstructor"
                ))
                .configTemplate(CONFIG_TEMPLATE)
                .configImports(java.util.Set.of(
                        "org.springframework.context.annotation.Configuration",
                        "org.springframework.context.annotation.Bean",
                        "org.springframework.beans.factory.annotation.Value",
                        "org.springframework.security.crypto.password.PasswordEncoder",
                        "org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder"
                ))
                .securityTemplate(SECURITY_TEMPLATE)
                .securityImports(java.util.Set.of(
                        "org.springframework.context.annotation.Configuration",
                        "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity",
                        "org.springframework.security.config.annotation.web.builders.HttpSecurity",
                        "org.springframework.security.config.http.SessionCreationPolicy",
                        "org.springframework.security.web.SecurityFilterChain",
                        "org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter",
                        "org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer",
                        "org.springframework.context.annotation.Bean",
                        "lombok.RequiredArgsConstructor"
                ))
                .build();
    }

    @Override
    public String getArchitectureDescription() {
        return """
                ## JWT Authentication Architecture
                
                **Flow:**
                1. Client sends credentials to `/api/auth/login`
                2. Server validates credentials against the user store
                3. Server generates a signed JWT access token
                4. Client stores the token and sends it in `Authorization: Bearer <token>` header
                5. `JwtAuthenticationFilter` intercepts every request, validates the token, and sets SecurityContext
                
                **Security Layers:**
                - BCrypt password hashing (cost factor 12)
                - Token signature verification (HMAC-SHA256)
                - Stateless session management (no server-side sessions)
                {{ARCHITECTURE_EXTENSIONS}}
                """;
    }

    @Override
    public List<String> getDefaultComponents() {
        return List.of(
                "AuthController",
                "AuthService",
                "JwtTokenProvider",
                "JwtAuthenticationFilter",
                "SecurityConfig",
                "UserDetailsService"
        );
    }

    @Override
    public List<String> getRecommendedStack() {
        return List.of(
                "Spring Boot 3.x",
                "Spring Security 6",
                "JJWT (io.jsonwebtoken)",
                "BCrypt Password Encoder",
                "PostgreSQL"
        );
    }

    @Override
    public Map<String, String> getDefaultPlaceholders() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("REFRESH_TOKEN_SECTION", "");
        defaults.put("REFRESH_TOKEN_ENDPOINT", "");
        defaults.put("REFRESH_TOKEN_SERVICE", "");
        defaults.put("REFRESH_TOKEN_CONFIG", "");
        defaults.put("RBAC_ANNOTATIONS", "");
        defaults.put("RBAC_ROLE_HIERARCHY", "");
        defaults.put("RBAC_METHOD_SECURITY", "");
        defaults.put("ARCHITECTURE_EXTENSIONS", "");
        defaults.put("ADDITIONAL_COMPONENTS", "");
        defaults.put("ADDITIONAL_STACK", "");
        return defaults;
    }

    // ─── Template Sections ─────────────────────────────────────────────────────

    private static final String CONTROLLER_TEMPLATE = """
            @RestController
            @RequestMapping("/api/auth")
            @RequiredArgsConstructor
            public class AuthController {
            
                private final AuthService authService;
            
                @PostMapping("/login")
                public ResponseEntity<ApiResponse<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest request) {
                    AuthResponse response = authService.authenticate(request);
                    return ResponseEntity.ok(ApiResponse.success("Login successful", response));
                }
            
                @PostMapping("/register")
                public ResponseEntity<ApiResponse<AuthResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {
                    AuthResponse response = authService.register(request);
                    return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
                }
            
                {{REFRESH_TOKEN_ENDPOINT}}
            }
            """;

    private static final String SERVICE_TEMPLATE = """
            @Service
            @RequiredArgsConstructor
            public class AuthService {
            
                private final UserRepository userRepository;
                private final PasswordEncoder passwordEncoder;
                private final JwtTokenProvider tokenProvider;
            
                public AuthResponse authenticate(LoginRequest request) {
                    User user = userRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new BadCredentialsException("Invalid credentials");
                    }
            
                    String accessToken = tokenProvider.generateToken(user);
                    {{REFRESH_TOKEN_SERVICE}}
            
                    return AuthResponse.builder()
                            .accessToken(accessToken)
                            .tokenType("Bearer")
                            .build();
                }
            
                public AuthResponse register(RegisterRequest request) {
                    if (userRepository.existsByEmail(request.getEmail())) {
                        throw new DuplicateResourceException("Email already registered");
                    }
            
                    User user = User.builder()
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .name(request.getName())
                            {{RBAC_ANNOTATIONS}}
                            .build();
            
                    userRepository.save(user);
            
                    String accessToken = tokenProvider.generateToken(user);
                    {{REFRESH_TOKEN_SERVICE}}
            
                    return AuthResponse.builder()
                            .accessToken(accessToken)
                            .tokenType("Bearer")
                            .build();
                }
            }
            """;

    private static final String CONFIG_TEMPLATE = """
            @Configuration
            public class JwtConfig {
            
                @Value("${jwt.secret}")
                private String jwtSecret;
            
                @Value("${jwt.expiration-ms:900000}")
                private long accessTokenExpirationMs; // 15 minutes
            
                {{REFRESH_TOKEN_CONFIG}}
            
                @Bean
                public JwtTokenProvider jwtTokenProvider() {
                    return new JwtTokenProvider(jwtSecret, accessTokenExpirationMs);
                }
            
                @Bean
                public PasswordEncoder passwordEncoder() {
                    return new BCryptPasswordEncoder(12);
                }
            }
            """;

    private static final String SECURITY_TEMPLATE = """
            @Configuration
            @EnableWebSecurity
            {{RBAC_METHOD_SECURITY}}
            @RequiredArgsConstructor
            public class SecurityConfig {
            
                private final JwtAuthenticationFilter jwtFilter;
            
                @Bean
                public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    http
                        .csrf(AbstractHttpConfigurer::disable)
                        .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            {{RBAC_ROLE_HIERARCHY}}
                            .anyRequest().authenticated()
                        )
                        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            
                    return http.build();
                }
            }
            """;
}
