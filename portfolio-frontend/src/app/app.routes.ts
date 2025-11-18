import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { ProjectListComponent } from './components/project-list/project-list.component';
import { ProjectDetailComponent } from './components/project-detail/project-detail.component';
import { LoginComponent } from './pages/login/login.component';
import { adminGuard } from './guards/auth.guard';
import { AdminLayoutComponent } from './pages/admin/admin-layout/admin-layout.component';
import { DashboardComponent } from './pages/admin/dashboard/dashboard.component';
import { ProjectListComponent as AdminProjectListComponent } from './pages/admin/projects/project-list/project-list.component';
import { ProjectFormComponent } from './pages/admin/projects/project-form/project-form.component';
import { SkillListComponent } from './pages/admin/skills/skill-list/skill-list.component';
import { SkillFormComponent } from './pages/admin/skills/skill-form/skill-form.component';

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
    path: 'about',
    component: HomeComponent, // TODO: Remplacer par AboutComponent quand créé (task 1.16)
  },
  {
    path: 'experiences',
    component: HomeComponent, // TODO: Remplacer par ExperiencesComponent quand créé (task 7.3)
  },
  {
    path: 'projects',
    component: ProjectListComponent,
  },
  {
    path: 'projects/:id',
    component: ProjectDetailComponent,
  },
  {
    path: 'contact',
    component: HomeComponent, // TODO: Remplacer par ContactComponent quand créé (task 3.5)
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    component: AdminLayoutComponent,
    children: [
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
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
