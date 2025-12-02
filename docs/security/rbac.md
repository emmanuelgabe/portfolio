# Role-Based Access Control (RBAC)

---

## Table of Contents
1. [Overview](#1-overview)
2. [Roles and Permissions](#2-roles-and-permissions)
3. [Backend Authorization](#3-backend-authorization)
4. [Frontend Route Guards](#4-frontend-route-guards)
5. [Permission Matrix](#5-permission-matrix)
6. [Implementation Details](#6-implementation-details)

---

## 1. Overview

The Portfolio application implements **Role-Based Access Control (RBAC)** to manage user permissions and restrict access to sensitive operations.

**Authorization Model**:
- **Backend**: Spring Security method-level authorization
- **Frontend**: Angular route guards
- **Token-Based**: User roles embedded in JWT access token

**Key Principles**:
- Public endpoints require no authentication
- Admin endpoints require ROLE_ADMIN
- Authorization checked on both frontend and backend
- Backend is the source of truth (frontend guards are UX only)

---

## 2. Roles and Permissions

### Available Roles

The system currently supports two roles:

| Role | Authority | Description |
|------|-----------|-------------|
| **USER** | `ROLE_USER` | Standard authenticated user |
| **ADMIN** | `ROLE_ADMIN` | Administrator with full access |

**Note**: In Spring Security, role names are prefixed with `ROLE_` in authorities.

### Role Hierarchy

```
ADMIN (inherits all USER permissions)
  └── USER (basic authenticated access)
```

**Current Implementation**: ADMIN does NOT automatically inherit USER permissions in Spring Security config. ADMIN has explicit permissions.

**Future Enhancement**: Role hierarchy can be configured:
```java
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
    return hierarchy;
}
```

---

## 3. Backend Authorization

### Endpoint Protection

**SecurityFilterChain Configuration**:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            // Public endpoints (no authentication required)
            .requestMatchers("/health", "/api/version", "/actuator/health", "/api/health/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/uploads/**").permitAll()
            .requestMatchers("/api/cv/current", "/api/cv/download").permitAll()

            // Admin endpoints (ROLE_ADMIN required)
            .requestMatchers("/api/admin/**").hasRole("ADMIN")

            // Public read-only endpoints (GET only)
            .requestMatchers(HttpMethod.GET, "/api/projects", "/api/projects/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/skills", "/api/skills/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/tags", "/api/tags/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/experiences", "/api/experiences/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/articles", "/api/articles/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/hero", "/api/hero/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/configuration").permitAll()

            // All other endpoints require authentication
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

    return http.build();
}
```

### Method-Level Authorization

**Using @PreAuthorize**:

```java
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // Only ADMIN can access
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Public access (no annotation needed if endpoint is public in SecurityFilterChain)
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        ProjectResponse response = projectService.getProject(id);
        return ResponseEntity.ok(response);
    }
}
```

**Alternative Annotations**:
```java
@PreAuthorize("hasRole('ADMIN')")              // Single role
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")   // Multiple roles
@PreAuthorize("hasAuthority('ROLE_ADMIN')")    // By authority
@PreAuthorize("isAuthenticated()")             // Any authenticated user
```

### Service-Level Authorization

**Example**:
```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse createProject(CreateProjectRequest request) {
        // Only admins can create projects
        Project project = projectMapper.toEntity(request);
        Project savedProject = projectRepository.save(project);
        return projectMapper.toResponse(savedProject);
    }

    // No authorization - public method
    public List<ProjectResponse> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream()
            .map(projectMapper::toResponse)
            .toList();
    }
}
```

---

## 4. Frontend Route Guards

### Auth Guard

Protects routes that require authentication (any logged-in user).

**File**: `auth.guard.ts`

```typescript
@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    const hasToken = this.tokenStorage.hasValidToken();

    if (!hasToken) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: state.url }
      });
      return false;
    }

    return true;
  }
}
```

### Admin Guard

Protects routes that require ADMIN role.

**File**: `admin.guard.ts`

```typescript
@Injectable({
  providedIn: 'root'
})
export class AdminGuard {
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    const accessToken = this.tokenStorage.getAccessToken();

    if (!accessToken) {
      this.router.navigate(['/login']);
      return false;
    }

