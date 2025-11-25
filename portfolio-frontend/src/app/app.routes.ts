import { Routes, Route } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { ProjectDetailComponent } from './components/project-detail/project-detail.component';
import { LoginComponent } from './pages/login/login.component';
import { ContactComponent } from './pages/contact/contact.component';
import { adminGuard } from './guards/auth.guard';
import { AdminLayoutComponent } from './pages/admin/admin-layout/admin-layout.component';
import { DemoLayoutComponent } from './pages/demo/demo-layout/demo-layout.component';
import { DashboardComponent } from './pages/admin/dashboard/dashboard.component';
import { ProjectListComponent as AdminProjectListComponent } from './pages/admin/projects/project-list/project-list.component';
import { ProjectFormComponent } from './pages/admin/projects/project-form/project-form.component';
import { SkillListComponent } from './pages/admin/skills/skill-list/skill-list.component';
import { SkillFormComponent } from './pages/admin/skills/skill-form/skill-form.component';
import { AdminCvComponent } from './pages/admin/admin-cv/admin-cv.component';
import { ExperienceListComponent } from './pages/admin/experiences/experience-list/experience-list.component';
import { ExperienceFormComponent } from './pages/admin/experiences/experience-form/experience-form.component';
import { ArticleDetailComponent } from './pages/blog/article-detail/article-detail.component';
import { ArticleListComponent } from './pages/blog/article-list/article-list.component';
import { AdminArticleListComponent } from './pages/admin/articles/article-list/article-list.component';
import { ArticleFormComponent } from './pages/admin/articles/article-form/article-form.component';
import { TagListComponent } from './pages/admin/tags/tag-list/tag-list.component';
import { TagFormComponent } from './pages/admin/tags/tag-form/tag-form.component';
import { SiteConfigurationFormComponent } from './pages/admin/site-configuration/site-configuration-form.component';

/**
 * Shared admin child routes used by both /admin and /admindemo
 */
const adminChildRoutes: Route[] = [
  {
    path: '',
    component: DashboardComponent,
  },
  {
    path: 'projects',
    component: AdminProjectListComponent,
  },
  {
    path: 'projects/new',
    component: ProjectFormComponent,
  },
  {
    path: 'projects/:id/edit',
    component: ProjectFormComponent,
  },
  {
    path: 'skills',
    component: SkillListComponent,
  },
  {
    path: 'skills/new',
    component: SkillFormComponent,
  },
  {
    path: 'skills/:id/edit',
    component: SkillFormComponent,
  },
  {
    path: 'tags',
    component: TagListComponent,
  },
  {
    path: 'tags/new',
    component: TagFormComponent,
  },
  {
    path: 'tags/:id/edit',
    component: TagFormComponent,
  },
  {
    path: 'cv',
    component: AdminCvComponent,
  },
  {
    path: 'experiences',
    component: ExperienceListComponent,
  },
  {
    path: 'experiences/new',
    component: ExperienceFormComponent,
  },
  {
    path: 'experiences/:id/edit',
    component: ExperienceFormComponent,
  },
  {
    path: 'articles',
    component: AdminArticleListComponent,
  },
  {
    path: 'articles/new',
    component: ArticleFormComponent,
  },
  {
    path: 'articles/:id/edit',
    component: ArticleFormComponent,
  },
  {
    path: 'configuration',
    component: SiteConfigurationFormComponent,
  },
];

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'projects/:id',
    component: ProjectDetailComponent,
  },
  {
    path: 'contact',
    component: ContactComponent,
  },
  {
    path: 'blog',
    component: ArticleListComponent,
  },
  {
    path: 'blog/:slug',
    component: ArticleDetailComponent,
  },
  {
    path: 'admindemo',
    component: DemoLayoutComponent,
    children: adminChildRoutes,
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    component: AdminLayoutComponent,
    children: adminChildRoutes,
  },
  {
    path: '**',
    redirectTo: '',
  },
];
