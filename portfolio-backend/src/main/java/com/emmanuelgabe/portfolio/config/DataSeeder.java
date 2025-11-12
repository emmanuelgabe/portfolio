package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.entity.Skill;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.emmanuelgabe.portfolio.service.SkillService;
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

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final SkillService skillService;
    private final SkillRepository skillRepository;

    public DataSeeder(ProjectService projectService, ProjectRepository projectRepository,
                      TagRepository tagRepository, SkillService skillService,
                      SkillRepository skillRepository) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
        this.skillService = skillService;
        this.skillRepository = skillRepository;
    }

    @Override
    public void run(String... args) {
        log.info("🌱 Checking database for seeding...");

        // Create skills if they don't exist
        if (skillRepository.count() == 0) {
            log.info("Creating skills...");
            createSkills();
            log.info("✅ Created {} skills", skillRepository.count());
        } else {
            log.info("⏭️  Skills already exist, skipping skill seeding.");
        }

        // Create tags and projects if they don't exist
        if (projectRepository.count() == 0) {
            log.info("Creating projects and tags...");

            // Create tags
            List<Tag> tags = createTags();
            log.info("✅ Created {} tags", tags.size());

            // Create projects
            createProjects(tags);
            log.info("✅ Created {} projects", projectRepository.count());
        } else {
            log.info("⏭️  Projects already exist, skipping project seeding.");
        }

        log.info("✅ Database seeding complete!");
    }

    private List<Tag> createTags() {
        List<Tag> tags = new ArrayList<>();

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

    private Tag createTag(String name, String color) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setColor(color);
        return tagRepository.save(tag);
    }

    private void createProjects(List<Tag> tags) {
        // Project 1: E-Commerce Platform (Featured)
        createProject(
                "E-Commerce Platform",
                "A full-stack e-commerce application built with Angular and Spring Boot. Features include product catalog, shopping cart, checkout process, and admin dashboard for inventory management.",
                "Angular, Spring Boot, PostgreSQL, Docker",
                "https://github.com/emmanuelgabe/ecommerce-platform",
                "https://ecommerce-demo.emmanuelgabe.com",
                null,
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
                "https://github.com/emmanuelgabe/task-manager",
                "https://tasks.emmanuelgabe.com",
                null,
                true,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "React"),
                        findTagByName(tags, "Node.js"),
                        findTagByName(tags, "PostgreSQL"),
                        findTagByName(tags, "Docker"),
                        findTagByName(tags, "REST API")
                ))
        );

        // Project 3: Weather Dashboard
        createProject(
                "Weather Dashboard",
                "A responsive weather application that displays current weather and 7-day forecasts. Uses OpenWeatherMap API and features location search, favorite locations, and interactive charts.",
                "TypeScript, Angular, REST API",
                "https://github.com/emmanuelgabe/weather-dashboard",
                "https://weather.emmanuelgabe.com",
                null,
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
                "https://github.com/emmanuelgabe/portfolio",
                null,
                null,
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
                "https://github.com/emmanuelgabe/blog-api",
                null,
                null,
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
                "https://github.com/emmanuelgabe/chat-app",
                "https://chat.emmanuelgabe.com",
                null,
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
                "https://github.com/emmanuelgabe/expense-tracker",
                null,
                null,
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
                "https://github.com/emmanuelgabe/recipe-finder",
                "https://recipes.emmanuelgabe.com",
                null,
                false,
                new HashSet<>(java.util.Arrays.asList(
                        findTagByName(tags, "React"),
                        findTagByName(tags, "Node.js"),
                        findTagByName(tags, "REST API")
                ))
        );
    }

    private void createProject(String title, String description, String techStack,
                               String githubUrl, String demoUrl, String imageUrl,
                               boolean featured, Set<Long> tagIds) {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setTechStack(techStack);
        request.setGithubUrl(githubUrl);
        request.setDemoUrl(demoUrl);
        request.setImageUrl(imageUrl);
        request.setFeatured(featured);
        request.setTagIds(tagIds);

        projectService.createProject(request);
    }

    private Long findTagByName(List<Tag> tags, String name) {
        return tags.stream()
                .filter(tag -> tag.getName().equals(name))
                .findFirst()
                .map(Tag::getId)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + name));
    }

    private void createSkills() {
        // Frontend Skills
        createSkill("Angular", "bi-code-square", "#dd0031", SkillCategory.FRONTEND, 90, 1);
        createSkill("React", "bi-bootstrap", "#61dafb", SkillCategory.FRONTEND, 85, 2);
        createSkill("TypeScript", "bi-file-code", "#3178c6", SkillCategory.FRONTEND, 90, 3);

        // Backend Skills
        createSkill("Spring Boot", "bi-gear", "#6db33f", SkillCategory.BACKEND, 90, 4);
        createSkill("Java", "bi-cup-hot", "#007396", SkillCategory.BACKEND, 88, 5);
        createSkill("Node.js", "bi-node-plus", "#339933", SkillCategory.BACKEND, 80, 6);

        // Database Skills
        createSkill("PostgreSQL", "bi-database", "#336791", SkillCategory.DATABASE, 85, 7);
        createSkill("MongoDB", "bi-server", "#47a248", SkillCategory.DATABASE, 75, 8);

        // DevOps Skills
        createSkill("Docker", "bi-box", "#2496ed", SkillCategory.DEVOPS, 85, 9);
        createSkill("Git", "bi-git", "#f05032", SkillCategory.DEVOPS, 90, 10);

        // Tools
        createSkill("REST API", "bi-arrow-left-right", "#009688", SkillCategory.TOOLS, 90, 11);
        createSkill("VS Code", "bi-code-slash", "#007acc", SkillCategory.TOOLS, 88, 12);
    }

    private void createSkill(String name, String icon, String color, SkillCategory category, Integer level, Integer displayOrder) {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName(name);
        request.setIcon(icon);
        request.setColor(color);
        request.setCategory(category);
        request.setLevel(level);
        request.setDisplayOrder(displayOrder);

        skillService.createSkill(request);
    }
}