    // Decode JWT to check roles
    try {
      const payload = JSON.parse(atob(accessToken.split('.')[1]));
      const roles = payload.authorities?.map((a: any) => a.authority) || [];

      if (!roles.includes('ROLE_ADMIN')) {
        this.router.navigate(['/']);
        return false;
      }

      return true;
    } catch (error) {
      this.router.navigate(['/login']);
      return false;
    }
  }
}
```

### Route Configuration

**File**: `app.routes.ts`

```typescript
export const routes: Routes = [
  // Public routes
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'projects',
    component: ProjectListComponent
  },
  {
    path: 'projects/:id',
    component: ProjectDetailComponent
  },

  // Admin routes (protected)
  {
    path: 'admin',
    canActivate: [AdminGuard],
    children: [
      {
        path: '',
        component: AdminDashboardComponent
      },
      {
        path: 'projects',
        component: AdminProjectsComponent
      },
      {
        path: 'skills',
        component: AdminSkillsComponent
      },
      {
        path: 'cv',
        component: AdminCvComponent
      }
    ]
  },

  // 404
  {
    path: '**',
    component: NotFoundComponent
  }
];
```

---

## 5. Permission Matrix

### API Endpoints

| Endpoint | Method | Public | USER | ADMIN | Description |
|----------|--------|--------|------|-------|-------------|
| `/api/auth/login` | POST | Yes | Yes | Yes | Login |
| `/api/auth/refresh` | POST | Yes | Yes | Yes | Refresh token |
| `/api/auth/logout` | POST | No | Yes | Yes | Logout (authenticated) |
| `/api/auth/change-password` | POST | No | Yes | Yes | Change password |
| `/api/projects` | GET | Yes | Yes | Yes | List projects |
| `/api/projects/{id}` | GET | Yes | Yes | Yes | Get project |
| `/api/admin/projects` | POST | No | No | Yes | Create project |
| `/api/admin/projects/{id}` | PUT | No | No | Yes | Update project |
| `/api/admin/projects/{id}` | DELETE | No | No | Yes | Delete project |
| `/api/admin/projects/{id}/upload-image` | POST | No | No | Yes | Upload image |
| `/api/admin/projects/{id}/delete-image` | DELETE | No | No | Yes | Delete image |
| `/api/skills` | GET | Yes | Yes | Yes | List skills |
| `/api/skills/{id}` | GET | Yes | Yes | Yes | Get skill |
| `/api/admin/skills` | POST | No | No | Yes | Create skill |
| `/api/admin/skills/{id}` | PUT | No | No | Yes | Update skill |
| `/api/admin/skills/{id}` | DELETE | No | No | Yes | Delete skill |
| `/api/cv/current` | GET | Yes | Yes | Yes | Get current CV |
| `/api/cv/download/{id}` | GET | Yes | Yes | Yes | Download CV |
| `/api/admin/cv/upload` | POST | No | No | Yes | Upload CV |
| `/api/admin/cv/all` | GET | No | No | Yes | List all CVs |
| `/api/admin/cv/{id}/set-current` | PUT | No | No | Yes | Set current CV |
| `/api/admin/cv/{id}` | DELETE | No | No | Yes | Delete CV |
| `/api/admin/upload` | POST | No | No | Yes | Generic file upload |
| `/api/configuration` | GET | Yes | Yes | Yes | Site configuration |
| `/api/tags` | GET | Yes | Yes | Yes | List tags |
| `/api/tags/{id}` | GET | Yes | Yes | Yes | Get tag |
| `/api/admin/tags` | POST | No | No | Yes | Create tag |
| `/api/admin/tags/{id}` | PUT | No | No | Yes | Update tag |
| `/api/admin/tags/{id}` | DELETE | No | No | Yes | Delete tag |
| `/api/experiences` | GET | Yes | Yes | Yes | List experiences |
| `/api/experiences/{id}` | GET | Yes | Yes | Yes | Get experience |
| `/api/admin/experiences` | POST | No | No | Yes | Create experience |
| `/api/admin/experiences/{id}` | PUT | No | No | Yes | Update experience |
| `/api/admin/experiences/{id}` | DELETE | No | No | Yes | Delete experience |
| `/api/articles` | GET | Yes | Yes | Yes | List articles |
| `/api/articles/{slug}` | GET | Yes | Yes | Yes | Get article by slug |
| `/api/admin/articles` | POST | No | No | Yes | Create article |
| `/api/admin/articles/{id}` | PUT | No | No | Yes | Update article |
| `/api/admin/articles/{id}` | DELETE | No | No | Yes | Delete article |
| `/api/hero` | GET | Yes | Yes | Yes | Hero section |
| `/api/admin/hero` | PUT | No | No | Yes | Update hero section |
| `/api/contact` | POST | Yes | Yes | Yes | Send contact message |
| `/actuator/health` | GET | Yes | Yes | Yes | Health check |

**Legend**:
- Yes = Allowed
- No = Forbidden (401 or 403)

### Frontend Routes

| Route | Public | USER | ADMIN | Description |
|-------|--------|------|-------|-------------|
| `/` | Yes | Yes | Yes | Home page |
| `/login` | Yes | Yes | Yes | Login page |
| `/projects` | Yes | Yes | Yes | Project list |
| `/projects/:id` | Yes | Yes | Yes | Project detail |
| `/admin` | No | No | Yes | Admin dashboard |
| `/admin/projects` | No | No | Yes | Manage projects |
| `/admin/skills` | No | No | Yes | Manage skills |
| `/admin/cv` | No | No | Yes | Manage CVs |

---

## 6. Implementation Details

### User Entity with Roles

**File**: `User.java`

```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

**Role Enum**:
```java
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
```

**Database Schema**:
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

### UserDetailsService Implementation

**File**: `CustomUserDetailsService.java`

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toArray(GrantedAuthority[]::new))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }
}
```

### JWT Token with Roles

**Token Generation**:
```java
public String generateAccessToken(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();

    return Jwts.builder()
        .claim("type", "access")
        .claim("authorities", userDetails.getAuthorities())  // Includes roles
        .setSubject(userDetails.getUsername())
        .setIssuer("portfolio-backend")
        .setAudience("portfolio-frontend")
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(SignatureAlgorithm.HS256, jwtSecret)
        .compact();
}
```

**Token Payload Example**:
```json
{
  "type": "access",
  "sub": "admin",
  "authorities": [
    {
      "authority": "ROLE_ADMIN"
    }
  ],
  "iss": "portfolio-backend",
  "aud": "portfolio-frontend",
  "iat": 1700000000,
  "exp": 1700000900
}
```

---

## Related Documentation

- [Security: Authentication](./authentication.md) - JWT authentication architecture
- [Security: Password Management](./password-management.md) - Password policies
- [API: Authentication](../api/authentication.md) - Authentication endpoints
- [Development: Testing](../development/testing-guide.md) - Testing guidelines
