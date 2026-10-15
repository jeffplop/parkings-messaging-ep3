package cl.duoc.parkings.messaging.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del backend: valida el JWT emitido por el IDaaS antes de autorizar
 * las peticiones (instrucción "el backend debe incluir filtros que validen el
 * JWT recibido desde el IDaaS").
 *
 * Perfil por defecto: al activar oauth2ResourceServer().jwt(), Spring Security
 * registra el filtro BearerTokenAuthenticationFilter, que extrae el token
 * "Authorization: Bearer <jwt>", verifica su firma contra el JWK del IDaaS
 * (jwk-set-uri) y su emisor (issuer-uri). Sin JWT válido → 401. El frontend
 * Angular obtiene ese JWT en el login con IDaaS y lo adjunta en cada llamada.
 *
 * Perfil "local": desactiva la seguridad SOLO para demostrar la mensajería en
 * local sin un IDaaS (ejecutar con --spring.profiles.active=local).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Profile("!local")
    SecurityFilterChain securedChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API stateless, sin cookies de sesión
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Profile("local")
    SecurityFilterChain localChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
