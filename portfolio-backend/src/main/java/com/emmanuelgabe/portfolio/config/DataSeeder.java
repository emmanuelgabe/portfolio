package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.entity.SiteConfiguration;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.repository.SiteConfigurationRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.emmanuelgabe.portfolio.service.SkillService;
import com.emmanuelgabe.portfolio.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DataSeeder component for populating the database with test data in development environment.
 * Only runs when the 'dev' profile is active.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);

    private final ProjectService projectService;
    private final TagService tagService;
    private final SkillService skillService;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;

    public DataSeeder(ProjectService projectService, TagService tagService,
                      SkillService skillService, ProjectRepository projectRepository,
                      SkillRepository skillRepository, SiteConfigurationRepository siteConfigurationRepository) {
        this.projectService = projectService;
        this.tagService = tagService;
        this.skillService = skillService;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.siteConfigurationRepository = siteConfigurationRepository;
    }

    @Override
    public void run(String... args) {
        LOG.info("[DATA_SEEDER] Checking database for seeding");

        // Create site configuration if it doesn't exist
        if (siteConfigurationRepository.count() == 0) {
            LOG.info("[DATA_SEEDER] Creating site configuration - count=0");
            createSiteConfiguration();
            LOG.info("[DATA_SEEDER] Site configuration created");
        } else {
            LOG.info("[DATA_SEEDER] Site configuration already exists, skipping seeding");
        }

        // Create skills if they don't exist
        if (skillRepository.count() == 0) {
            LOG.info("[DATA_SEEDER] Creating skills - count=0");
            createSkills();
            LOG.info("[DATA_SEEDER] Skills created - count={}", skillRepository.count());
        } else {
            LOG.info("[DATA_SEEDER] Skills already exist, skipping seeding");
        }

        // Create tags and projects if they don't exist
        if (projectRepository.count() == 0) {
            LOG.info("[DATA_SEEDER] Creating projects and tags - count=0");

            // Create tags
            List<TagResponse> tags = createTags();
            LOG.info("[DATA_SEEDER] Tags created - count={}", tags.size());

            // Create projects
            createProjects(tags);
            LOG.info("[DATA_SEEDER] Projects created - count={}", projectRepository.count());
        } else {
            LOG.info("[DATA_SEEDER] Projects already exist, skipping seeding");
        }

        LOG.info("[DATA_SEEDER] Database seeding complete");
    }

    private void createSiteConfiguration() {
        SiteConfiguration config = new SiteConfiguration();
        config.setFullName("Emmanuel Gabe");
        config.setEmail("contact@emmanuelgabe.com");
        config.setHeroTitle("Developpeur Backend");
        config.setHeroDescription("Je cree des applications web modernes et evolutives avec Angular, "
                + "Spring Boot et les technologies de pointe. Passionne par le code propre, les bonnes "
                + "pratiques et la creation d'experiences utilisateur exceptionnelles.");
        config.setSiteTitle("Portfolio - Emmanuel Gabe");
        config.setSeoDescription("Portfolio de Emmanuel Gabe, developpeur backend Java/Spring Boot.");
        config.setGithubUrl("https://github.com/emmanuelgabe");
        config.setLinkedinUrl("https://linkedin.com/in/egabe");
        siteConfigurationRepository.save(config);
    }

    private List<TagResponse> createTags() {
        List<TagResponse> tags = new ArrayList<>();

        tags.add(createTag("Angular", "#dd0031"));
        tags.add(createTag("Spring Boot", "#6db33f"));
        tags.add(createTag("TypeScript", "#3178c6"));
        tags.add(createTag("Java", "#007396"));
        tags.add(createTag("PostgreSQL", "#336791"));
        tags.add(createTag("Docker", "#2496ed"));
        tags.add(createTag("Git", "#f05032"));
        tags.add(createTag("REST API", "#009688"));
        tags.add(createTag("React", "#61dafb"));
        tags.add(createTag("Node.js", "#339933"));

        return tags;
    }

    private TagResponse createTag(String name, String color) {
        CreateTagRequest request = new CreateTagRequest();
        request.setName(name);
        request.setColor(color);
        return tagService.createTag(request);
    }

    private void createProjects(List<TagResponse> tags) {
        createFeaturedProjects(tags);
        createOtherProjects(tags);
    }

    private void createFeaturedProjects(List<TagResponse> tags) {
        // Project 1: E-Commerce Platform (Featured)
        createProject(
                "E-Commerce Platform",
                "A full-stack e-commerce application built with Angular and Spring Boot. Features include product catalog, shopping cart, checkout process, and admin dashboard for inventory management.",
                "Angular, Spring Boot, PostgreSQL, Docker",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/ecommerce-platform",
                        "https://ecommerce-demo.emmanuelgabe.com",
                        "https://images.unsplash.com/photo-1557821552-17105176677c?w=800&h=600&fit=crop"
                ),
                true,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "Angular"),
                        findTagByName(tags, "Spring Boot"),
                        findTagByName(tags, "PostgreSQL"),
                        findTagByName(tags, "Docker"),
                        findTagByName(tags, "REST API")
                ))
        );

        // Project 2: Task Management System (Featured)
        createProject(
                "Task Management System",
                "A collaborative task management tool inspired by Trello. Users can create boards, lists, and cards with drag-and-drop functionality. Real-time updates using WebSockets.",
                "React, Node.js, PostgreSQL, Docker",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/task-manager",
                        "https://tasks.emmanuelgabe.com",
                        "https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=800&h=600&fit=crop"
                ),
                true,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "React"),
                        findTagByName(tags, "Node.js"),
                        findTagByName(tags, "PostgreSQL"),
                        findTagByName(tags, "Docker"),
                        findTagByName(tags, "REST API")
                ))
        );
    }

    private void createOtherProjects(List<TagResponse> tags) {
        // Project 3: Weather Dashboard
        createProject(
                "Weather Dashboard",
                "A responsive weather application that displays current weather and 7-day forecasts. Uses OpenWeatherMap API and features location search, favorite locations, and interactive charts.",
                "TypeScript, Angular, REST API",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/weather-dashboard",
                        "https://weather.emmanuelgabe.com",
                        "https://images.unsplash.com/photo-1592210454359-9043f067919b?w=800&h=600&fit=crop"
                ),
                false,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "Angular"),
                        findTagByName(tags, "TypeScript"),
                        findTagByName(tags, "REST API")
                ))
        );

        // Project 4: Portfolio Website (Featured)
        createProject(
                "Portfolio Website",
                "This very portfolio website! Built with Angular for the frontend and Spring Boot for the backend. Features project management, tagging system, and responsive design.",
                "Angular, Spring Boot, PostgreSQL, Docker",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/portfolio",
                        "http://localhost:4200/admindemo",
                        "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&h=600&fit=crop"
                ),
                true,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "Angular"),
                        findTagByName(tags, "Spring Boot"),
                        findTagByName(tags, "TypeScript"),
                        findTagByName(tags, "Java"),
                        findTagByName(tags, "PostgreSQL"),
                        findTagByName(tags, "Docker")
                ))
        );

        // Project 5: Blog API
        createProject(
                "Blog REST API",
                "A RESTful API for a blogging platform with JWT authentication, role-based access control, and full CRUD operations. Includes pagination, sorting, and search functionality.",
                "Spring Boot, PostgreSQL, JWT",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/blog-api",
                        null,
                        "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=800&h=600&fit=crop"
                ),
                false,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "Spring Boot"),
                        findTagByName(tags, "Java"),
                        findTagByName(tags, "PostgreSQL"),
                        findTagByName(tags, "REST API")
                ))
        );

        // Project 6: Chat Application
        createProject(
                "Real-Time Chat Application",
                "A real-time messaging application with WebSocket support. Features include private messaging, group chats, online status indicators, and message history.",
                "React, Node.js, WebSocket",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/chat-app",
                        "https://chat.emmanuelgabe.com",
                        "https://images.unsplash.com/photo-1577563908411-5077b6dc7624?w=800&h=600&fit=crop"
                ),
                false,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "React"),
                        findTagByName(tags, "Node.js"),
                        findTagByName(tags, "TypeScript")
                ))
        );

        // Project 7: Expense Tracker
        createProject(
                "Expense Tracker",
                "A personal finance management application to track income and expenses. Features budget planning, category management, and data visualization with charts and graphs.",
                "Angular, Spring Boot, PostgreSQL",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/expense-tracker",
                        null,
                        "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800&h=600&fit=crop"
                ),
                false,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "Angular"),
                        findTagByName(tags, "Spring Boot"),
                        findTagByName(tags, "PostgreSQL"),
                        findTagByName(tags, "TypeScript")
                ))
        );

        // Project 8: Recipe Finder
        createProject(
                "Recipe Finder",
                "A web application to search and save recipes. Integrates with external recipe APIs, allows users to create collections, and includes a meal planning feature.",
                "React, Node.js, REST API",
                new ProjectUrls(
                        "https://github.com/emmanuelgabe/recipe-finder",
                        "https://recipes.emmanuelgabe.com",
                        "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800&h=600&fit=crop"
                ),
                false,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "React"),
                        findTagByName(tags, "Node.js"),
                        findTagByName(tags, "REST API")
                ))
        );
    }

    private void createProject(String title, String description, String techStack,
                               ProjectUrls urls, boolean featured, Set<Long> tagIds) {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setTechStack(techStack);
        request.setGithubUrl(urls.githubUrl());
        request.setDemoUrl(urls.demoUrl());
        request.setImageUrl(urls.imageUrl());
        request.setFeatured(featured);
        request.setTagIds(tagIds);

        projectService.createProject(request);
    }

    private record ProjectUrls(String githubUrl, String demoUrl, String imageUrl) {
    }

    private Long findTagByName(List<TagResponse> tags, String name) {
        return tags.stream()
                .filter(tag -> tag.getName().equals(name))
                .findFirst()
                .map(TagResponse::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "name", name));
    }

    private void createSkills() {
        // Frontend Skills
        createSkill("Angular", "bi-code-square", "#dd0031", SkillCategory.FRONTEND, 1);
        createSkill("React", "bi-bootstrap", "#61dafb", SkillCategory.FRONTEND, 2);
        createSkill("TypeScript", "bi-file-code", "#3178c6", SkillCategory.FRONTEND, 3);

        // Backend Skills
        createSkill("Spring Boot", "bi-gear", "#6db33f", SkillCategory.BACKEND, 4);
        createSkill("Java", "bi-cup-hot", "#007396", SkillCategory.BACKEND, 5);
        createSkill("Node.js", "bi-node-plus", "#339933", SkillCategory.BACKEND, 6);

        // Database Skills
        createSkill("PostgreSQL", "bi-database", "#336791", SkillCategory.DATABASE, 7);
        createSkill("MongoDB", "bi-server", "#47a248", SkillCategory.DATABASE, 8);

        // DevOps Skills
        createSkill("Docker", "bi-box", "#2496ed", SkillCategory.DEVOPS, 9);
        createSkill("Git", "bi-git", "#f05032", SkillCategory.DEVOPS, 10);

        // Tools
        createSkill("REST API", "bi-arrow-left-right", "#009688", SkillCategory.TOOLS, 11);
        createSkill("VS Code", "bi-code-slash", "#007acc", SkillCategory.TOOLS, 12);
    }

    private void createSkill(String name, String icon, String color, SkillCategory category, Integer displayOrder) {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName(name);
        request.setIcon(icon);
        request.setColor(color);
        request.setCategory(category);
        request.setDisplayOrder(displayOrder);

        skillService.createSkill(request);
    }
}
